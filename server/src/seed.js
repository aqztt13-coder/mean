require('dotenv').config();
const bcrypt = require('bcryptjs');
const { initDatabase, getAsync, runAsync } = require('./db');

async function seedAdmin() {
  await initDatabase();

  const username = process.env.ADMIN_USERNAME || 'admin';
  const password = process.env.ADMIN_PASSWORD || 'AdminSecurePassword2026!';
  const email = process.env.ADMIN_EMAIL || 'admin@daliliktibbi.com';

  console.log(`Checking existing admin user for: ${username}...`);
  const existing = await getAsync('SELECT * FROM admins WHERE username = ?', [username]);

  const salt = await bcrypt.genSalt(10);
  const passwordHash = await bcrypt.hash(password, salt);
  const timestamp = Date.now();

  if (existing) {
    await runAsync(
      'UPDATE admins SET password_hash = ?, email = ? WHERE username = ?',
      [passwordHash, email, username]
    );
    console.log(`✅ Admin user "${username}" updated with new password.`);
  } else {
    const id = 'admin_' + timestamp;
    await runAsync(
      'INSERT INTO admins (id, username, email, password_hash, created_at) VALUES (?, ?, ?, ?, ?)',
      [id, username, email, passwordHash, timestamp]
    );
    console.log(`✅ Initial Admin user "${username}" created successfully.`);
  }

  console.log('----------------------------------------------------');
  console.log(`🔑 Admin Username: ${username}`);
  console.log(`🔑 Admin Password: ${password}`);
  console.log('----------------------------------------------------');
  process.exit(0);
}

seedAdmin().catch((err) => {
  console.error('Error seeding admin user:', err);
  process.exit(1);
});
