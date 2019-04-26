package cy.agorise.bitsybitshareswallet.views

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.picker.MaterialDatePickerDialog
import java.util.*

/**
 * Lets the user select a Date and communicates the selection back to the parent fragment
 * using the OnDateSetListener interface, which has to be implemented by the parent.
 */
class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

    companion object {
        const val TAG = "DatePickerFragment"

        const val KEY_WHICH = "key_which"
        const val KEY_CURRENT = "key_current"
        const val KEY_MAX = "key_max"

        fun newInstance(which: Int, currentTime: Long, maxTime: Long): DatePickerFragment {
            val f = DatePickerFragment()
            val bundle = Bundle()
            bundle.putInt(KEY_WHICH, which)
            bundle.putLong(KEY_CURRENT, currentTime)
            bundle.putLong(KEY_MAX, maxTime)
            f.arguments = bundle
            return f
        }
    }

    /**
     * Callback used to communicate the date selection back to the parent
     */
    private var mCallback: OnDateSetListener? = null

    private var which: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        which = arguments!!.getInt(KEY_WHICH)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        onAttachToParentFragment(parentFragment)

        val currentTime = arguments!!.getLong(KEY_CURRENT)
        val maxTime = arguments!!.getLong(KEY_MAX)

        // Use the current date as the default date in the picker
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create a new instance of DatePickerDialog and return it
        val datePicker = MaterialDatePickerDialog(activity!!, this, year, month, day)

        // Set maximum date allowed to today
        datePicker.datePicker.maxDate = maxTime

        return datePicker
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        val calendar = GregorianCalendar()
        calendar.set(year, month, day)
        mCallback?.onDateSet(which, calendar.time.time)
    }

    /**
     * Attaches the current [DialogFragment] to its [Fragment] parent, to initialize the
     * [OnDateSetListener] interface
     */
    private fun onAttachToParentFragment(fragment: Fragment?) {
        try {
            mCallback = fragment as OnDateSetListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$fragment must implement OnDateSetListener")
        }
    }

    // Container Activity must implement this interface
    interface OnDateSetListener {
        fun onDateSet(which: Int, timestamp: Long)
    }
}