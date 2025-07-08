package com.library.bmi.ui.history

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.library.bmi.data.adapter.HistoryAdapter
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.library.bmi.BmiApplication
import com.library.bmi.R
import com.library.bmi.data.repository.HistoryViewModelFactory
import com.library.bmi.databinding.ActivityHistoryBinding
import androidx.core.graphics.drawable.toDrawable

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    // ✅ Use the factory to create the ViewModel
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
        observeViewModel()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.historyRecyclerView.adapter = historyAdapter


        // Get the delete icon and color for drawing
        val deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete)!!
        val backgroundColor = Color.RED.toDrawable()

        // ✅ START: Add ItemTouchHelper for swipe-to-delete
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // We don't need drag-and-drop
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val record = historyAdapter.getRecordAt(position)
                viewModel.delete(record)

                // Show a snackbar with an undo option
                Snackbar.make(binding.root, "Record deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        viewModel.insert(record) // Re-insert the record
                    }.show()
            }

            /**
             * ✅ This is the new method to draw the red background and icon.
             */
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // Let the ItemTouchHelper handle the default drawing
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                val itemView = viewHolder.itemView
                val backgroundCornerOffset = 20 // To add rounded corners to the background

                val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
                val iconBottom = iconTop + deleteIcon.intrinsicHeight

                when {
                    // Swiping to the right
                    dX > 0 -> {
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = itemView.left + iconMargin + deleteIcon.intrinsicWidth
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                        backgroundColor.setBounds(
                            itemView.left, itemView.top,
                            itemView.left + dX.toInt() + backgroundCornerOffset, itemView.bottom
                        )
                    }
                    // Swiping to the left
                    dX < 0 -> {
                        val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                        backgroundColor.setBounds(
                            itemView.right + dX.toInt() - backgroundCornerOffset,
                            itemView.top, itemView.right, itemView.bottom
                        )
                    }
                    // View is not being swiped
                    else -> {
                        backgroundColor.setBounds(0, 0, 0, 0)
                    }
                }

                backgroundColor.draw(c)
                deleteIcon.draw(c)
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.historyRecyclerView)
        // ✅ END: Add ItemTouchHelper
    }

    private fun observeViewModel() {
        viewModel.allRecords.observe(this) { records ->
            binding.emptyHistoryText.visibility = if (records.isNullOrEmpty()) View.VISIBLE else View.GONE
            historyAdapter.submitList(records)
        }
    }

    // ✅ Inflate the menu for the toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        return true
    }


    /**
     * ✅ Handles clicks on menu items, including the Up/Back button.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            // ✅ Handle the delete all action
            R.id.action_delete_all -> {
                showDeleteAllConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    // ✅ Show a confirmation dialog before clearing all data
    private fun showDeleteAllConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete All History?")
            .setMessage("Are you sure you want to delete all entries? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteAll()
            }
            .show()
    }
}