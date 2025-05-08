package com.library.bmi.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

fun hideKeyboard(activity: Activity) {
    val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = activity.currentFocus ?: View(activity)
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

@SuppressLint("ClickableViewAccessibility")
fun Fragment.setupHideKeyboardOnTouch(root: View, vararg viewsToClearFocus: View) {
    root.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            hideKeyboard(requireActivity())
            viewsToClearFocus.forEach { it.clearFocus() }
        }
        false
    }
}