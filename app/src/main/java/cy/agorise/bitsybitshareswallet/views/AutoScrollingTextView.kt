package cy.agorise.bitsybitshareswallet.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * TextView that in case the content does not fit the available width, scrolls horizontally automatically. The
 * TextView needs this xml attributes to work correctly:
 *      android:singleLine="true"
 *      android:ellipsize="marquee"
 *      android:marqueeRepeatLimit="marquee_forever"
 *      android:scrollHorizontally="true"
 */
class AutoScrollingTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {
    override fun onFocusChanged(
        focused: Boolean, direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        if (focused) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect)
        }
    }

    override fun onWindowFocusChanged(focused: Boolean) {
        if (focused) {
            super.onWindowFocusChanged(focused)
        }
    }

    override fun isFocused(): Boolean {
        return true
    }
}