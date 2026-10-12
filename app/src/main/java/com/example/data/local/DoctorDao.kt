package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DoctorDao {

    @Query("SELECT * FROM doctors ORDER BY fullName ASC")
    fun getAllDoctors(): Flow<List<DoctorEntity>>

    @Query("SELECT * FROM doctors WHERE id = :id LIMIT 1")
    fun getDoctorById(id: String): Flow<DoctorEntity?>

    @Query("SELECT * FROM doctors WHERE isFavorite = 1 ORDER BY fullName ASC")
    fun getFavoriteDoctors(): Flow<List<DoctorEntity>>

    @Query("SELECT COUNT(*) FROM doctors")
    fun getDoctorsCount(): Flow<Int>

    @Query("SELECT DISTINCT specialty FROM doctors WHERE specialty != '' ORDER BY specialty ASC")
    fun getDistinctSpecialties(): Flow<List<String>>

    @Query("SELECT DISTINCT governorate FROM doctors WHERE governorate IS NOT NULL AND governorate != '' ORDER BY governorate ASC")
    fun getDistinctGovernorates(): Flow<List<String>>

    @Query("""
        SELECT * FROM doctors 
        WHERE normalizedName LIKE '%' || :query || '%' 
           OR fullName LIKE '%' || :query || '%'
           OR specialty LIKE '%' || :query || '%'
           OR governorate LIKE '%' || :query || '%'
        ORDER BY fullName ASC
    """)
    fun searchDoctors(query: String): Flow<List<DoctorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctors(doctors: List<DoctorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: DoctorEntity)

    @Query("UPDATE doctors SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("DELETE FROM doctors WHERE id = :id")
    suspend fun deleteDoctor(id: String)

    @Query("DELETE FROM doctors")
    suspend fun clearAll()

    @Query("SELECT id FROM doctors WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<String>
}

@Dao
interface ImportHistoryDao {
    @Query("SELECT * FROM import_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ImportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ImportHistoryEntity)

    @Query("SELECT COUNT(*) FROM import_history")
    fun getCount(): Flow<Int>
}
