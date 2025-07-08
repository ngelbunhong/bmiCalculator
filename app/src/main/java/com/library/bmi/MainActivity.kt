package com.library.bmi

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
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
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        setupUnitToggle()
        setupCalculateButton()
        setupSaveButton()
        setupHistoryButton()
        setupGaugeChart()
        binding.referenceButton.setOnClickListener {
            showLegendDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.bmiResult.observe(this) { result ->
            if (result != null) displayResults(result)
            else binding.resultCard.visibility = View.GONE
        }

        viewModel.errorEvent.observe(this) {
            showError(getString(it))
        }
    }

    private fun setupUnitToggle() = binding.unitRadioGroup.setOnCheckedChangeListener { _, _ ->
        binding.metricInputCard.visibility =
            if (binding.metricRadioButton.isChecked) View.VISIBLE else View.GONE
        binding.imperialInputCard.visibility =
            if (binding.imperialRadioButton.isChecked) View.VISIBLE else View.GONE
        clearUi()
    }

    private fun setupCalculateButton() = binding.calculateButton.setOnClickListener {
        val age = binding.ageEditText.text.toString()
        val gender = when (binding.genderRadioGroup.checkedRadioButtonId) {
            R.id.maleRadioButton -> Gender.MALE
            R.id.femaleRadioButton -> Gender.FEMALE
            else -> null
        }

        if (binding.metricRadioButton.isChecked) {
            viewModel.calculate(
                isMetric = true,
                ageStr = age,
                gender = gender,
                weightStr = binding.weightEditText.text.toString(),
                heightStr = binding.heightEditText.text.toString()
            )
        } else {
            viewModel.calculate(
                isMetric = false,
                ageStr = age,
                gender = gender,
                weightStr = binding.weightImperialEditText.text.toString(),
                heightStr = binding.feetEditText.text.toString(),
                inchesStr = binding.inchesEditText.text.toString()
            )
        }
    }

    private fun setupSaveButton() = binding.saveButton.setOnClickListener {
        viewModel.saveCurrentResult()
        Toast.makeText(this, R.string.result_saved, Toast.LENGTH_SHORT).show()
        it.isEnabled = false
    }

    private fun setupHistoryButton() = binding.historyButton.setOnClickListener {
        startActivity(Intent(this, HistoryActivity::class.java))
    }

    private fun displayResults(result: BmiResult) {
        binding.resultCard.apply {
            visibility = View.VISIBLE
            ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
                duration = 500
                start()
            }
        }

        binding.saveButton.isEnabled = true

        val color = ContextCompat.getColor(this, result.colorRes)
        binding.resultTextView.text = getString(R.string.bmi_result_format, result.bmiValue)
        binding.categoryTextView.apply {
            text = getString(result.categoryResId)
            setTextColor(color)
        }
        binding.tipTextView.text = getString(result.tipResId)
        binding.classificationTextView.apply {
            text = getString(result.descriptionResId)
            setTextColor(color)
        }

        binding.rangeTextView.text = if (binding.metricRadioButton.isChecked) {
            getString(R.string.healthy_range_kg, result.minHealthyKg, result.maxHealthyKg)
        } else {
            getString(
                R.string.healthy_range_lbs,
                viewModel.kilogramsToPounds(result.minHealthyKg),
                viewModel.kilogramsToPounds(result.maxHealthyKg)
            )
        }

        updateGauge(result.bmiValue, color)
    }

    private fun setupGaugeChart() = binding.gaugeChart.apply {
        setUsePercentValues(false)
        description.isEnabled = false
        legend.isEnabled = false
        isDrawHoleEnabled = true
        setHoleColor(android.graphics.Color.TRANSPARENT)
        setTransparentCircleAlpha(0)
        holeRadius = 65f
        transparentCircleRadius = 70f
        setDrawCenterText(true)
        centerText = "BMI"
        setCenterTextSize(24f)
        maxAngle = 180f
        rotationAngle = 180f
        setTouchEnabled(false)

        val entries = listOf(
            PieEntry(1f),//category_severe_thinness
            PieEntry(1f),//category_moderate_thinness
            PieEntry(8.5f), //category_mild_thinness
            PieEntry(6.5f), // category_normal
            PieEntry(5f),   // category_overweight
            PieEntry(5f),   // category_obese1
            PieEntry(5f),   // category_obese2
            PieEntry(5f),   // category_obese3

        )

        val dataSet = PieDataSet(entries, "BMI Categories").apply {
            setDrawValues(false)
            colors = listOf(
                ContextCompat.getColor(this@MainActivity, R.color.severeThinness),
                ContextCompat.getColor(this@MainActivity, R.color.moderateThinness),
                ContextCompat.getColor(this@MainActivity, R.color.mildThinness),
                ContextCompat.getColor(this@MainActivity, R.color.normal),
                ContextCompat.getColor(this@MainActivity, R.color.overweight),
                ContextCompat.getColor(this@MainActivity, R.color.obese1),
                ContextCompat.getColor(this@MainActivity, R.color.obese2),
                ContextCompat.getColor(this@MainActivity, R.color.obese3)
            )
            sliceSpace = 2f
        }

        data = PieData(dataSet)
        invalidate()
    }

    private fun updateGauge(bmiValue: Float, color: Int) = binding.gaugeChart.apply {
        centerText = "%.1f".format(bmiValue)
        setCenterTextColor(color)
        animateY(1000)
    }

    private fun clearUi() {
        with(binding) {
            weightEditText.text = null
            heightEditText.text = null
            feetEditText.text = null
            inchesEditText.text = null
            weightImperialEditText.text = null
            ageEditText.text = null
            genderRadioGroup.clearCheck()
        }
        viewModel.clearResult()
    }


    private fun showLegendDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_bmi_legend, null)
        val legendLayout = dialogView.findViewById<ViewGroup>(R.id.legendContainer)

        val categories = listOf(
            R.string.category_severe_thinness,
            R.string.category_moderate_thinness,
            R.string.category_mild_thinness,
            R.string.category_normal,
            R.string.category_overweight,
            R.string.category_obese1,
            R.string.category_obese2,
            R.string.category_obese3
        )

        val colors = listOf(
            R.color.severeThinness,
            R.color.moderateThinness,
            R.color.mildThinness,
            R.color.normal,
            R.color.overweight,
            R.color.obese1,
            R.color.obese2,
            R.color.obese3
        )

        categories.forEachIndexed { index, categoryRes ->
            val color = ContextCompat.getColor(this, colors[index])
            val label = getString(categoryRes)

            val legendItem = LayoutInflater.from(this).inflate(R.layout.legend_item, legendLayout, false)
            val colorCircle = legendItem.findViewById<View>(R.id.colorCircle)
            val labelText = legendItem.findViewById<TextView>(R.id.labelText)

            colorCircle.background.setTint(color)
            labelText.text = label

            legendLayout.addView(legendItem)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title) // e.g., "Classification Reference"
            .setView(dialogView)
            .setPositiveButton(R.string.dialog_ok_button, null) // e.g., "OK"
            .show()
    }


    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
