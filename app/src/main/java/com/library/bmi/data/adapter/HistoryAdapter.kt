package com.library.bmi.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
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

    fun getRecordAt(position: Int): BmiRecord {
        return getItem(position)
    }

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val bmiTextView: TextView = itemView.findViewById(R.id.bmiValueTextView)
        private val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val detailsTextView: TextView = itemView.findViewById(R.id.detailsTextView)
        private val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        fun bind(record: BmiRecord) {
            bmiTextView.text = itemView.context.getString(R.string.bmi_result_format, record.bmi)
            categoryTextView.text = record.category
            dateTextView.text = dateFormat.format(record.timestamp)
            val details = "Age: ${record.age}, ${record.gender}\n" +
                    "Weight: ${record.weight}, Height: ${record.height}"
            detailsTextView.text = details

            // ✅ START: Logic to set card and text color
            val (cardColorRes, textColorRes) = getBmiColors(record.bmi)

            val cardColor = ContextCompat.getColor(itemView.context, cardColorRes)
            val textColor = ContextCompat.getColor(itemView.context, textColorRes)

            cardView.setCardBackgroundColor(cardColor)

            // Set all text views inside the card to the appropriate color for readability
            bmiTextView.setTextColor(textColor)
            categoryTextView.setTextColor(textColor)
            dateTextView.setTextColor(textColor)
            detailsTextView.setTextColor(textColor)
            // ✅ END: Logic to set card and text color
        }

        /**
         * Determines the background color for the card and the appropriate text color
         * for readability based on the BMI value.
         *
         * @return A Pair containing the resource ID for the card color and the text color.
         */
        private fun getBmiColors(bmi: Float): Pair<Int, Int> {
            val cardColor: Int
            val textColor: Int

            when {
                bmi < 16f -> {
                    cardColor = R.color.severeThinness
                    textColor = R.color.white
                }
                bmi < 17f -> {
                    cardColor = R.color.moderateThinness
                    textColor = R.color.white
                }
                bmi < 18.5f -> {
                    cardColor = R.color.mildThinness
                    textColor = R.color.white
                }
                bmi < 25f -> {
                    cardColor = R.color.normal
                    textColor = R.color.white
                }
                bmi < 30f -> {
                    cardColor = R.color.overweight
                    textColor = R.color.black
                }
                bmi < 35f -> {
                    cardColor = R.color.obese1
                    textColor = R.color.white
                }
                bmi < 40f -> {
                    cardColor = R.color.obese2
                    textColor = R.color.white
                }
                else -> {
                    cardColor = R.color.obese3
                    textColor = R.color.white
                }
            }
            return Pair(cardColor, textColor)
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
