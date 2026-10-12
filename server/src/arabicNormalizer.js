/**
 * Arabic Normalizer and data cleaning utility for Basra Doctors Directory.
 */
const TASHKEEL_REGEX = /[\u064B-\u065F\u0670\u06D6-\u06ED]/g;
const TATWEEL = '\u0640';

// Prominent Basra districts and areas
const BASRA_AREAS = [
  'العشار', 'الجبيلة', 'الجزائر', 'الطويسة', 'المعقل', 'بريهة', 'الخورة',
  'الزبير', 'القرنة', 'أبو الخصيب', 'ابو الخصيب', 'الهارثة', 'كرمة علي',
  'شط العرب', 'التنومة', 'دور النفط', 'التحسينية', 'مناوي باشا', 'الأصمعي',
  'المشراق', 'الجمعيات', 'القبلة', 'حي الحسين', 'خمسة ميل', 'الفاو',
  'المدينة', 'الحيانية', 'البراضعية', 'الرباط', 'الأبلة', 'الكزيزة'
];

function normalizeArabic(text) {
  if (!text || typeof text !== 'string') return '';

  return text
    .replace(TASHKEEL_REGEX, '')
    .replaceAll(TATWEEL, '')
    .replace(/[أإآٱ]/g, 'ا')
    .replace(/[ىئ]/g, 'ي')
    .replace(/[ؤ]/g, 'و')
    .replace(/[ة]/g, 'ه')
    .replace(/[ـ]/g, '')
    .replace(/[.,\/#!$%\^&\*;:{}=\-_`~()؟?]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
}

/**
 * Levenshtein distance for fuzzy matching
 */
function levenshteinDistance(a, b) {
  if (a.length === 0) return b.length;
  if (b.length === 0) return a.length;
  const matrix = [];

  for (let i = 0; i <= b.length; i++) {
    matrix[i] = [i];
  }
  for (let j = 0; j <= a.length; j++) {
    matrix[0][j] = j;
  }

  for (let i = 1; i <= b.length; i++) {
    for (let j = 1; j <= a.length; j++) {
      if (b.charAt(i - 1) === a.charAt(j - 1)) {
        matrix[i][j] = matrix[i - 1][j - 1];
      } else {
        matrix[i][j] = Math.min(
          matrix[i - 1][j - 1] + 1, // substitution
          matrix[i][j - 1] + 1,     // insertion
          matrix[i - 1][j] + 1      // deletion
        );
      }
    }
  }
  return matrix[b.length][a.length];
}

/**
 * Calculates a relevance score for a search query against doctor fields.
 */
function calculateMatchScore(query, doc) {
  const normQ = normalizeArabic(query);
  if (!normQ) return 0;

  const normName = normalizeArabic(doc.fullName || doc.full_name || '');
  const normSpec = normalizeArabic(doc.specialty || '');
  const normAddr = normalizeArabic(doc.address || '');
  const normNotes = normalizeArabic(doc.notes || '');
  const combined = `${normName} ${normSpec} ${normAddr} ${normNotes}`;

  // 1. Exact match on name
  if (normName === normQ) return 100;
  if (normName.startsWith(normQ)) return 90;

  // 2. Query words containment in doctor details
  const qWords = normQ.split(' ').filter(Boolean);
  let allWordsMatched = true;
  let wordScore = 0;

  for (const qw of qWords) {
    if (normName.includes(qw)) {
      wordScore += 25;
    } else if (normSpec.includes(qw)) {
      wordScore += 20;
    } else if (normAddr.includes(qw)) {
      wordScore += 15;
    } else if (normNotes.includes(qw)) {
      wordScore += 10;
    } else {
      // Fuzzy check on each name word (1 typo allowed for words >= 4 chars)
      const nameWords = normName.split(' ');
      const fuzzyMatch = nameWords.some((nw) => {
        if (nw.length >= 4 && qw.length >= 4) {
          return levenshteinDistance(nw, qw) <= 1;
        }
        return false;
      });
      if (fuzzyMatch) {
        wordScore += 15;
      } else {
        allWordsMatched = false;
      }
    }
  }

  if (allWordsMatched && qWords.length > 0) {
    return Math.min(85, wordScore);
  }

  // Substring anywhere in combined fields
  if (combined.includes(normQ)) {
    return 60;
  }

  return wordScore > 20 ? wordScore : 0;
}

/**
 * Normalizes Iraqi phone numbers to standard display (07XXXXXXXXX)
 * and international WhatsApp format (9647XXXXXXXXX).
 */
function cleanIraqiPhone(phoneInput) {
  if (!phoneInput) {
    return { displayPhone: '', whatsappPhone: '', isValid: false };
  }

  // Convert Arabic/Persian digits to English digits
  const rawStr = String(phoneInput)
    .replace(/[٠-٩]/g, (d) => '٠١٢٣٤٥٦٧٨٩'.indexOf(d))
    .replace(/[۰-۹]/g, (d) => '۰۱۲۳۴۵۶۷۸۹'.indexOf(d))
    .replace(/[^0-9+]/g, '');

  let digits = rawStr.replace(/^\+/, '');

  // Strip international prefix 00964 or 964
  if (digits.startsWith('00964')) {
    digits = digits.slice(5);
  } else if (digits.startsWith('964')) {
    digits = digits.slice(3);
  }

  // Now digits should start with 07 or 7
  if (digits.startsWith('0') && digits.length >= 10) {
    digits = digits.slice(1);
  }

  // Iraqi mobile numbers are 10 digits after leading 0 (e.g. 7801234567, 7701234567, 7501234567)
  if (digits.startsWith('7') && digits.length === 10) {
    return {
      displayPhone: '0' + digits,
      whatsappPhone: '964' + digits,
      isValid: true
    };
  }

  // Fallback if landline or non-standard
  if (digits.length >= 7) {
    return {
      displayPhone: rawStr.startsWith('+') ? rawStr : '0' + digits,
      whatsappPhone: '964' + digits,
      isValid: true
    };
  }

  return {
    displayPhone: String(phoneInput).trim(),
    whatsappPhone: '',
    isValid: false
  };
}

/**
 * Detects known Basra area/neighborhood from address text
 */
function detectBasraArea(addressText) {
  if (!addressText || typeof addressText !== 'string') return 'مركز البصرة';

  const norm = normalizeArabic(addressText);
  for (const area of BASRA_AREAS) {
    if (norm.includes(normalizeArabic(area))) {
      return area;
    }
  }
  return 'البصرة';
}

module.exports = {
  normalizeArabic,
  calculateMatchScore,
  cleanIraqiPhone,
  detectBasraArea,
  levenshteinDistance,
  BASRA_AREAS
};

