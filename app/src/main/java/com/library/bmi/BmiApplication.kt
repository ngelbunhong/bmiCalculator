package com.library.bmi

import android.app.Application
import com.library.bmi.data.database.AppDatabase
import com.library.bmi.data.repository.BmiRepository
import kotlin.getValue

class BmiApplication : Application() {
    // 1. The database is created here, only when first needed.
    val database by lazy { AppDatabase.getDatabase(this) }

    // 2. The repository is created using the database's DAO.
    val repository by lazy { BmiRepository(database.bmiDao()) }
}