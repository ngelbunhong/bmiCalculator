package com.library.bmi.utils

import android.text.InputFilter
import android.text.Spanned

class InputFilterMinMax(private val min: Float, private val max: Float) : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        try {
            // Handle empty input (backspace case)
            if (source.isEmpty()) return null

            // Combine existing text with new input
            val newText = dest.subSequence(0, dstart).toString() +
                    source.subSequence(start, end) +
                    dest.subSequence(dend, dest.length)

            // Handle cases where input might just be a decimal point
            if (newText == ".") return "0."

            val input = newText.toFloat()

            // Check range and decimal places
            if (isInRange(min, max, input) && hasValidDecimalPlaces(newText)) {
                return null
            }
        } catch (e: NumberFormatException) {
            // Handle invalid number format
        }
        return ""
    }

    private fun isInRange(min: Float, max: Float, input: Float): Boolean {
        return input in min..max
    }

    private fun hasValidDecimalPlaces(text: String): Boolean {
        // Limit to 2 decimal places
        if (text.contains('.')) {
            val parts = text.split('.')
            if (parts.size == 2 && parts[1].length > 2) {
                return false
            }
        }
        return true
    }
}