package com.library.bmi.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.library.bmi.data.database.BmiRecord
import com.library.bmi.data.repository.BmiRepository
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: BmiRepository) : ViewModel() {
    // Expose the Flow from the repository as LiveData for the UI to observe.
    val allRecords = repository.allRecords.asLiveData()

    /**
     * ✅ Insert a single record from the database.
     */
    fun insert(record: BmiRecord) = viewModelScope.launch {
        repository.insert(record)
    }

    /**
     * ✅ Deletes a single record from the database.
     */
    fun delete(record: BmiRecord) = viewModelScope.launch {
        repository.delete(record)
    }

    /**
     * ✅ Deletes all records from the database.
     */
    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }
}