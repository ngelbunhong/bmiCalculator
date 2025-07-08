package com.library.bmi.data.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.library.bmi.BmiViewModel
import com.library.bmi.data.repository.BmiRepository

/**
 * Factory to create BmiViewModel with its dependencies (Application and Repository).
 */
class BmiViewModelFactory(
    private val application: Application,
    private val repository: BmiRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BmiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BmiViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}