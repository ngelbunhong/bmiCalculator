package com.library.bmi.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "bmi_history")
data class BmiRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Date,
    val bmi: Float,
    val category: String,
    val age: Int,
    val gender: String,
    val weight: String, // Storing as String to include units (e.g., "70 kg" or "154 lbs")
    val height: String  // Storing as String to include units (e.g., "175 cm" or "5' 9\"")
    // ✅ END: Added new fields
)
