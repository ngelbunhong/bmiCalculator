package com.library.bmi

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.library.bmi.data.factory.BmiViewModelFactory
import com.library.bmi.databinding.ActivityMainBinding
import com.library.bmi.ui.history.HistoryActivity

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: BmiViewModel by viewModels {
        BmiViewModelFactory(
            application,
            (application as BmiApplication).repository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideKeyboard()
        setupUnitToggle()
        setupCalculateButton()
        setupReferenceButton()
        observeViewModel()
        setupSaveButton()
        setupHistoryButton()
    }

    // Observe the LiveData from the ViewModel
    private fun observeViewModel() {
        viewModel.bmiResult.observe(this) { result ->
            if (result != null) {
                displayResults(result)
            } else {
                binding.resultCard.visibility = View.GONE
            }
        }

        // ✅ Observe the error resource ID and get the string here.
        viewModel.errorEvent.observe(this) { messageResId ->
            showError(getString(messageResId))
        }
    }

    private fun setupUnitToggle() {
        binding.unitRadioGroup.setOnCheckedChangeListener { _, _ ->
            // Switch visibility
            binding.metricInputCard.visibility = if (binding.metricRadioButton.isChecked) View.VISIBLE else View.GONE
            binding.imperialInputCard.visibility = if (binding.imperialRadioButton.isChecked) View.VISIBLE else View.GONE
            clearUi()
        }
    }

    private fun setupCalculateButton() {
        binding.calculateButton.setOnClickListener {
            // ✅ Read the new age and gender inputs from the UI
            val age = binding.ageEditText.text.toString()
            val selectedGender = when (binding.genderRadioGroup.checkedRadioButtonId) {
                R.id.maleRadioButton -> Gender.MALE
                R.id.femaleRadioButton -> Gender.FEMALE
                else -> null
            }

            // Send all inputs to the ViewModel for calculation
            if (binding.metricRadioButton.isChecked) {
                viewModel.calculate(
                    isMetric = true,
                    ageStr = age,
                    gender = selectedGender,
                    weightStr = binding.weightEditText.text.toString(),
                    heightStr = binding.heightEditText.text.toString()
                )
            } else {
                viewModel.calculate(
                    isMetric = false,
                    ageStr = age,
                    gender = selectedGender,
                    weightStr = binding.weightImperialEditText.text.toString(),
                    heightStr = binding.feetEditText.text.toString(),
                    inchesStr = binding.inchesEditText.text.toString()
                )
            }
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            viewModel.saveCurrentResult()
            Toast.makeText(this, "Result saved!", Toast.LENGTH_SHORT).show()
            it.isEnabled = false // Disable button after saving
        }
    }

    private fun setupHistoryButton() {
        binding.historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun displayResults(result: BmiResult) {
        binding.resultCard.visibility = View.VISIBLE
        binding.saveButton.isEnabled = true // Re-enable on new result
        ObjectAnimator.ofFloat(binding.resultCard, "alpha", 0f, 1f).apply {
            duration = 500
            start()
        }

        val color = ContextCompat.getColor(this, result.colorRes)

        // ✅ Get strings from resources
        binding.resultTextView.text = getString(R.string.bmi_result_format, result.bmiValue)
        binding.categoryTextView.text = getString(result.categoryResId)
        binding.categoryTextView.setTextColor(color)
        binding.tipTextView.text = getString(result.tipResId)
        binding.classificationTextView.text = getString(result.descriptionResId)

        // ✅ Format the healthy range string here in the UI layer
        if (binding.metricRadioButton.isChecked) {
            binding.rangeTextView.text = getString(R.string.healthy_range_kg, result.minHealthyKg, result.maxHealthyKg)
        } else {
            binding.rangeTextView.text = getString(R.string.healthy_range_lbs,
                viewModel.kilogramsToPounds(result.minHealthyKg),
                viewModel.kilogramsToPounds(result.maxHealthyKg)
            )
        }
    }


    private fun clearUi() {
        binding.weightEditText.text = null
        binding.heightEditText.text = null
        binding.feetEditText.text = null
        binding.inchesEditText.text = null
        binding.weightImperialEditText.text = null
        binding.ageEditText.text = null
        binding.genderRadioGroup.clearCheck()
        viewModel.clearResult()
    }

    // Functions that only interact with the UI remain in the Activity
    private fun setupReferenceButton() {
        binding.referenceButton.setOnClickListener {
            showClassificationReference()
        }
    }

    private fun showClassificationReference() {
        MaterialAlertDialogBuilder(this)
            // ✅ Get strings from resources
            .setTitle(getString(R.string.dialog_title))
            .setBackground(ContextCompat.getDrawable(this,R.drawable.dialog_background))
            .setMessage(getString(R.string.dialog_message))
            .setPositiveButton(getString(R.string.dialog_ok_button), null)
            .show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}