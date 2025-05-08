package com.library.bmi

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.drawable.DrawableUtils
import com.library.bmi.databinding.ActivityMainBinding
import androidx.core.graphics.drawable.toDrawable

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideKeyboard()
        setupUnitToggle()
        setupCalculateButton()
        setupReferenceButton()
    }


    private fun setupUnitToggle() {
        binding.unitRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.metricRadioButton -> {
                    binding.metricInputCard.visibility = View.VISIBLE
                    binding.imperialInputCard.visibility = View.GONE
                }

                R.id.imperialRadioButton -> {
                    binding.metricInputCard.visibility = View.GONE
                    binding.imperialInputCard.visibility = View.VISIBLE
                }
            }
            clearResults()
        }
    }

    private fun setupCalculateButton() {
        binding.calculateButton.setOnClickListener {
            calculateBMI()
        }
    }

    private fun setupReferenceButton() {
        binding.referenceButton.setOnClickListener {
            showClassificationReference()
        }
    }

    private fun calculateBMI() {
        try {
            if (binding.metricRadioButton.isChecked) {
                calculateMetricBMI()
            } else {
                calculateImperialBMI()
            }
        } catch (e: Exception) {
            showError("Please enter valid numbers")
        }
    }

    private fun calculateMetricBMI() {
        val weightKg = binding.weightEditText.text.toString().toFloat()
        val heightCm = binding.heightEditText.text.toString().toFloat()

        if (weightKg <= 0 || heightCm <= 0) {
            showError("Values must be positive")
            return
        }

        val bmi = calculateMetricBMI(weightKg, heightCm)
        val heightM = heightCm / 100
        displayResults(bmi, weightKg, heightM)
    }

    private fun calculateImperialBMI() {
        val weightLbs = binding.weightImperialEditText.text.toString().toFloat()
        val feet = binding.feetEditText.text.toString().toInt()
        val inches = binding.inchesEditText.text.toString().toInt()

        if (weightLbs <= 0 || feet <= 0 || inches < 0) {
            showError("Values must be positive")
            return
        }

        val totalInches = feetAndInchesToInches(feet, inches)
        val bmi = calculateImperialBMI(weightLbs, totalInches)
        val weightKg = poundsToKilograms(weightLbs)
        val heightM = inchesToMeters(totalInches)
        displayResults(bmi, weightKg, heightM)
    }

    // Metric BMI calculation (kg and cm)
    private fun calculateMetricBMI(weightKg: Float, heightCm: Float): Float {
        val heightM = heightCm / 100
        return weightKg / (heightM * heightM)
    }

    // Imperial BMI calculation (lbs and inches)
    private fun calculateImperialBMI(weightLbs: Float, heightIn: Float): Float {
        return 703f * (weightLbs / (heightIn * heightIn))
    }

    private fun displayResults(bmi: Float, weightKg: Float, heightM: Float) {
        // Animation
        ObjectAnimator.ofFloat(binding.resultCard, "alpha", 0f, 1f).apply {
            duration = 500
            start()
        }
        binding.resultCard.visibility = View.VISIBLE

        // Get category and color
        val (category, colorRes) = getBmiCategory(bmi)
        val color = ContextCompat.getColor(this, colorRes)

        // Display results
        binding.resultTextView.text = "BMI: %.1f".format(bmi)
        binding.categoryTextView.text = category
        binding.categoryTextView.setTextColor(color)

        // Calculate healthy weight range (18.5-25 BMI)
        val minHealthyKg = 18.5f * (heightM * heightM)
        val maxHealthyKg = 25f * (heightM * heightM)

        // Display range in current units
        if (binding.metricRadioButton.isChecked) {
            binding.rangeTextView.text =
                "Healthy range: %.1f - %.1f kg".format(minHealthyKg, maxHealthyKg)
        } else {
            binding.rangeTextView.text = "Healthy range: %.1f - %.1f lbs".format(
                kilogramsToPounds(minHealthyKg),
                kilogramsToPounds(maxHealthyKg)
            )
        }

        // Set classification info
        binding.classificationTextView.text = getClassificationDescription(bmi)
    }

    private fun getBmiCategory(bmi: Float): Pair<String, Int> {
        return when {
            bmi < 16f -> Pair("Severe Thinness (<16)", R.color.severeThinness)
            bmi < 17f -> Pair("Moderate Thinness (16-17)", R.color.moderateThinness)
            bmi < 18.5f -> Pair("Mild Thinness (17-18.5)", R.color.mildThinness)
            bmi < 25f -> Pair("Normal (18.5-25)", R.color.normal)
            bmi < 30f -> Pair("Overweight (25-30)", R.color.overweight)
            bmi < 35f -> Pair("Obese Class I (30-35)", R.color.obese1)
            bmi < 40f -> Pair("Obese Class II (35-40)", R.color.obese2)
            else -> Pair("Obese Class III (>40)", R.color.obese3)
        }
    }

    private fun getClassificationDescription(bmi: Float): String {
        return when {
            bmi < 16f -> "Severely underweight - High health risk, consult doctor"
            bmi < 17f -> "Moderately underweight - Moderate health risk"
            bmi < 18.5f -> "Mildly underweight - Mild health risk"
            bmi < 25f -> "Healthy weight - Low health risk"
            bmi < 30f -> "Overweight - Increased health risk"
            bmi < 35f -> "Obese Class I - High health risk"
            bmi < 40f -> "Obese Class II - Very high health risk"
            else -> "Obese Class III - Extremely high health risk"
        }
    }

    private fun showClassificationReference() {

        MaterialAlertDialogBuilder(this)
            .setTitle("WHO BMI Classification")
            .setBackground(ContextCompat.getDrawable(this,R.drawable.dialog_background)) // or any color
            .setMessage(
                """
                Severe Thinness: < 16
                Moderate Thinness: 16 - 17
                Mild Thinness: 17 - 18.5
                Normal: 18.5 - 25
                Overweight: 25 - 30
                Obese Class I: 30 - 35
                Obese Class II: 35 - 40
                Obese Class III: > 40
                
                Note: BMI is a screening tool but not diagnostic.
                Muscle mass, age, and ethnicity can affect interpretation.
            """.trimIndent()
            )
            .setPositiveButton("OK", null)
            .show()
    }

    // Conversion functions
    private fun poundsToKilograms(lbs: Float): Float = lbs / 2.20462f
    private fun kilogramsToPounds(kg: Float): Float = kg * 2.20462f
    private fun inchesToMeters(inches: Float): Float = inches * 0.0254f
    private fun feetAndInchesToInches(feet: Int, inches: Int): Float =
        (feet * 12 + inches).toFloat()

    private fun clearResults() {
        binding.weightEditText.text = null
        binding.heightEditText.text = null
        binding.feetEditText.text = null
        binding.inchesEditText.text = null
        binding.weightImperialEditText.text = null
        binding.resultCard.visibility = View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}