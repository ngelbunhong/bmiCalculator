package com.library.bmi.data.repository

import com.library.bmi.data.database.BmiDao
import com.library.bmi.data.database.BmiRecord
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository that abstracts access to the data source.
 * The ViewModel will interact with this class instead of the DAO directly.
 */
class BmiRepository(private val bmiDao: BmiDao) {

    // Get all records as a Flow, which the ViewModel can collect.
    val allRecords: Flow<List<BmiRecord>> = bmiDao.getAllRecords()

    /**
     * A suspend function to insert a new record.
     * This will be called from a coroutine in the ViewModel.
     */
    suspend fun insert(
        bmi: Float, category: String, age: Int,
        gender: String,
        weight: String,
        height: String
    ) {
        val record = BmiRecord(
            timestamp = Date(),
            bmi = bmi,
            category = category,
            age = age,
            gender = gender,
            weight = weight,
            height = height
        )
        bmiDao.insert(record)
    }

    /**
     * ✅ Add this new function to re-insert an existing record for the Undo action.
     */
    suspend fun insert(record: BmiRecord) {
        bmiDao.insert(record)
    }

    // ✅ Add this function to call the DAO's delete method
    suspend fun delete(record: BmiRecord) {
        bmiDao.delete(record)
    }

    // ✅ Add this function to call the DAO's deleteAll method
    suspend fun deleteAll() {
        bmiDao.deleteAll()
    }
}