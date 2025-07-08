package com.library.bmi.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BmiDao {
    @Insert
    suspend fun insert(record: BmiRecord)

    // ✅ Add this function to delete a single record
    @Delete
    suspend fun delete(record: BmiRecord)

    // ✅ Add this function to clear the entire table
    @Query("DELETE FROM bmi_history")
    suspend fun deleteAll()

    @Query("SELECT * FROM bmi_history ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BmiRecord>>
}