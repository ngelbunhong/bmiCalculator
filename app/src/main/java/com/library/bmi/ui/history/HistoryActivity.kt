package com.library.bmi.ui.history

import android.content.ContentValues
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
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

            // ✅ Ensure dot value labels show only 1 decimal place
            valueFormatter = object : ValueFormatter() {
                override fun getPointLabel(entry: Entry?): String {
                    return entry?.let { String.format("%.1f", it.y) } ?: ""
                }
            }
        }

        binding.historyChart.data = LineData(dataSet)

        // X-axis shows dates
        binding.historyChart.xAxis.valueFormatter = object : ValueFormatter() {
            private val format = SimpleDateFormat("MMM d", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt().coerceIn(0, reversedRecords.lastIndex)
                return format.format(reversedRecords[index].timestamp)
            }
        }

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
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val record = historyAdapter.getRecordAt(position)
                viewModel.delete(record)

                Snackbar.make(
                    binding.root,
                    getString(R.string.record_deleted),
                    Snackbar.LENGTH_LONG
                )
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
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )

                val itemView = viewHolder.itemView
                val backgroundCornerOffset = 20

                val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + deleteIcon.intrinsicHeight

                when {
                    dX > 0 -> {
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + deleteIcon.intrinsicWidth
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        backgroundColor.setBounds(
                            itemView.left,
                            itemView.top,
                            itemView.left + dX.toInt() + backgroundCornerOffset,
                            itemView.bottom
                        )
                    }

                    dX < 0 -> {
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - deleteIcon.intrinsicWidth
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        backgroundColor.setBounds(
                            itemView.right + dX.toInt() - backgroundCornerOffset,
                            itemView.top,
                            itemView.right,
                            itemView.bottom
                        )
                    }

                    else -> {
                        backgroundColor.setBounds(0, 0, 0, 0)
                    }
                }

                backgroundColor.draw(c)
                deleteIcon.draw(c)
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.invalidate()
            }
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

            R.id.action_export_csv -> {
                showExportCSVConfirmationDialog()
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showExportCSVConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_export_title_message))
            .setMessage(getString(R.string.dialog_export_all_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.dialog_export_title)) { _, _ ->
                exportCsvFile()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportCsvFile() {
        viewModel.allRecords.value?.let { records ->
            if (records.isEmpty()) {
                Snackbar.make(binding.root, "No history to export", Snackbar.LENGTH_SHORT).show()
                return
            }

            // Step 1: Prepare CSV content
            val csvHeader = "Date,BMI,Category,Age,Gender,Weight,Height"
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val csvBody = records.joinToString("\n") {
                val date = sdf.format(it.timestamp) // FIX: wrap timestamp
                "$date,${it.bmi},${it.category},${it.age},${it.gender},${it.weight},${it.height}"
            }
            val csvContent = "$csvHeader\n$csvBody"

            // Step 2: Use MediaStore to save to Downloads
            val filename = "bmi_history_${
                SimpleDateFormat(
                    "yyyy_MM_dd",
                    Locale.getDefault()
                ).format(Date())
            }.csv"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray())
                    }

                    Snackbar.make(binding.root, "CSV saved to Downloads", Snackbar.LENGTH_LONG)
                        .setAction("Open") {
                            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "text/csv")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                startActivity(Intent.createChooser(openIntent, "Open CSV with"))
                            } catch (e: Exception) {
                                Toast.makeText(this, "No app found to open CSV", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        .show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Snackbar.make(binding.root, "Failed to save CSV", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                Snackbar.make(binding.root, "Unable to create file", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportCsvFile(records: List<BmiRecord>) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val fileName =
            "bmi_history_${SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())}.csv"
        val csvHeader = "Date,BMI,Category,Age,Gender,Weight,Height"
        val csvBody = records.joinToString("\n") {
            val date = sdf.format(it.timestamp)
            "${date},${it.bmi},${it.category},${it.age},${it.gender},${it.weight},${it.height}"
        }
        val content = "$csvHeader\n$csvBody"

        try {
            val file = File(cacheDir, fileName)
            file.writeText(content)

            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_csv)))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to export CSV", Toast.LENGTH_SHORT).show()
        }
    }

}
