require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const { initDatabase } = require('./db');
const doctorRoutes = require('./routes/doctorRoutes');
const adminRoutes = require('./routes/adminRoutes');

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// Ensure DB is initialized for serverless environments (e.g. Vercel)
let dbInitialized = false;
app.use(async (req, res, next) => {
  if (!dbInitialized) {
    try {
      await initDatabase();
      dbInitialized = true;
    } catch (err) {
      console.error('Database auto-initialization error:', err);
    }
  }
  next();
});

// Resolve static files directory (supports root or server directory execution)
function findPublicDir() {
  const candidates = [
    path.join(__dirname, '../public'),
    path.join(__dirname, '../../public'),
    path.join(process.cwd(), 'server/public'),
    path.join(process.cwd(), 'public')
  ];
  for (const dir of candidates) {
    if (fs.existsSync(dir)) return dir;
  }
  return path.join(__dirname, '../public');
}
const publicDir = findPublicDir();

// Protected or direct admin page route (handled directly to avoid 301 folder redirects)
app.get(['/admin', '/admin/', '/admin.html'], (req, res) => {
  const candidates = [
    path.join(publicDir, 'admin.html'),
    path.join(publicDir, 'admin/index.html'),
    path.resolve(__dirname, '../../public/admin.html'),
    path.resolve(__dirname, '../public/admin.html'),
    path.join(process.cwd(), 'public/admin.html')
  ];
  for (const file of candidates) {
    if (fs.existsSync(file)) {
      return res.sendFile(file);
    }
  }
  res.status(404).send('صفحة الإدارة غير موجودة');
});

// Explicit home page route
app.get('/', (req, res) => {
  const indexFile = path.join(publicDir, 'index.html');
  if (fs.existsSync(indexFile)) {
    return res.sendFile(indexFile);
  }
  res.send('دليل أطباء البصرة API يعمل بنجاح');
});

// Serve static assets
app.use(express.static(publicDir));

// Mount API routes
app.use('/api', doctorRoutes);
app.use('/api', adminRoutes);

// Centralized error handler
app.use((err, req, res, next) => {
  console.error('Unhandled server error:', err);
  res.status(500).json({
    success: false,
    message: 'حدث خطأ داخلي في الخادم: ' + (err.message || 'خطأ غير معروف')
  });
});

// Start standalone HTTP listener if run directly (and not on Vercel / serverless)
if (require.main === module && !process.env.VERCEL) {
  initDatabase()
    .then(() => {
      console.log('✅ SQLite Database initialized successfully.');
      app.listen(PORT, '0.0.0.0', () => {
        console.log(`🚀 دليل أطباء البصرة — Backend running on http://0.0.0.0:${PORT}`);
      });
    })
    .catch((err) => {
      console.error('❌ Failed to initialize database:', err);
    });
}

module.exports = app;
