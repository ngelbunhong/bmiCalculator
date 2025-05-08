package com.library.bmi

import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            hideKeyboard()
            clearFocus()
        }
        return super.dispatchTouchEvent(ev)
    }

    fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow((currentFocus ?: View(this)).windowToken, 0)
    }

    fun clearFocus() {
        currentFocus?.clearFocus()
    }
}