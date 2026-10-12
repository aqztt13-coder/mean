package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Doctor(
    val id: String,
    val fullName: String,
    val normalizedName: String = "",
    val specialty: String = "",
    val subSpecialty: String? = null,
    val title: String? = null,
    val phonePrimary: String = "",
    val phoneSecondary: String? = null,
    val governorate: String? = null,
    val city: String? = null,
    val address: String? = null,
    val clinicName: String? = null,
    val workplace: String? = null,
    val workingHours: String? = null,
    val notes: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val extraAttributes: Map<String, String> = emptyMap(),
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ImportMode(val titleArabic: String, val descriptionArabic: String) {
    APPEND(
        titleArabic = "إضافة بيانات جديدة",
        descriptionArabic = "إضافة السجلات الجديدة فقط مع الحفاظ على السجلات الحالية ومراجعة التطابقات المحتملة."
    ),
    MERGE(
        titleArabic = "تحديث البيانات الموجودة",
        descriptionArabic = "تحديث السجلات المتطابقة برقم المعرف أو الهاتف وإضافة السجلات الجديدة دون حذف الباقي."
    ),
    REPLACE_ALL(
        titleArabic = "استبدال قاعدة بيانات الأطباء",
        descriptionArabic = "حذف جميع السجلات الحالية واستبدالها بسجلات الملف الجديد بالكامل بعد أخذ نسخة احتياطية تلقائية."
    )
}

data class DuplicatePair(
    val existingDoctor: Doctor?,
    val incomingDoctor: Doctor,
    val reason: String
)

data class ImportPreviewResult(
    val fileName: String,
    val fileSizeFormatted: String,
    val totalRowsDetected: Int,
    val validRowsCount: Int,
    val missingNameCount: Int,
    val incompleteDataCount: Int,
    val potentialDuplicatesCount: Int,
    val newRecordsCount: Int,
    val detectedColumns: List<String>,
    val parsedDoctors: List<Doctor>,
    val samplePreview: List<Doctor>,
    val duplicates: List<DuplicatePair>,
    val warnings: List<String>
)

data class ImportReport(
    val id: String,
    val fileName: String,
    val importMode: ImportMode,
    val addedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val timestamp: Long,
    val message: String,
    val success: Boolean
)

@JsonClass(generateAdapter = true)
data class ImportHistoryItem(
    val id: String,
    val fileName: String,
    val importMode: String,
    val addedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val timestamp: Long,
    val success: Boolean = true
)

data class AdminUser(
    val username: String,
    val token: String,
    val email: String? = null,
    val loginTime: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ServerMetadata(
    val doctorCount: Int,
    val specialtiesCount: Int,
    val governoratesCount: Int,
    val lastImportDate: Long?,
    val serverStatus: String = "online",
    val version: String = "1.0.0"
)
