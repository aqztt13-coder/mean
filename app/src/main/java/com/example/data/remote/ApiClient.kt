package com.example.data.remote

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val PREFS_NAME = "dalilik_api_prefs"
    private const val KEY_BASE_URL = "custom_base_url"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_ADMIN_USER = "admin_username"

    // Default development server URL (Standard Android Emulator localhost mapping)
    const val DEFAULT_BASE_URL = "http://10.0.2.2:5000/"

    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var authToken: String? = null
    private var cachedService: DoctorApiService? = null

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentBaseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        if (!currentBaseUrl.endsWith("/")) {
            currentBaseUrl += "/"
        }
        authToken = prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun getBaseUrl(): String = currentBaseUrl

    fun setBaseUrl(context: Context, newUrl: String) {
        var cleanUrl = newUrl.trim()
        if (!cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        currentBaseUrl = cleanUrl
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, cleanUrl)
            .apply()
        cachedService = null // Force recreation with new URL
    }

    fun saveAuthToken(context: Context, token: String, username: String) {
        authToken = token
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_ADMIN_USER, username)
            .apply()
        cachedService = null
    }

    fun clearAuthToken(context: Context) {
        authToken = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_ADMIN_USER)
            .apply()
        cachedService = null
    }

    fun getAdminUsername(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ADMIN_USER, null)
    }

    fun isAuthenticated(): Boolean {
        return !authToken.isNullOrBlank()
    }

    fun getService(): DoctorApiService {
        val existing = cachedService
        if (existing != null) return existing

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            authToken?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            requestBuilder.addHeader("Accept", "application/json")
            requestBuilder.addHeader("Content-Type", "application/json")
            chain.proceed(requestBuilder.build())
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val service = retrofit.create(DoctorApiService::class.java)
        cachedService = service
        return service
    }
}
