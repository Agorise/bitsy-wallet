package cy.agorise.bitsybitshareswallet.fragments


import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.Observer
import com.crashlytics.android.Crashlytics
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.BalancesDetailsAdapter
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail
import cy.agorise.bitsybitshareswallet.models.FilterOptions
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceDetailViewModel
import cy.agorise.bitsybitshareswallet.views.DatePickerFragment
import java.text.SimpleDateFormat
import java.util.*
import kotlin.ClassCastException
import kotlin.collections.ArrayList


/**
 * Creates a Dialog that communicates with {@link TransactionsActivity} to give it parameters about
 * how to filter the list of Transactions
 */
class FilterOptionsDialog : DialogFragment(), DatePickerFragment.OnDateSetListener {

    companion object {
        private const val TAG = "FilterOptionsDialog"

        const val KEY_FILTER_OPTIONS = "key_filter_options"

        const val START_DATE_PICKER = 0
        const val END_DATE_PICKER = 1
    }

    private lateinit var mFilterOptions: FilterOptions

    // Widgets TODO use android-kotlin-extensions {onViewCreated}
    private lateinit var rbTransactionAll: RadioButton
    private lateinit var rbTransactionSent: RadioButton
    private lateinit var rbTransactionReceived: RadioButton
    private lateinit var cbDateRange: CheckBox
    private lateinit var llDateRange: LinearLayout
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var cbAsset: CheckBox
    private lateinit var sAsset: Spinner
    private lateinit var cbEquivalentValue: CheckBox
    private lateinit var llEquivalentValue: LinearLayout
    private lateinit var etFromEquivalentValue: EditText
    private lateinit var etToEquivalentValue: EditText
    private lateinit var tvEquivalentValueSymbol: TextView
    private lateinit var switchAgoriseFees: Switch

    private var mCallback: OnFilterOptionsSelectedListener? = null

    private var dateFormat: SimpleDateFormat = SimpleDateFormat("d/MMM/yyyy",
        ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0])

    private var mBalanceDetails = ArrayList<BalanceDetail>()

    private lateinit var mBalanceDetailViewModel: BalanceDetailViewModel

    private var mBalancesDetailsAdapter: BalancesDetailsAdapter? = null

    private lateinit var mCurrency: Currency

    override fun onDateSet(which: Int, timestamp: Long) {
        when(which) {
            START_DATE_PICKER -> {
                mFilterOptions.startDate = timestamp

                updateDateTextViews()
            }
            END_DATE_PICKER -> {
                mFilterOptions.endDate = timestamp

                // Make sure there is at least one moth difference between start and end time
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = mFilterOptions.endDate
                calendar.add(Calendar.MONTH, -1)

                val tmpTime = calendar.timeInMillis

                if (tmpTime < mFilterOptions.startDate)
                    mFilterOptions.startDate = tmpTime

                updateDateTextViews()
            }
        }
    }

    private fun updateDateTextViews() {
        var date = Date(mFilterOptions.startDate)
        tvStartDate.text = dateFormat.format(date)

        date = Date(mFilterOptions.endDate)
        tvEndDate.text = dateFormat.format(date)
    }

    // Container Fragment must implement this interface
    interface OnFilterOptionsSelectedListener {
        fun onFilterOptionsSelected(filterOptions: FilterOptions)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        onAttachToParentFragment(parentFragment!!)

        Crashlytics.setString(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        mFilterOptions = arguments?.getParcelable(KEY_FILTER_OPTIONS)!!

        val builder = AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.title_filter_options))
            .setPositiveButton(getString(R.string.button__filter)) { _, _ ->  validateFields() }
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ ->  dismiss() }

        // Inflate layout
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.dialog_filter_options, null)

        // Initialize Transactions direction
        rbTransactionAll = view.findViewById(R.id.rbTransactionAll)
        rbTransactionSent = view.findViewById(R.id.rbTransactionSent)
        rbTransactionReceived = view.findViewById(R.id.rbTransactionReceived)
        when (mFilterOptions.transactionsDirection) {
            0 -> rbTransactionAll.isChecked = true
            1 -> rbTransactionSent.isChecked = true
            2 -> rbTransactionReceived.isChecked = true
        }

        // Initialize Date range
        cbDateRange = view.findViewById(R.id.cbDateRange)
        llDateRange = view.findViewById(R.id.llDateRange)
        cbDateRange.setOnCheckedChangeListener { _, isChecked ->
            llDateRange.visibility = if(isChecked) View.GONE else View.VISIBLE }
        cbDateRange.isChecked = mFilterOptions.dateRangeAll

        tvStartDate = view.findViewById(R.id.tvStartDate)
        tvEndDate = view.findViewById(R.id.tvEndDate)

        tvStartDate.setOnClickListener(mDateClickListener)

        tvEndDate.setOnClickListener(mDateClickListener)

        updateDateTextViews()

        // Initialize Asset
        cbAsset = view.findViewById(R.id.cbAsset)
        sAsset = view.findViewById(R.id.sAsset)
        cbAsset.setOnCheckedChangeListener { _, isChecked ->
            sAsset.visibility = if(isChecked) View.GONE else View.VISIBLE
        }
        cbAsset.isChecked = mFilterOptions.assetAll

        // Configure BalanceDetailViewModel to obtain the user's Balances
        mBalanceDetailViewModel = ViewModelProviders.of(this).get(BalanceDetailViewModel::class.java)

        mBalanceDetailViewModel.getAll().observe(this, Observer<List<BalanceDetail>> { balancesDetails ->
            mBalanceDetails.clear()
            mBalanceDetails.addAll(balancesDetails)
            mBalanceDetails.sortWith(
                Comparator { a, b -> a.toString().compareTo(b.toString(), true) }
            )
            mBalancesDetailsAdapter = BalancesDetailsAdapter(context!!, android.R.layout.simple_spinner_item, mBalanceDetails)
            sAsset.adapter = mBalancesDetailsAdapter

            // Try to select the selectedAssetSymbol
            for (i in 0 until mBalancesDetailsAdapter!!.count) {
                if (mBalancesDetailsAdapter!!.getItem(i)!!.symbol == mFilterOptions.asset) {
                    sAsset.setSelection(i)
                    break
                }
            }
        })

        // Initialize Equivalent Value
        cbEquivalentValue = view.findViewById(R.id.cbEquivalentValue)
        llEquivalentValue = view.findViewById(R.id.llEquivalentValue)
        cbEquivalentValue.setOnCheckedChangeListener { _, isChecked ->
            llEquivalentValue.visibility = if(isChecked) View.GONE else View.VISIBLE }
        cbEquivalentValue.isChecked = mFilterOptions.equivalentValueAll

        // TODO obtain user selected currency
        val currencySymbol = "usd"
        mCurrency = Currency.getInstance(currencySymbol)

        etFromEquivalentValue = view.findViewById(R.id.etFromEquivalentValue)
        val fromEquivalentValue = mFilterOptions.fromEquivalentValue /
                Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()
        etFromEquivalentValue.setText("$fromEquivalentValue", TextView.BufferType.EDITABLE)

        etToEquivalentValue = view.findViewById(R.id.etToEquivalentValue)
        val toEquivalentValue = mFilterOptions.toEquivalentValue /
                Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()
        etToEquivalentValue.setText("$toEquivalentValue", TextView.BufferType.EDITABLE)

        tvEquivalentValueSymbol = view.findViewById(R.id.tvEquivalentValueSymbol)
        tvEquivalentValueSymbol.text = currencySymbol.toUpperCase()

        // Initialize transaction network fees
        switchAgoriseFees = view.findViewById(R.id.switchAgoriseFees)
        switchAgoriseFees.isChecked = mFilterOptions.agoriseFees

        builder.setView(view)

        return builder.create()
    }

    /**
     * Attaches the current [DialogFragment] to its [Fragment] parent, to initialize the
     * [OnFilterOptionsSelectedListener] interface
     */
    private fun onAttachToParentFragment(fragment: Fragment) {
        try {
            mCallback = fragment as OnFilterOptionsSelectedListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$fragment must implement OnFilterOptionsSelectedListener")
        }
    }

    private val mDateClickListener = View.OnClickListener { v ->
        val calendar = Calendar.getInstance()

        // Variable used to select that date on the calendar
        var currentTime = calendar.timeInMillis
        var maxTime = currentTime

        var which = -1
        if (v.id == R.id.tvStartDate) {
            which = START_DATE_PICKER
            currentTime = mFilterOptions.startDate
            calendar.timeInMillis = mFilterOptions.endDate
            calendar.add(Calendar.MONTH, -1)
            maxTime = calendar.timeInMillis
        } else if (v.id == R.id.tvEndDate) {
            which = END_DATE_PICKER
            currentTime = mFilterOptions.endDate
        }

        val datePickerFragment = DatePickerFragment.newInstance(which, currentTime, maxTime)
        datePickerFragment.show(childFragmentManager, "date-picker")
    }

    private fun validateFields() {
        mFilterOptions.transactionsDirection =  when {
            rbTransactionAll.isChecked -> 0
            rbTransactionSent.isChecked -> 1
            rbTransactionReceived.isChecked -> 2
            else -> { 0 }
        }

        mFilterOptions.dateRangeAll = cbDateRange.isChecked

        mFilterOptions.assetAll = cbAsset.isChecked

        mFilterOptions.asset = (sAsset.selectedItem as BalanceDetail).symbol

        mFilterOptions.equivalentValueAll = cbEquivalentValue.isChecked

        mFilterOptions.fromEquivalentValue = etFromEquivalentValue.text.toString().toLong() *
                Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()

        mFilterOptions.toEquivalentValue = etToEquivalentValue.text.toString().toLong() *
                Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()

        // Make sure ToEquivalentValue is at least 50 units bigger than FromEquivalentValue
        mFilterOptions.toEquivalentValue =
            Math.max(mFilterOptions.toEquivalentValue, mFilterOptions.fromEquivalentValue + 50)

        mFilterOptions.agoriseFees = switchAgoriseFees.isChecked

        mCallback!!.onFilterOptionsSelected(mFilterOptions)
    }
}