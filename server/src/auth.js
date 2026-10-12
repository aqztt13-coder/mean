const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'dalilik_tibbi_jwt_super_secret_key_2026_change_in_production';

function generateToken(admin) {
  return jwt.sign(
    {
      id: admin.id,
      username: admin.username,
      role: 'admin'
    },
    JWT_SECRET,
    { expiresIn: '7d' }
  );
}

function authenticateAdmin(req, res, next) {
  const authHeader = req.headers['authorization'];
  if (!authHeader) {
    return res.status(401).json({
      success: false,
      message: 'غير مصرّح: يجب تسجيل الدخول كمدير لتنفيذ هذه العملية.'
    });
  }

  const token = authHeader.startsWith('Bearer ')
    ? authHeader.slice(7).trim()
    : authHeader.trim();

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    if (decoded.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'ممنوع: هذه العملية محصورة بمدير التطبيق فقط.'
      });
    }
    req.admin = decoded;
    next();
  } catch (err) {
    return res.status(401).json({
      success: false,
      message: 'رمز المصادقة غير صالح أو منتهي الصلاحية. يرجى تسجيل الدخول مجدداً.'
    });
  }
}

module.exports = {
  generateToken,
  authenticateAdmin,
  JWT_SECRET
};
