package com.library.bmi

import android.app.Application
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.library.bmi.data.database.AppDatabase
import com.library.bmi.data.database.BmiRecord
import com.library.bmi.data.repository.BmiRepository
import kotlinx.coroutines.launch
import java.util.Date

// ✅ BmiResult now holds resource IDs and raw data for formatting.
data class BmiResult(
    val bmiValue: Float,
    @StringRes val categoryResId: Int,
    @ColorRes val colorRes: Int,
    val minHealthyKg: Float,
    val maxHealthyKg: Float,
    @StringRes val descriptionResId: Int,
    @StringRes val tipResId: Int
)

// ✅ Enum for type safety
enum class Gender { MALE, FEMALE }

class BmiViewModel(
    private val application: Application,
    private val repository: BmiRepository
) : ViewModel() {

    companion object {
        private const val BMI_NORMAL_LOWER_BOUND = 18.5f
        private const val BMI_NORMAL_UPPER_BOUND = 25f
        private const val IMPERIAL_BMI_FACTOR = 703f
        private const val LBS_TO_KG_FACTOR = 2.20462f
        private const val INCHES_TO_METERS = 0.0254f
    }


    // This will temporarily hold the inputs for saving.
    private var lastInputs: Triple<String, String, String>? = null
    private var lastGender: Gender? = null
    private var lastAge: String? = null
    private var isMetricMode: Boolean = true
    private val _bmiResult = MutableLiveData<BmiResult?>()
    val bmiResult: LiveData<BmiResult?> = _bmiResult

    // ✅ Error event now sends a String Resource ID.
    private val _errorEvent = MutableLiveData<Int>()
    val errorEvent: LiveData<Int> = _errorEvent

    fun calculate(
        isMetric: Boolean,
        ageStr: String,
        gender: Gender?,
        weightStr: String,
        heightStr: String,
        inchesStr: String = ""
    ) {
        try {
            val age = ageStr.toInt()
            // Basic validation for new fields
            if (age <= 0 || gender == null) {
                _errorEvent.value = R.string.error_invalid_numbers
                return
            }
            if (isMetric) {
                // ... (validation logic is the same)
                processMetric(weightStr.toFloat(), heightStr.toFloat())
            } else {
                // ... (validation logic is the same)
                processImperial(weightStr.toFloat(), heightStr.toInt(), inchesStr.toInt())
            }
            // ✅ Store the inputs when a calculation is made
            this.isMetricMode = isMetric
            this.lastAge = ageStr
            this.lastGender = gender
            this.lastInputs = Triple(weightStr, heightStr, inchesStr)
        } catch (e: NumberFormatException) {
            // ✅ Send the resource ID for the error.
            _errorEvent.value = R.string.error_invalid_numbers
        }
    }

    fun saveCurrentResult() {
        val result = bmiResult.value ?: return // Don't save if there's no result
        val age = lastAge?.toIntOrNull() ?: return
        val gender = lastGender ?: return
        val (weight, height, inches) = lastInputs ?: return
        val weightText: String
        val heightText: String

        if (isMetricMode) {
            weightText = "$weight kg"
            heightText = "$height cm"
        } else {
            weightText = "$weight lbs"
            heightText = "$height' $inches\""
        }


        // Use a coroutine to call the suspend function in the repository
        viewModelScope.launch {
            val category = application.getString(result.categoryResId)
            repository.insert(
                result.bmiValue, category, age = age,
                gender = gender.name.replaceFirstChar { it.titlecase() }, // "MALE" -> "Male"
                weight = weightText,
                height = heightText
            )
        }
    }

    private fun processMetric(weightKg: Float, heightCm: Float) {
        val heightM = heightCm / 100
        val bmi = weightKg / (heightM * heightM)
        updateResult(bmi, heightM)
    }

    private fun processImperial(weightLbs: Float, feet: Int, inches: Int) {
        val totalInches = (feet * 12 + inches).toFloat()
        val bmi = IMPERIAL_BMI_FACTOR * (weightLbs / (totalInches * totalInches))
        val heightM = totalInches * INCHES_TO_METERS
        updateResult(bmi, heightM)
    }

    private fun updateResult(bmi: Float, heightM: Float) {
        val (categoryResId, colorRes) = getBmiCategory(bmi)
        val minHealthyKg = BMI_NORMAL_LOWER_BOUND * (heightM * heightM)
        val maxHealthyKg = BMI_NORMAL_UPPER_BOUND * (heightM * heightM)

        _bmiResult.value = BmiResult(
            bmiValue = bmi,
            categoryResId = categoryResId,
            colorRes = colorRes,
            minHealthyKg = minHealthyKg,
            descriptionResId = getClassificationDescription(bmi),
            maxHealthyKg = maxHealthyKg,
            tipResId = getPersonalizedTip(bmi)

        )
    }

    // ✅ New function to select a tip based on BMI
    @StringRes
    private fun getPersonalizedTip(bmi: Float): Int {
        return when {
            bmi < 16f -> R.string.tip_severe_thinness
            bmi < 17f -> R.string.tip_moderate_thinness
            bmi < 18.5f -> R.string.tip_mild_thinness
            bmi < 25f -> R.string.tip_normal
            bmi < 30f -> R.string.tip_overweight
            bmi < 35f -> R.string.tip_obese1
            bmi < 40f -> R.string.tip_obese2
            else -> R.string.tip_obese3
        }
    }

    fun clearResult() {
        _bmiResult.value = null
    }

    // ✅ Now returns Pair<StringRes, ColorRes>
    private fun getBmiCategory(bmi: Float): Pair<Int, Int> {
        return when {
            bmi < 16f -> Pair(R.string.category_severe_thinness, R.color.severeThinness)
            bmi < 17f -> Pair(R.string.category_moderate_thinness, R.color.moderateThinness)
            bmi < 18.5f -> Pair(R.string.category_mild_thinness, R.color.mildThinness)
            bmi < 25f -> Pair(R.string.category_normal, R.color.normal)
            bmi < 30f -> Pair(R.string.category_overweight, R.color.overweight)
            bmi < 35f -> Pair(R.string.category_obese1, R.color.obese1)
            bmi < 40f -> Pair(R.string.category_obese2, R.color.obese2)
            else -> Pair(R.string.category_obese3, R.color.obese3)
        }
    }

    // ✅ Now returns @StringRes Int
    @StringRes
    private fun getClassificationDescription(bmi: Float): Int {
        return when {
            bmi < 16f -> R.string.desc_severe_thinness
            bmi < 17f -> R.string.desc_moderate_thinness
            bmi < 18.5f -> R.string.desc_mild_thinness
            bmi < 25f -> R.string.desc_normal
            bmi < 30f -> R.string.desc_overweight
            bmi < 35f -> R.string.desc_obese1
            bmi < 40f -> R.string.desc_obese2
            else -> R.string.desc_obese3
        }
    }

    fun kilogramsToPounds(kg: Float): Float = kg * LBS_TO_KG_FACTOR
}