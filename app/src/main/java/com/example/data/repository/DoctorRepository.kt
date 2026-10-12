package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.DoctorEntity
import com.example.data.model.Doctor
import com.example.data.model.ServerMetadata
import com.example.data.remote.ApiClient
import com.example.util.ArabicSearchNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed class ServerConnectionState {
    object Checking : ServerConnectionState()
    data class Connected(val url: String, val lastChecked: Long) : ServerConnectionState()
    data class Offline(val reason: String) : ServerConnectionState()
}

class DoctorRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val doctorDao = db.doctorDao()

    private val _connectionState = MutableStateFlow<ServerConnectionState>(ServerConnectionState.Checking)
    val connectionState = _connectionState.asStateFlow()

    private val _serverMetadata = MutableStateFlow<ServerMetadata?>(null)
    val serverMetadata = _serverMetadata.asStateFlow()

    init {
        ApiClient.initialize(context)
    }

    /**
     * Checks if the backend server is reachable and active.
     */
    suspend fun checkServerHealth(): Boolean = withContext(Dispatchers.IO) {
        _connectionState.value = ServerConnectionState.Checking
        try {
            val response = ApiClient.getService().checkHealth()
            if (response.isSuccessful && response.body()?.status == "ok") {
                _connectionState.value = ServerConnectionState.Connected(
                    url = ApiClient.getBaseUrl(),
                    lastChecked = System.currentTimeMillis()
                )
                refreshMetadata()
                return@withContext true
            } else {
                _connectionState.value = ServerConnectionState.Offline(
                    "استجابة غير متوقعة من الخادم: كود ${response.code()}"
                )
                return@withContext false
            }
        } catch (e: Exception) {
            _connectionState.value = ServerConnectionState.Offline(
                "تعذر الاتصال بالخادم المركزي (${e.localizedMessage ?: "انقطاع الشبكة"}). استخدام البيانات المحلية المؤقتة."
            )
            return@withContext false
        }
    }

    /**
     * Refreshes server summary statistics if connected.
     */
    suspend fun refreshMetadata() = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService().getSummary()
            if (response.isSuccessful) {
                _serverMetadata.value = response.body()
            }
        } catch (_: Exception) {}
    }

    /**
     * Fetches doctors from server if online, caches them into Room, or retrieves from Room.
     */
    suspend fun syncDoctorsFromServer(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService().searchDoctors(limit = 1000)
            if (response.isSuccessful) {
                val serverDoctors = response.body()?.doctors.orEmpty()
                val existingFavorites = doctorDao.getFavoriteIds().toSet()

                val entities = serverDoctors.map { doctor ->
                    val isFav = existingFavorites.contains(doctor.id) || doctor.isFavorite
                    DoctorEntity.fromDoctor(doctor, isFavorite = isFav)
                }

                // If server has doctors, update local cache
                if (entities.isNotEmpty()) {
                    doctorDao.insertDoctors(entities)
                }

                _connectionState.value = ServerConnectionState.Connected(
                    url = ApiClient.getBaseUrl(),
                    lastChecked = System.currentTimeMillis()
                )
                refreshMetadata()
                Result.success(entities.size)
            } else {
                Result.failure(Exception("خطأ في الخادم: ${response.code()}"))
            }
        } catch (e: Exception) {
            _connectionState.value = ServerConnectionState.Offline(
                "الخادم غير متاح حالياً (${e.localizedMessage}). يتم استخدام البيانات المحلية المخزنة."
            )
            Result.failure(e)
        }
    }

    /**
     * Observes doctors from local Room database.
     */
    fun getAllDoctorsLocal(): Flow<List<Doctor>> {
        return doctorDao.getAllDoctors().map { list -> list.map { it.toDoctor() } }
    }

    /**
     * Observes total registered doctor count.
     */
    fun getDoctorsCount(): Flow<Int> {
        return doctorDao.getDoctorsCount()
    }

    /**
     * Observes distinct specialties.
     */
    fun getSpecialties(): Flow<List<String>> {
        return doctorDao.getDistinctSpecialties()
    }

    /**
     * Observes distinct governorates.
     */
    fun getGovernorates(): Flow<List<String>> {
        return doctorDao.getDistinctGovernorates()
    }

    /**
     * Smart Search engine with Arabic normalization and match scoring.
     */
    suspend fun searchDoctors(
        query: String,
        specialtyFilter: String? = null,
        governorateFilter: String? = null
    ): List<Doctor> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        val allLocal = doctorDao.getAllDoctors().first().map { it.toDoctor() }

        if (allLocal.isEmpty()) {
            // Attempt server search if local is empty
            try {
                val resp = ApiClient.getService().searchDoctors(
                    query = if (trimmedQuery.isNotBlank()) trimmedQuery else null,
                    specialty = specialtyFilter,
                    governorate = governorateFilter,
                    limit = 200
                )
                if (resp.isSuccessful) {
                    val remote = resp.body()?.doctors.orEmpty()
                    if (remote.isNotEmpty()) {
                        doctorDao.insertDoctors(remote.map { DoctorEntity.fromDoctor(it) })
                        return@withContext remote
                    }
                }
            } catch (_: Exception) {}
        }

        // Apply filters & smart Arabic ranking
        var filtered = allLocal

        if (!specialtyFilter.isNullOrBlank() && specialtyFilter != "الكل") {
            filtered = filtered.filter { it.specialty.equals(specialtyFilter, ignoreCase = true) }
        }

        if (!governorateFilter.isNullOrBlank() && governorateFilter != "الكل") {
            filtered = filtered.filter {
                it.governorate?.equals(governorateFilter, ignoreCase = true) == true
            }
        }

        if (trimmedQuery.isBlank()) {
            return@withContext filtered.sortedBy { it.fullName }
        }

        // Score each doctor using Arabic normalizer
        val scoredList = filtered.mapNotNull { doctor ->
            val nameScore = ArabicSearchNormalizer.calculateMatchScore(trimmedQuery, doctor.fullName)
            val specScore = if (ArabicSearchNormalizer.matches(trimmedQuery, doctor.specialty)) 35 else 0
            val govScore = if (ArabicSearchNormalizer.matches(trimmedQuery, doctor.governorate)) 30 else 0
            val clinicScore = if (ArabicSearchNormalizer.matches(trimmedQuery, doctor.clinicName)) 25 else 0
            val cityScore = if (ArabicSearchNormalizer.matches(trimmedQuery, doctor.city)) 20 else 0

            val bestScore = maxOf(nameScore, specScore, govScore, clinicScore, cityScore)
            if (bestScore > 0) {
                Pair(doctor, bestScore)
            } else {
                null
            }
        }

        return@withContext scoredList
            .sortedWith(
                compareByDescending<Pair<Doctor, Int>> { it.second }
                    .thenBy { it.first.fullName }
            )
            .map { it.first }
    }

    /**
     * Get doctor by ID.
     */
    fun getDoctorById(id: String): Flow<Doctor?> {
        return doctorDao.getDoctorById(id).map { it?.toDoctor() }
    }

    /**
     * Toggle favorite status.
     */
    suspend fun toggleFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        doctorDao.updateFavoriteStatus(id, isFavorite)
    }

    /**
     * Get favorite doctors.
     */
    fun getFavoriteDoctors(): Flow<List<Doctor>> {
        return doctorDao.getFavoriteDoctors().map { list -> list.map { it.toDoctor() } }
    }

    /**
     * Clear all doctors locally.
     */
    suspend fun clearLocalCache() = withContext(Dispatchers.IO) {
        doctorDao.clearAll()
    }
}
