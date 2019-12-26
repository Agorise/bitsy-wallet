package cy.agorise.bitsybitshareswallet.utils

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Creates an enabled state, by enabling the button and using the given [colorResource] to color it.
 */
fun FloatingActionButton.enable(colorResource: Int) {
    this.isEnabled = true
    this.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this.context, colorResource))
}

/**
 * Creates a disabled state, by disabling the button and using the given [colorResource] to color it.
 */
fun FloatingActionButton.disable(colorResource: Int) {
    this.isEnabled = false
    this.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this.context, colorResource))
}

/**
 * Easily create a toast message with less boilerplate code
 */
fun Context.toast(message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Verifies that the current string contains at least one digit
 */
fun String.containsDigits(): Boolean {
    return this.matches(".*\\d.*".toRegex())
}

/**
 * Verifies that the current string contains at least one vowel
 */
fun String.containsVowels(): Boolean {
    return matches(".*[aeiou].*".toRegex())
}

/**
 * Allows to hide the Keyboard from any view
 */
fun View.hideKeyboard(){
    val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
}

/**
 * Allows to request focus and show the Keyboard from an EditText (and its sub-classes).
 */
fun EditText.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}