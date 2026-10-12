const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

const isVercel = !!process.env.VERCEL;
let dbPath;

if (process.env.DB_PATH) {
  dbPath = process.env.DB_PATH;
} else if (isVercel) {
  dbPath = '/tmp/dalilik_tibbi.db';
  const bundledDbPath = path.resolve(__dirname, '../data/dalilik_tibbi.db');
  if (fs.existsSync(bundledDbPath) && !fs.existsSync('/tmp/dalilik_tibbi.db')) {
    try {
      fs.copyFileSync(bundledDbPath, '/tmp/dalilik_tibbi.db');
    } catch (e) {
      console.warn('Could not copy bundled DB to /tmp:', e);
    }
  }
} else {
  const dbDir = path.resolve(__dirname, '../data');
  if (!fs.existsSync(dbDir)) {
    try {
      fs.mkdirSync(dbDir, { recursive: true });
    } catch (e) {
      console.warn('Could not create data directory, falling back to /tmp:', e);
    }
  }
  dbPath = fs.existsSync(dbDir) ? path.join(dbDir, 'dalilik_tibbi.db') : '/tmp/dalilik_tibbi.db';
}

const db = new sqlite3.Database(dbPath);

function initDatabase() {
  return new Promise((resolve, reject) => {
    db.serialize(() => {
      // 1. Doctors table
      db.run(`
        CREATE TABLE IF NOT EXISTS doctors (
          id TEXT PRIMARY KEY,
          full_name TEXT NOT NULL,
          normalized_name TEXT NOT NULL,
          specialty TEXT NOT NULL,
          sub_specialty TEXT,
          title TEXT,
          phone_primary TEXT,
          phone_secondary TEXT,
          governorate TEXT,
          city TEXT,
          address TEXT,
          clinic_name TEXT,
          workplace TEXT,
          working_hours TEXT,
          notes TEXT,
          latitude REAL,
          longitude REAL,
          extra_attributes TEXT,
          created_at INTEGER,
          updated_at INTEGER
        )
      `);

      // Indexes for high performance searches
      db.run(`CREATE INDEX IF NOT EXISTS idx_doctors_norm_name ON doctors(normalized_name)`);
      db.run(`CREATE INDEX IF NOT EXISTS idx_doctors_specialty ON doctors(specialty)`);
      db.run(`CREATE INDEX IF NOT EXISTS idx_doctors_governorate ON doctors(governorate)`);
      db.run(`CREATE INDEX IF NOT EXISTS idx_doctors_phone ON doctors(phone_primary)`);

      // 2. Admins table
      db.run(`
        CREATE TABLE IF NOT EXISTS admins (
          id TEXT PRIMARY KEY,
          username TEXT UNIQUE NOT NULL,
          email TEXT,
          password_hash TEXT NOT NULL,
          created_at INTEGER
        )
      `);

      // 3. Import Logs / Audit table
      db.run(`
        CREATE TABLE IF NOT EXISTS import_logs (
          id TEXT PRIMARY KEY,
          file_name TEXT NOT NULL,
          import_mode TEXT NOT NULL,
          total_rows INTEGER,
          valid_rows INTEGER,
          added_count INTEGER,
          updated_count INTEGER,
          skipped_count INTEGER,
          errors_json TEXT,
          admin_id TEXT,
          created_at INTEGER
        )
      `);

      // 4. System Metadata table
      db.run(`
        CREATE TABLE IF NOT EXISTS system_metadata (
          key TEXT PRIMARY KEY,
          value TEXT
        )
      `);

      // 5. Geocoding Cache table for OpenStreetMap / Nominatim
      db.run(`
        CREATE TABLE IF NOT EXISTS geocache (
          query TEXT PRIMARY KEY,
          latitude REAL,
          longitude REAL,
          display_name TEXT,
          created_at INTEGER
        )
      `, (err) => {
        if (err) reject(err);
        else resolve();
      });
    });
  });
}

function runAsync(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.run(sql, params, function (err) {
      if (err) reject(err);
      else resolve(this);
    });
  });
}

function getAsync(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.get(sql, params, (err, row) => {
      if (err) reject(err);
      else resolve(row);
    });
  });
}

function allAsync(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.all(sql, params, (err, rows) => {
      if (err) reject(err);
      else resolve(rows);
    });
  });
}

module.exports = {
  db,
  initDatabase,
  runAsync,
  getAsync,
  allAsync
};
