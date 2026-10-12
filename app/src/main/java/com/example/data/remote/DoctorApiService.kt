package com.example.data.remote

import com.example.data.model.Doctor
import com.example.data.model.ImportHistoryItem
import com.example.data.model.ImportReport
import com.example.data.model.ServerMetadata
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val status: String,
    val timestamp: Long,
    val message: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val username: String?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val doctors: List<Doctor>,
    val totalCount: Int,
    val page: Int,
    val totalPages: Int
)

@JsonClass(generateAdapter = true)
data class BatchImportRequest(
    val fileName: String,
    val importMode: String,
    val doctors: List<Doctor>
)

@JsonClass(generateAdapter = true)
data class GenericResponse(
    val success: Boolean,
    val message: String
)

interface DoctorApiService {

    @GET("api/health")
    suspend fun checkHealth(): Response<HealthResponse>

    @GET("api/doctors/meta/summary")
    suspend fun getSummary(): Response<ServerMetadata>

    @GET("api/doctors")
    suspend fun searchDoctors(
        @Query("q") query: String? = null,
        @Query("specialty") specialty: String? = null,
        @Query("governorate") governorate: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): Response<SearchResponse>

    @GET("api/doctors/{id}")
    suspend fun getDoctorById(@Path("id") id: String): Response<Doctor>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/admin/doctors/batch-import")
    suspend fun batchImport(@Body request: BatchImportRequest): Response<ImportReport>

    @GET("api/admin/import-history")
    suspend fun getImportHistory(): Response<List<ImportHistoryItem>>

    @POST("api/admin/doctors")
    suspend fun createDoctor(@Body doctor: Doctor): Response<Doctor>

    @PUT("api/admin/doctors/{id}")
    suspend fun updateDoctor(@Path("id") id: String, @Body doctor: Doctor): Response<Doctor>

    @DELETE("api/admin/doctors/{id}")
    suspend fun deleteDoctor(@Path("id") id: String): Response<GenericResponse>
}
