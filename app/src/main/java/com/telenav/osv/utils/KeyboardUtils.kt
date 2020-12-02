package com.telenav.osv.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * This object provides utility methods for keyboard.
 * Eg - Hiding keyboard, etc
 */
object KeyboardUtils {

    /**
     * This method hides keyboard for a given activity
     */
    fun hideKeyboard(activity: Activity?) {
        val view: View? = activity?.currentFocus ?: return
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}