package cy.agorise.bitsybitshareswallet.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.google.android.material.textfield.TextInputEditText
import cy.agorise.bitsybitshareswallet.utils.hideKeyboard


/**
 * A TextInputEditText that hides the keyboard when the focus is removed from it and also lets you
 * use actions ("Done", "Go", etc.) on multi-line edits.
 */
class MyTextInputEditText(context: Context?, attrs: AttributeSet?) : TextInputEditText(context, attrs){

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val connection = super.onCreateInputConnection(outAttrs)
        val imeActions = outAttrs.imeOptions and EditorInfo.IME_MASK_ACTION
        if (imeActions and EditorInfo.IME_ACTION_DONE != 0) {
            // clear the existing action
            outAttrs.imeOptions = outAttrs.imeOptions xor imeActions
            // set the DONE action
            outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_ACTION_DONE
        }
        if (outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) {
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION.inv()
        }
        return connection
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) this.hideKeyboard()
    }
}