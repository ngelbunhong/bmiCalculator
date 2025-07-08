package com.library.bmi.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.library.bmi.R
import com.library.bmi.data.database.BmiRecord
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter : ListAdapter<BmiRecord, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_record, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ✅ Helper function to get the BmiRecord at a given position.
     */
    fun getRecordAt(position: Int): BmiRecord {
        return getItem(position)
    }

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bmiTextView: TextView = itemView.findViewById(R.id.bmiValueTextView)
        private val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)

        private val detailsTextView: TextView = itemView.findViewById(R.id.detailsTextView)

        private val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        fun bind(record: BmiRecord) {
            bmiTextView.text = "BMI: %.1f".format(record.bmi)
            categoryTextView.text = record.category
            dateTextView.text = dateFormat.format(record.timestamp)
            // ✅ Set the combined details text
            val details = "Age: ${record.age}, ${record.gender}\n" +
                    "Weight: ${record.weight}, Height: ${record.height}"
            detailsTextView.text = details
        }
    }
}

class HistoryDiffCallback : DiffUtil.ItemCallback<BmiRecord>() {
    override fun areItemsTheSame(oldItem: BmiRecord, newItem: BmiRecord): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BmiRecord, newItem: BmiRecord): Boolean {
        return oldItem == newItem
    }
}