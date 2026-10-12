const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const multer = require('multer');
const XLSX = require('xlsx');
const { getAsync, runAsync, allAsync } = require('../db');
const { generateToken, authenticateAdmin } = require('../auth');
const {
  normalizeArabic,
  cleanIraqiPhone,
  detectBasraArea
} = require('../arabicNormalizer');

// Configure multer memory storage for file uploads
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 25 * 1024 * 1024 } // 25 MB max
});

// In-Memory Rate Limiter for Login Protection
// Lockout after 5 failed attempts for 15 minutes
const loginAttempts = new Map();

function checkLoginRateLimit(key) {
  const now = Date.now();
  const entry = loginAttempts.get(key);
  if (!entry) return { allowed: true };

  if (entry.lockUntil && now < entry.lockUntil) {
    const remainingMinutes = Math.ceil((entry.lockUntil - now) / 60000);
    return { allowed: false, remainingMinutes };
  }

  if (entry.lockUntil && now >= entry.lockUntil) {
    loginAttempts.delete(key);
    return { allowed: true };
  }

  return { allowed: true };
}

function recordFailedLogin(key) {
  const now = Date.now();
  const entry = loginAttempts.get(key) || { count: 0, firstAttempt: now };
  entry.count += 1;

  if (entry.count >= 5) {
    entry.lockUntil = now + 15 * 60 * 1000; // 15 mins
  }

  loginAttempts.set(key, entry);
}

function resetLoginRateLimit(key) {
  loginAttempts.delete(key);
}

// 1. Admin Login with Rate Limiting Protection
router.post('/auth/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({
        success: false,
        message: 'يرجى إدخال اسم المستخدم وكلمة المرور'
      });
    }

    const rateKey = (req.ip || 'unknown') + '_' + username.trim().toLowerCase();
    const rateCheck = checkLoginRateLimit(rateKey);

    if (!rateCheck.allowed) {
      return res.status(429).json({
        success: false,
        locked: true,
        message: `تم قفل تسجيل الدخول مؤقتاً لتجاوز الحد الأقصى للمحاولات (5 محاولات خاطئة). يرجى المحاولة بعد ${rateCheck.remainingMinutes} دقيقة.`
      });
    }

    const admin = await getAsync('SELECT * FROM admins WHERE username = ? OR email = ?', [
      username.trim(),
      username.trim()
    ]);

    if (!admin) {
      recordFailedLogin(rateKey);
      return res.status(401).json({
        success: false,
        message: 'اسم المستخدم أو كلمة المرور غير صحيحة'
      });
    }

    const passwordMatch = await bcrypt.compare(password, admin.password_hash);
    if (!passwordMatch) {
      recordFailedLogin(rateKey);
      return res.status(401).json({
        success: false,
        message: 'اسم المستخدم أو كلمة المرور غير صحيحة'
      });
    }

    // Success: reset attempts
    resetLoginRateLimit(rateKey);
    const token = generateToken(admin);

    res.json({
      success: true,
      token,
      username: admin.username,
      message: 'تم تسجيل الدخول بنجاح إلى لوحة الإدارة'
    });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// 2. Admin Check Session
router.get('/auth/me', authenticateAdmin, async (req, res) => {
  res.json({
    authenticated: true,
    admin: req.admin
  });
});

// 3. Change Admin Password
router.put('/admin/change-password', authenticateAdmin, async (req, res) => {
  try {
    const { currentPassword, newPassword } = req.body;
    if (!currentPassword || !newPassword) {
      return res.status(400).json({
        success: false,
        message: 'يرجى تقديم كلمة المرور الحالية وكلمة المرور الجديدة'
      });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({
        success: false,
        message: 'كلمة المرور الجديدة يجب ألا تقل عن 6 خانات'
      });
    }

    const admin = await getAsync('SELECT * FROM admins WHERE id = ?', [req.admin.id]);
    if (!admin) {
      return res.status(404).json({ success: false, message: 'حساب المدير غير موجود' });
    }

    const match = await bcrypt.compare(currentPassword, admin.password_hash);
    if (!match) {
      return res.status(400).json({
        success: false,
        message: 'كلمة المرور الحالية غير صحيحة'
      });
    }

    const salt = await bcrypt.genSalt(10);
    const newHash = await bcrypt.hash(newPassword, salt);

    await runAsync('UPDATE admins SET password_hash = ? WHERE id = ?', [newHash, req.admin.id]);

    res.json({
      success: true,
      message: 'تم تحديث كلمة المرور بنجاح. استخدم كلمة المرور الجديدة في المرات القادمة.'
    });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// 4. Download Blank Excel Template
router.get('/admin/template', (req, res) => {
  try {
    const headers = ['اسم الطبيب', 'الاختصاص', 'رقم الهاتف', 'العنوان', 'الملاحظات'];
    const sampleRows = [
      [
        'د. أحمد علي البصري',
        'جراحة العظام والكسور',
        '07801234567',
        'البصرة - الجبيلة - مقابل المستشفى العام',
        'عيادة مسائية من ٤ إلى ٨ مساءً'
      ],
      [
        'د. فاطمة حسن الموسوي',
        'طب وجراحة العيون',
        '07709876543',
        'البصرة - العشار - شارع الاستقلال - مجمع الشفاء الطبي',
        'فحص بالليزر والليزك'
      ],
      [
        'د. حيدر جاسم التميمي',
        'أمراض وجراحة القلب',
        '07501122334',
        'البصرة - الجزائر - تقاطع التأميم - عمارة الأطباء',
        'تخطيط وإيكو القلب'
      ]
    ];

    const wsData = [headers, ...sampleRows];
    const wb = XLSX.utils.book_new();
    const ws = XLSX.utils.aoa_to_sheet(wsData);

    // Set column widths
    ws['!cols'] = [
      { wch: 25 },
      { wch: 25 },
      { wch: 18 },
      { wch: 45 },
      { wch: 35 }
    ];

    XLSX.utils.book_append_sheet(wb, ws, 'أطباء البصرة');
    const buffer = XLSX.write(wb, { type: 'buffer', bookType: 'xlsx' });

    res.setHeader(
      'Content-Disposition',
      'attachment; filename="basra_doctors_template.xlsx"'
    );
    res.setHeader(
      'Content-Type',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    );
    res.send(buffer);
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// 5. Export Current Doctors to Excel
router.get('/admin/export-excel', authenticateAdmin, async (req, res) => {
  try {
    const rows = await allAsync('SELECT * FROM doctors ORDER BY full_name ASC');
    const headers = ['اسم الطبيب', 'الاختصاص', 'رقم الهاتف', 'العنوان', 'الملاحظات'];
    const dataRows = rows.map((r) => [
      r.full_name || '',
      r.specialty || '',
      r.phone_primary || '',
      r.address || '',
      r.notes || ''
    ]);

    const wsData = [headers, ...dataRows];
    const wb = XLSX.utils.book_new();
    const ws = XLSX.utils.aoa_to_sheet(wsData);

    ws['!cols'] = [
      { wch: 25 },
      { wch: 25 },
      { wch: 18 },
      { wch: 45 },
      { wch: 35 }
    ];

    XLSX.utils.book_append_sheet(wb, ws, 'أطباء البصرة');
    const buffer = XLSX.write(wb, { type: 'buffer', bookType: 'xlsx' });

    res.setHeader(
      'Content-Disposition',
      `attachment; filename="basra_doctors_export_${Date.now()}.xlsx"`
    );
    res.setHeader(
      'Content-Type',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    );
    res.send(buffer);
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

/**
 * Helper function to parse raw Excel sheet rows with the 5 required columns
 */
function parseAndCleanExcelRows(sheetRows) {
  if (!Array.isArray(sheetRows) || sheetRows.length < 2) {
    return { validRows: [], errors: ['الملف فارغ أو لا يحتوي على صفوف بيانات'] };
  }

  // Row 0 is header
  const validRows = [];
  const errors = [];

  for (let i = 1; i < sheetRows.length; i++) {
    const row = sheetRows[i];
    if (!row || row.length === 0) continue;

    // Check if entire row is empty
    const isEmpty = row.every((cell) => cell === undefined || cell === null || String(cell).trim() === '');
    if (isEmpty) continue;

    const rawName = row[0] !== undefined ? String(row[0]).trim() : '';
    const rawSpec = row[1] !== undefined ? String(row[1]).trim() : '';
    const rawPhone = row[2] !== undefined ? String(row[2]).trim() : '';
    const rawAddr = row[3] !== undefined ? String(row[3]).trim() : '';
    const rawNotes = row[4] !== undefined ? String(row[4]).trim() : '';

    if (!rawName) {
      errors.push(`الصف رقم ${i + 1}: اسم الطبيب فارغ`);
      continue;
    }

    const phoneInfo = cleanIraqiPhone(rawPhone);
    const detectedArea = detectBasraArea(rawAddr);

    validRows.push({
      fullName: rawName,
      normalizedName: normalizeArabic(rawName),
      specialty: rawSpec || 'طب عام',
      phonePrimary: phoneInfo.displayPhone || rawPhone,
      phoneSecondary: null,
      address: rawAddr,
      city: detectedArea,
      governorate: 'البصرة',
      notes: rawNotes,
      latitude: null,
      longitude: null,
      extraAttributes: {}
    });
  }

  return { validRows, errors };
}

// 6. Direct Excel File Upload (Preview or Commit)
router.post('/admin/upload-excel', authenticateAdmin, upload.single('file'), async (req, res) => {
  try {
    if (!req.file || !req.file.buffer) {
      return res.status(400).json({ success: false, message: 'يرجى اختيار ملف Excel بصيغة .xlsx أو .xls' });
    }

    const isPreview = req.query.preview === 'true' || req.body.preview === 'true';
    const importMode = (req.body.importMode || req.query.importMode || 'MERGE').toUpperCase();

    const workbook = XLSX.read(req.file.buffer, { type: 'buffer' });
    const sheetName = workbook.SheetNames[0];
    if (!sheetName) {
      return res.status(400).json({ success: false, message: 'ملف Excel لا يحتوي على أي صفحات عمل' });
    }

    const sheet = workbook.Sheets[sheetName];
    const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 });

    const { validRows, errors } = parseAndCleanExcelRows(rows);

    if (isPreview) {
      return res.json({
        success: true,
        fileName: req.file.originalname,
        totalRows: Math.max(0, rows.length - 1),
        validCount: validRows.length,
        errorCount: errors.length,
        errors: errors.slice(0, 10),
        previewRows: validRows.slice(0, 10)
      });
    }

    // Process Import
    const timestamp = Date.now();
    let addedCount = 0;
    let updatedCount = 0;
    let skippedCount = 0;

    if (importMode === 'REPLACE_ALL') {
      await runAsync(`CREATE TABLE IF NOT EXISTS doctors_backup_${timestamp} AS SELECT * FROM doctors`);
      await runAsync('DELETE FROM doctors');
    }

    for (const doc of validRows) {
      const docId = 'doc_' + Math.random().toString(36).substring(2, 9) + '_' + Date.now();
      const normName = doc.normalizedName || normalizeArabic(doc.fullName);
      const phone = doc.phonePrimary || '';

      if (importMode === 'MERGE') {
        // Prevent duplicate by name + phone
        const existing = await getAsync(
          'SELECT id FROM doctors WHERE normalized_name = ? OR (phone_primary = ? AND phone_primary != "")',
          [normName, phone]
        );

        if (existing) {
          await runAsync(
            `UPDATE doctors SET
              full_name = ?, normalized_name = ?, specialty = ?, phone_primary = ?,
              address = ?, city = ?, notes = ?, updated_at = ?
             WHERE id = ?`,
            [
              doc.fullName,
              normName,
              doc.specialty,
              phone,
              doc.address,
              doc.city,
              doc.notes,
              timestamp,
              existing.id
            ]
          );
          updatedCount++;
          continue;
        }
      }

      await runAsync(
        `INSERT INTO doctors (
          id, full_name, normalized_name, specialty, phone_primary,
          governorate, city, address, notes, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          docId,
          doc.fullName,
          normName,
          doc.specialty,
          phone,
          'البصرة',
          doc.city,
          doc.address,
          doc.notes,
          timestamp,
          timestamp
        ]
      );
      addedCount++;
    }

    // Save import metadata
    await runAsync(
      `INSERT OR REPLACE INTO system_metadata (key, value) VALUES ("last_import_time", ?)`,
      [String(timestamp)]
    );

    res.json({
      success: true,
      message: `تمت المعالجة بنجاح: تم إضافة ${addedCount} سجل جديد، وتحديث ${updatedCount} سجل موجود.`,
      addedCount,
      updatedCount,
      skippedCount,
      timestamp
    });
  } catch (err) {
    res.status(500).json({ success: false, message: 'فشل في قراءة ملف Excel: ' + err.message });
  }
});

// 7. Batch Import from parsed client data
router.post('/admin/doctors/batch-import', authenticateAdmin, async (req, res) => {
  try {
    const { fileName, importMode, doctors } = req.body;

    if (!Array.isArray(doctors) || doctors.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'قائمة الأطباء فارغة أو غير صالحة'
      });
    }

    const mode = (importMode || 'MERGE').toUpperCase();
    const timestamp = Date.now();
    let addedCount = 0;
    let updatedCount = 0;
    let skippedCount = 0;

    if (mode === 'REPLACE_ALL') {
      await runAsync(`CREATE TABLE IF NOT EXISTS doctors_backup_${timestamp} AS SELECT * FROM doctors`);
      await runAsync('DELETE FROM doctors');
    }

    for (const doc of doctors) {
      const fullName = (doc.fullName || '').trim();
      if (!fullName) {
        skippedCount++;
        continue;
      }

      const docId = doc.id || ('doc_' + Math.random().toString(36).substring(2, 9) + '_' + Date.now());
      const normalizedName = doc.normalizedName || normalizeArabic(fullName);
      const phoneInfo = cleanIraqiPhone(doc.phonePrimary || doc.phone || '');
      const phone = phoneInfo.displayPhone || doc.phonePrimary || '';
      const address = doc.address || '';
      const detectedArea = detectBasraArea(address);

      if (mode === 'MERGE') {
        const existing = await getAsync(
          'SELECT id FROM doctors WHERE normalized_name = ? OR (phone_primary = ? AND phone_primary != "")',
          [normalizedName, phone]
        );

        if (existing) {
          await runAsync(
            `UPDATE doctors SET 
              full_name = ?, normalized_name = ?, specialty = ?, phone_primary = ?,
              address = ?, city = ?, notes = ?, latitude = ?, longitude = ?, updated_at = ?
             WHERE id = ?`,
            [
              fullName,
              normalizedName,
              doc.specialty || 'طب عام',
              phone,
              address,
              detectedArea,
              doc.notes || '',
              doc.latitude || null,
              doc.longitude || null,
              timestamp,
              existing.id
            ]
          );
          updatedCount++;
          continue;
        }
      }

      await runAsync(
        `INSERT INTO doctors (
          id, full_name, normalized_name, specialty, phone_primary,
          governorate, city, address, notes, latitude, longitude, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          docId,
          fullName,
          normalizedName,
          doc.specialty || 'طب عام',
          phone,
          'البصرة',
          detectedArea,
          address,
          doc.notes || '',
          doc.latitude || null,
          doc.longitude || null,
          timestamp,
          timestamp
        ]
      );
      addedCount++;
    }

    await runAsync(
      `INSERT OR REPLACE INTO system_metadata (key, value) VALUES ("last_import_time", ?)`,
      [String(timestamp)]
    );

    res.json({
      success: true,
      message: `تم استيراد ${addedCount} طبيب جديد وتحديث ${updatedCount} سجل بنجاح.`,
      addedCount,
      updatedCount,
      skippedCount,
      timestamp
    });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// 8. Create Doctor Manually
router.post('/admin/doctors', authenticateAdmin, async (req, res) => {
  try {
    const doc = req.body;
    const fullName = (doc.fullName || '').trim();
    if (!fullName) {
      return res.status(400).json({ success: false, message: 'اسم الطبيب مطلوب' });
    }

    const docId = doc.id || ('doc_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6));
    const normalizedName = normalizeArabic(fullName);
    const phoneInfo = cleanIraqiPhone(doc.phonePrimary || '');
    const phone = phoneInfo.displayPhone || doc.phonePrimary || '';
    const address = doc.address || '';
    const detectedArea = detectBasraArea(address);
    const timestamp = Date.now();

    await runAsync(
      `INSERT INTO doctors (
        id, full_name, normalized_name, specialty, phone_primary,
        governorate, city, address, notes, latitude, longitude, created_at, updated_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        docId,
        fullName,
        normalizedName,
        doc.specialty || 'طب عام',
        phone,
        'البصرة',
        detectedArea,
        address,
        doc.notes || '',
        doc.latitude ? parseFloat(doc.latitude) : null,
        doc.longitude ? parseFloat(doc.longitude) : null,
        timestamp,
        timestamp
      ]
    );

    res.status(201).json({
      success: true,
      doctor: {
        id: docId,
        fullName,
        specialty: doc.specialty || 'طب عام',
        phonePrimary: phone,
        address,
        notes: doc.notes || '',
        latitude: doc.latitude,
        longitude: doc.longitude
      },
      message: 'تمت إضافة الطبيب بنجاح'
    });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// 9. Update Doctor Manually
router.put('/admin/doctors/:id', authenticateAdmin, async (req, res) => {
  try {
    const docId = req.params.id;
    const doc = req.body;
    const fullName = (doc.fullName || '').trim();
    if (!fullName) {
      return res.status(400).json({ success: false, message: 'اسم الطبيب مطلوب' });
    }

    const normalizedName = normalizeArabic(fullName);
    const phoneInfo = cleanIraqiPhone(doc.phonePrimary || '');
    const phone = phoneInfo.displayPhone || doc.phonePrimary || '';
    const address = doc.address || '';
    const detectedArea = detectBasraArea(address);
    const timestamp = Date.now();

    await runAsync(
      `UPDATE doctors SET 
        full_name = ?, normalized_name = ?, specialty = ?, phone_primary = ?,
        governorate = ?, city = ?, address = ?, notes = ?, latitude = ?, longitude = ?,
        updated_at = ?
       WHERE id = ?`,
      [
        fullName,
        normalizedName,
        doc.specialty || 'طب عام',
        phone,
        'البصرة',
        detectedArea,
        address,
        doc.notes || '',
        doc.latitude ? parseFloat(doc.latitude) : null,
        doc.longitude ? parseFloat(doc.longitude) : null,
        timestamp,
        docId
      ]
    );

    res.json({
      success: true,
      message: 'تم تحديث بيانات الطبيب بنجاح',
      doctor: {
        id: docId,
        fullName,
        specialty: doc.specialty,
        phonePrimary: phone,
        address,
        notes: doc.notes,
        latitude: doc.latitude,
        longitude: doc.longitude
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// 10. Delete Doctor
router.delete('/admin/doctors/:id', authenticateAdmin, async (req, res) => {
  try {
    await runAsync('DELETE FROM doctors WHERE id = ?', [req.params.id]);
    res.json({ success: true, message: 'تم حذف الطبيب من الدليل بنجاح' });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;
