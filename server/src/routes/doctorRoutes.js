const express = require('express');
const router = express.Router();
const https = require('https');
const { getAsync, allAsync, runAsync } = require('../db');
const {
  normalizeArabic,
  calculateMatchScore,
  cleanIraqiPhone,
  detectBasraArea,
  BASRA_AREAS
} = require('../arabicNormalizer');

// 1. Health check
router.get('/health', async (req, res) => {
  try {
    const row = await getAsync('SELECT COUNT(*) as count FROM doctors');
    res.json({
      status: 'ok',
      timestamp: Date.now(),
      message: 'خادم دليل أطباء البصرة يعمل بصورة طبيعية',
      doctorCount: row?.count || 0
    });
  } catch (err) {
    res.status(500).json({ status: 'error', message: err.message });
  }
});

// 2. Summary & metadata
router.get('/doctors/meta/summary', async (req, res) => {
  try {
    const countRow = await getAsync('SELECT COUNT(*) as count FROM doctors');
    const specRows = await allAsync(
      'SELECT DISTINCT specialty FROM doctors WHERE specialty != "" ORDER BY specialty ASC'
    );
    const lastImportRow = await getAsync('SELECT value FROM system_metadata WHERE key = "last_import_time"');

    // Extract populated areas in the database
    const addrRows = await allAsync('SELECT address, city FROM doctors WHERE address != "" OR city != ""');
    const foundAreas = new Set();
    addrRows.forEach((r) => {
      const area = detectBasraArea(`${r.city || ''} ${r.address || ''}`);
      if (area && area !== 'البصرة') foundAreas.add(area);
    });

    res.json({
      doctorCount: countRow?.count || 0,
      specialtiesCount: specRows.length,
      specialties: specRows.map((s) => s.specialty),
      areas: Array.from(foundAreas).sort(),
      defaultBasraAreas: BASRA_AREAS,
      lastImportDate: lastImportRow ? parseInt(lastImportRow.value, 10) : null,
      serverStatus: 'online',
      version: '1.0.0'
    });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

// 3. Search doctors
router.get('/doctors', async (req, res) => {
  try {
    const { q, specialty, area, page = 1, limit = 100 } = req.query;
    const pageNum = Math.max(1, parseInt(page, 10));
    const limitNum = Math.min(500, Math.max(1, parseInt(limit, 10)));
    const offset = (pageNum - 1) * limitNum;

    let sql = 'SELECT * FROM doctors WHERE 1=1';
    const params = [];

    if (specialty && specialty !== 'الكل' && specialty.trim()) {
      sql += ' AND specialty = ?';
      params.push(specialty.trim());
    }

    const rows = await allAsync(sql, params);

    // Map doctors and parse extra_attributes JSON
    const mapped = rows.map((r) => {
      let extras = {};
      if (r.extra_attributes) {
        try {
          extras = JSON.parse(r.extra_attributes);
        } catch (_) {}
      }

      const phoneInfo = cleanIraqiPhone(r.phone_primary || '');
      const detectedArea = detectBasraArea(`${r.city || ''} ${r.address || ''}`);

      return {
        id: r.id,
        fullName: r.full_name,
        normalizedName: r.normalized_name,
        specialty: r.specialty,
        subSpecialty: r.sub_specialty,
        title: r.title,
        phonePrimary: phoneInfo.displayPhone || r.phone_primary || '',
        whatsappPhone: phoneInfo.whatsappPhone,
        phoneSecondary: r.phone_secondary,
        governorate: r.governorate || 'البصرة',
        city: r.city || detectedArea,
        area: detectedArea,
        address: r.address || '',
        clinicName: r.clinic_name,
        workplace: r.workplace,
        workingHours: r.working_hours,
        notes: r.notes || '',
        latitude: r.latitude,
        longitude: r.longitude,
        extraAttributes: extras,
        updatedAt: r.updated_at || Date.now()
      };
    });

    let results = mapped;

    // Filter by area if selected
    if (area && area !== 'الكل' && area.trim()) {
      const normArea = normalizeArabic(area.trim());
      results = results.filter((doc) => {
        const docAddrNorm = normalizeArabic(`${doc.address} ${doc.city} ${doc.area}`);
        return docAddrNorm.includes(normArea);
      });
    }

    // Apply ranking and smart Arabic matching
    if (q && q.trim()) {
      const queryTrim = q.trim();
      const scored = [];

      for (const doc of results) {
        const score = calculateMatchScore(queryTrim, doc);
        if (score > 0) {
          scored.push({ doc, score });
        }
      }

      scored.sort((a, b) => {
        if (b.score !== a.score) return b.score - a.score;
        return a.doc.fullName.localeCompare(b.doc.fullName, 'ar');
      });

      results = scored.map((s) => s.doc);
    } else {
      results.sort((a, b) => a.fullName.localeCompare(b.fullName, 'ar'));
    }

    const totalCount = results.length;
    const paginated = results.slice(offset, offset + limitNum);

    res.json({
      doctors: paginated,
      totalCount,
      page: pageNum,
      totalPages: Math.ceil(totalCount / limitNum) || 1
    });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

// 4. Geocoding endpoint with SQLite cache & Nominatim OpenStreetMap
router.get('/geocode', async (req, res) => {
  try {
    const query = (req.query.q || '').trim();
    if (!query) {
      return res.status(400).json({ success: false, message: 'العنوان مطلوب' });
    }

    const normQuery = normalizeArabic(query);

    // 1. Check local geocache table
    const cached = await getAsync('SELECT * FROM geocache WHERE query = ?', [normQuery]);
    if (cached) {
      return res.json({
        cached: true,
        latitude: cached.latitude,
        longitude: cached.longitude,
        displayName: cached.display_name
      });
    }

    // 2. Query Nominatim OpenStreetMap API
    // Append 'البصرة العراق' to improve geocoding accuracy within Basra
    const searchQuery = `${query} البصرة العراق`;
    const encoded = encodeURIComponent(searchQuery);
    const nominatimUrl = `https://nominatim.openstreetmap.org/search?format=json&q=${encoded}&limit=1`;

    const fetchCoordinates = () =>
      new Promise((resolve) => {
        const request = https.get(
          nominatimUrl,
          {
            headers: {
              'User-Agent': 'BasraDoctorsDirectoryApp/1.0 (contact: info@basradoctors.iq)'
            },
            timeout: 5000
          },
          (response) => {
            let data = '';
            response.on('data', (chunk) => (data += chunk));
            response.on('end', () => {
              try {
                const parsed = JSON.parse(data);
                if (Array.isArray(parsed) && parsed.length > 0) {
                  resolve({
                    lat: parseFloat(parsed[0].lat),
                    lon: parseFloat(parsed[0].lon),
                    displayName: parsed[0].display_name
                  });
                } else {
                  resolve(null);
                }
              } catch (_) {
                resolve(null);
              }
            });
          }
        );
        request.on('error', () => resolve(null));
        request.on('timeout', () => {
          request.destroy();
          resolve(null);
        });
      });

    const geoResult = await fetchCoordinates();

    // Default downtown Basra coordinates if geocoding fails or returns empty
    const defaultLat = 30.5081;
    const defaultLon = 47.7835;

    const lat = geoResult ? geoResult.lat : defaultLat;
    const lon = geoResult ? geoResult.lon : defaultLon;
    const displayName = geoResult ? geoResult.displayName : `${query}، البصرة، العراق`;

    // Save into cache
    await runAsync(
      `INSERT OR REPLACE INTO geocache (query, latitude, longitude, display_name, created_at) VALUES (?, ?, ?, ?, ?)`,
      [normQuery, lat, lon, displayName, Date.now()]
    );

    res.json({
      cached: false,
      latitude: lat,
      longitude: lon,
      displayName
    });
  } catch (err) {
    // Graceful fallback to downtown Basra
    res.json({
      cached: false,
      latitude: 30.5081,
      longitude: 47.7835,
      displayName: 'البصرة، العراق'
    });
  }
});

// 5. Get single doctor details
router.get('/doctors/:id', async (req, res) => {
  try {
    const row = await getAsync('SELECT * FROM doctors WHERE id = ?', [req.params.id]);
    if (!row) {
      return res.status(404).json({ message: 'الطبيب غير موجود' });
    }

    let extras = {};
    if (row.extra_attributes) {
      try {
        extras = JSON.parse(row.extra_attributes);
      } catch (_) {}
    }

    const phoneInfo = cleanIraqiPhone(row.phone_primary || '');
    const detectedArea = detectBasraArea(`${row.city || ''} ${row.address || ''}`);

    res.json({
      id: row.id,
      fullName: row.full_name,
      normalizedName: row.normalized_name,
      specialty: row.specialty,
      subSpecialty: row.sub_specialty,
      title: row.title,
      phonePrimary: phoneInfo.displayPhone || row.phone_primary || '',
      whatsappPhone: phoneInfo.whatsappPhone,
      phoneSecondary: row.phone_secondary,
      governorate: row.governorate || 'البصرة',
      city: row.city || detectedArea,
      area: detectedArea,
      address: row.address || '',
      clinicName: row.clinic_name,
      workplace: row.workplace,
      workingHours: row.working_hours,
      notes: row.notes || '',
      latitude: row.latitude,
      longitude: row.longitude,
      extraAttributes: extras,
      updatedAt: row.updated_at
    });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

module.exports = router;
