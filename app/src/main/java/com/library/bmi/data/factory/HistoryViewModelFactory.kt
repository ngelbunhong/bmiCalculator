package com.library.bmi.data.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.library.bmi.data.repository.BmiRepository
import com.library.bmi.ui.history.HistoryViewModel

/**
 * Factory to create HistoryViewModel with its dependency (Repository).
 */
class HistoryViewModelFactory(private val repository: BmiRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}