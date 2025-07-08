package com.library.bmi.ui.history

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.library.bmi.BmiApplication
import com.library.bmi.R
import com.library.bmi.data.adapter.HistoryAdapter
import com.library.bmi.data.database.BmiRecord
import com.library.bmi.data.factory.HistoryViewModelFactory
import com.library.bmi.databinding.ActivityHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory((application as BmiApplication).repository)
    }
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupHistoryChart()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.allRecords.observe(this) { records ->
            val isEmpty = records.isNullOrEmpty()
            binding.emptyHistoryText.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.historyRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.historyChart.visibility = if (isEmpty) View.GONE else View.VISIBLE

            if (isEmpty) {
                binding.historyChart.clear()
            } else {
                historyAdapter.submitList(records)
                updateHistoryChart(records)
            }
        }
    }

    private fun setupHistoryChart() {
        binding.historyChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setExtraOffsets(10f, 10f, 10f, 20f)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false
        }
    }

    private fun updateHistoryChart(records: List<BmiRecord>) {
        if (records.isEmpty()) {
            binding.historyChart.clear()
            return
        }

        // Set the dynamic Y-axis range to ensure all points are visible
        val minBmi = records.minOfOrNull { it.bmi } ?: 15f
        val maxBmi = records.maxOfOrNull { it.bmi } ?: 30f
        binding.historyChart.axisLeft.axisMinimum = floor(minBmi - 2).coerceAtLeast(0f)
        binding.historyChart.axisLeft.axisMaximum = maxBmi + 2

        // Reverse records so newest is last (for chart order)
        val reversedRecords = records.reversed()

        // Use index as X value to guarantee uniqueness
        val entries = reversedRecords.mapIndexed { index, record ->
            Entry(index.toFloat(), record.bmi)
        }

        // Color each point based on BMI
        val circleColors = reversedRecords.map { record ->
            getBmiColor(record.bmi)
        }

        // Set up dataset
        val dataSet = LineDataSet(entries, "BMI History").apply {
            color = Color.GRAY
            valueTextColor = ContextCompat.getColor(this@HistoryActivity, R.color.black)
            setCircleColors(circleColors)
            lineWidth = 2f
            circleRadius = 5f
            circleHoleRadius = 2.5f
            setDrawCircleHole(true)
            valueTextSize = 10f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        // Apply to chart
        binding.historyChart.data = LineData(dataSet)

        // Update X-axis label formatter based on date
        binding.historyChart.xAxis.valueFormatter = object : ValueFormatter() {
            private val format = SimpleDateFormat("MMM d", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt().coerceIn(0, reversedRecords.lastIndex)
                return format.format(reversedRecords[index].timestamp)
            }
        }

        // Refresh the chart
        binding.historyChart.invalidate()
        binding.historyChart.animateX(500)
    }


    private fun getBmiColor(bmi: Float): Int {
        val colorRes = when {
            bmi < 16f -> R.color.severeThinness
            bmi < 17f -> R.color.moderateThinness
            bmi < 18.5f -> R.color.mildThinness
            bmi < 25f -> R.color.normal
            bmi < 30f -> R.color.overweight
            bmi < 35f -> R.color.obese1
            bmi < 40f -> R.color.obese2
            else -> R.color.obese3
        }
        return ContextCompat.getColor(this, colorRes)
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.historyRecyclerView.adapter = historyAdapter

        val deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete)!!
        val backgroundColor = ColorDrawable(Color.RED)

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val record = historyAdapter.getRecordAt(position)
                viewModel.delete(record)

                Snackbar.make(binding.root, getString(R.string.record_deleted), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.undo)) {
                        viewModel.insert(record)
                    }.show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                val itemView = viewHolder.itemView
                val backgroundCornerOffset = 20

                val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
                val iconBottom = iconTop + deleteIcon.intrinsicHeight

                when {
                    dX > 0 -> {
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = itemView.left + iconMargin + deleteIcon.intrinsicWidth
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        backgroundColor.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt() + backgroundCornerOffset, itemView.bottom)
                    }
                    dX < 0 -> {
                        val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        backgroundColor.setBounds(itemView.right + dX.toInt() - backgroundCornerOffset, itemView.top, itemView.right, itemView.bottom)
                    }
                    else -> {
                        backgroundColor.setBounds(0, 0, 0, 0)
                    }
                }
                backgroundColor.draw(c)
                deleteIcon.draw(c)
            }

            /**
             * ✅ START: This new override fixes the "stuck" red background bug.
             * It's called when a swipe is finished or cancelled (like with an undo).
             */
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // This forces the item view to redraw itself, clearing the red background.
                viewHolder.itemView.invalidate()
            }
            // ✅ END: Fix for stuck background
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.historyRecyclerView)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_delete_all -> {
                showDeleteAllConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteAllConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_delete_all_title))
            .setMessage(getString(R.string.dialog_delete_all_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteAll()
            }
            .show()
    }
}
