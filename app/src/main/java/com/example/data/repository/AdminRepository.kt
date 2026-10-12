package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.DoctorEntity
import com.example.data.local.ImportHistoryEntity
import com.example.data.model.AdminUser
import com.example.data.model.Doctor
import com.example.data.model.ImportHistoryItem
import com.example.data.model.ImportMode
import com.example.data.model.ImportReport
import com.example.data.remote.ApiClient
import com.example.data.remote.BatchImportRequest
import com.example.data.remote.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class AdminRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val doctorDao = db.doctorDao()
    private val importHistoryDao = db.importHistoryDao()

    fun isLoggedIn(): Boolean = ApiClient.isAuthenticated()

    fun getLoggedAdmin(): AdminUser? {
        val username = ApiClient.getAdminUsername(context)
        return if (isLoggedIn() && username != null) {
            AdminUser(username = username, token = "active")
        } else null
    }

    suspend fun login(username: String, password: String): Result<AdminUser> = withContext(Dispatchers.IO) {
        val trimmedUser = username.trim()

        // 1. Try remote server authentication first
        try {
            val response = ApiClient.getService().login(LoginRequest(username = trimmedUser, password = password))
            if (response.isSuccessful && response.body()?.success == true) {
                val token = response.body()?.token.orEmpty()
                val adminName = response.body()?.username ?: trimmedUser
                ApiClient.saveAuthToken(context, token, adminName)
                return@withContext Result.success(AdminUser(username = adminName, token = token))
            } else if (response.code() == 401 || response.code() == 400) {
                val errMessage = response.body()?.message ?: "بيانات الدخول غير صحيحة. تحقق من اسم المستخدم وكلمة المرور."
                return@withContext Result.failure(Exception(errMessage))
            }
        } catch (_: Exception) {
            // Server is offline or unreachable - fall back to local admin check
        }

        // 2. Local fallback credentials (configured for offline / emulator testing)
        val defaultAdminUser = "admin"
        val defaultAdminPass = "AdminSecurePassword2026!"

        if (trimmedUser == defaultAdminUser && password == defaultAdminPass) {
            val localToken = "local_admin_token_${System.currentTimeMillis()}"
            ApiClient.saveAuthToken(context, localToken, defaultAdminUser)
            return@withContext Result.success(AdminUser(username = defaultAdminUser, token = localToken))
        }

        Result.failure(Exception("بيانات الدخول غير صحيحة. يرجى التحقق من اسم المستخدم وكلمة المرور."))
    }

    fun logout() {
        ApiClient.clearAuthToken(context)
    }

    /**
     * Executes the import workflow with the chosen mode (Append, Merge, or Replace All).
     * Updates server central database if reachable, and updates local Room database.
     */
    suspend fun executeImport(
        fileName: String,
        mode: ImportMode,
        doctorsToImport: List<Doctor>
    ): Result<ImportReport> = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val importId = UUID.randomUUID().toString()

        var serverSucceeded = false
        var serverReport: ImportReport? = null

        // 1. Try sending to central server first
        try {
            val req = BatchImportRequest(
                fileName = fileName,
                importMode = mode.name,
                doctors = doctorsToImport
            )
            val resp = ApiClient.getService().batchImport(req)
            if (resp.isSuccessful && resp.body() != null) {
                serverReport = resp.body()
                serverSucceeded = true
            }
        } catch (_: Exception) {
            serverSucceeded = false
        }

        // 2. Perform safe local atomic transaction in Room
        try {
            when (mode) {
                ImportMode.REPLACE_ALL -> {
                    doctorDao.clearAll()
                    val entities = doctorsToImport.map { DoctorEntity.fromDoctor(it) }
                    doctorDao.insertDoctors(entities)
                }
                ImportMode.MERGE -> {
                    val entities = doctorsToImport.map { DoctorEntity.fromDoctor(it) }
                    doctorDao.insertDoctors(entities)
                }
                ImportMode.APPEND -> {
                    val entities = doctorsToImport.map { DoctorEntity.fromDoctor(it) }
                    doctorDao.insertDoctors(entities)
                }
            }

            // Save history record
            val historyEntity = ImportHistoryEntity(
                id = importId,
                fileName = fileName,
                importMode = mode.titleArabic,
                addedCount = serverReport?.addedCount ?: doctorsToImport.size,
                updatedCount = serverReport?.updatedCount ?: 0,
                skippedCount = serverReport?.skippedCount ?: 0,
                timestamp = timestamp,
                success = true
            )
            importHistoryDao.insert(historyEntity)

            val finalReport = serverReport ?: ImportReport(
                id = importId,
                fileName = fileName,
                importMode = mode,
                addedCount = doctorsToImport.size,
                updatedCount = 0,
                skippedCount = 0,
                errorCount = 0,
                timestamp = timestamp,
                message = if (serverSucceeded) "تم استيراد وحفظ البيانات بنجاح في الخادم وقاعدة البيانات المركزية."
                else "تم حفظ البيانات محلياً. (ملاحظة: الخادم المركزي لم يكن متصلاً أثناء الرفع).",
                success = true
            )

            Result.success(finalReport)
        } catch (e: Exception) {
            Result.failure(Exception("فشلت عملية الاستيراد: ${e.localizedMessage}"))
        }
    }

    fun getImportHistory(): Flow<List<ImportHistoryItem>> {
        return importHistoryDao.getAllHistory().map { list ->
            list.map {
                ImportHistoryItem(
                    id = it.id,
                    fileName = it.fileName,
                    importMode = it.importMode,
                    addedCount = it.addedCount,
                    updatedCount = it.updatedCount,
                    skippedCount = it.skippedCount,
                    timestamp = it.timestamp,
                    success = it.success
                )
            }
        }
    }

    suspend fun saveDoctorManually(doctor: Doctor): Result<Doctor> = withContext(Dispatchers.IO) {
        try {
            // Save to server if connected
            var finalDoc = doctor
            try {
                val resp = ApiClient.getService().createDoctor(doctor)
                if (resp.isSuccessful && resp.body() != null) {
                    finalDoc = resp.body()!!
                }
            } catch (_: Exception) {}

            doctorDao.insertDoctor(DoctorEntity.fromDoctor(finalDoc))
            Result.success(finalDoc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDoctor(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            try {
                ApiClient.getService().deleteDoctor(id)
            } catch (_: Exception) {}
            doctorDao.deleteDoctor(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
