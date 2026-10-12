const XLSX = require('xlsx');
const path = require('path');
const fs = require('fs');

const headers = ['اسم الطبيب', 'الاختصاص', 'رقم الهاتف', 'العنوان', 'الملاحظات'];

const sampleData = [
  headers,
  [
    'د. أحمد علي البصري',
    'جراحة العظام والكسور والعمود الفقري',
    '07801234567',
    'البصرة - الجبيلة - مقابل مستشفى ابن البيطار',
    'عيادة مسائية من ٤ حتى ٨:٣٠ مساءً'
  ],
  [
    'د. فاطمة حسن الموسوي',
    'طب وجراحة العيون وتصحيح البصر',
    '07709876543',
    'البصرة - العشار - شارع الاستقلال - مجمع الشفاء الطبي',
    'فحص بالليزر والليزك، حجز مسبق عبر الواتساب'
  ],
  [
    'د. حيدر جاسم التميمي',
    'أمراض القلب والقسطرة العلاجية',
    '07501122334',
    'البصرة - الجزائر - تقاطع التأميم - عمارة الأطباء الدور الثاني',
    'تخطيط وإيكو القلب وفحص الجهد'
  ]
];

const wb = XLSX.utils.book_new();
const ws = XLSX.utils.aoa_to_sheet(sampleData);

ws['!cols'] = [
  { wch: 28 },
  { wch: 32 },
  { wch: 18 },
  { wch: 50 },
  { wch: 45 }
];

XLSX.utils.book_append_sheet(wb, ws, 'أطباء البصرة');

const outDir = path.resolve(__dirname, '../public');
if (!fs.existsSync(outDir)) {
  fs.mkdirSync(outDir, { recursive: true });
}

const outPath1 = path.join(outDir, 'sample_doctors_basra.xlsx');
const outPath2 = path.resolve(__dirname, '../../sample_doctors_basra.xlsx');

XLSX.writeFile(wb, outPath1);
XLSX.writeFile(wb, outPath2);

console.log('✅ Sample Excel files created successfully:');
console.log(' - ' + outPath1);
console.log(' - ' + outPath2);
