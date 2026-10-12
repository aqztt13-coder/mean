package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Doctor
import org.json.JSONObject

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey
    val id: String,
    val fullName: String,
    val normalizedName: String,
    val specialty: String,
    val subSpecialty: String?,
    val title: String?,
    val phonePrimary: String,
    val phoneSecondary: String?,
    val governorate: String?,
    val city: String?,
    val address: String?,
    val clinicName: String?,
    val workplace: String?,
    val workingHours: String?,
    val notes: String?,
    val latitude: Double?,
    val longitude: Double?,
    val extraAttributesJson: String,
    val isFavorite: Boolean,
    val cachedAt: Long
) {
    fun toDoctor(): Doctor {
        val extras = mutableMapOf<String, String>()
        if (extraAttributesJson.isNotBlank()) {
            try {
                val json = JSONObject(extraAttributesJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    extras[key] = json.optString(key)
                }
            } catch (_: Exception) {}
        }

        return Doctor(
            id = id,
            fullName = fullName,
            normalizedName = normalizedName,
            specialty = specialty,
            subSpecialty = subSpecialty,
            title = title,
            phonePrimary = phonePrimary,
            phoneSecondary = phoneSecondary,
            governorate = governorate,
            city = city,
            address = address,
            clinicName = clinicName,
            workplace = workplace,
            workingHours = workingHours,
            notes = notes,
            latitude = latitude,
            longitude = longitude,
            extraAttributes = extras,
            isFavorite = isFavorite,
            updatedAt = cachedAt
        )
    }

    companion object {
        fun fromDoctor(doctor: Doctor, isFavorite: Boolean = doctor.isFavorite): DoctorEntity {
            val json = JSONObject()
            doctor.extraAttributes.forEach { (k, v) ->
                json.put(k, v)
            }

            return DoctorEntity(
                id = doctor.id,
                fullName = doctor.fullName,
                normalizedName = doctor.normalizedName,
                specialty = doctor.specialty,
                subSpecialty = doctor.subSpecialty,
                title = doctor.title,
                phonePrimary = doctor.phonePrimary,
                phoneSecondary = doctor.phoneSecondary,
                governorate = doctor.governorate,
                city = doctor.city,
                address = doctor.address,
                clinicName = doctor.clinicName,
                workplace = doctor.workplace,
                workingHours = doctor.workingHours,
                notes = doctor.notes,
                latitude = doctor.latitude,
                longitude = doctor.longitude,
                extraAttributesJson = json.toString(),
                isFavorite = isFavorite,
                cachedAt = doctor.updatedAt
            )
        }
    }
}

@Entity(tableName = "import_history")
data class ImportHistoryEntity(
    @PrimaryKey
    val id: String,
    val fileName: String,
    val importMode: String,
    val addedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val timestamp: Long,
    val success: Boolean
)
