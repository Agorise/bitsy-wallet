package cy.agorise.bitsybitshareswallet.fragments


import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.crashlytics.FirebaseCrashlytics
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.BalancesDetailsAdapter
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail
import cy.agorise.bitsybitshareswallet.databinding.DialogFilterOptionsBinding
import cy.agorise.bitsybitshareswallet.models.FilterOptions
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.Helper
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceDetailViewModel
import cy.agorise.bitsybitshareswallet.views.DatePickerFragment
import java.text.SimpleDateFormat
import java.util.*
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

    private var _binding: DialogFilterOptionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var mFilterOptions: FilterOptions

    private var mCallback: OnFilterOptionsSelectedListener? = null

    private var dateFormat: SimpleDateFormat = SimpleDateFormat(
        "d/MMM/yyyy",
        ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
    )

    private var mBalanceDetails = ArrayList<BalanceDetail>()

    private lateinit var mBalanceDetailViewModel: BalanceDetailViewModel

    private var mBalancesDetailsAdapter: BalancesDetailsAdapter? = null

    private lateinit var mCurrency: Currency

    override fun onDateSet(which: Int, timestamp: Long) {
        when (which) {
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
        binding.tvStartDate.text = dateFormat.format(date)

        date = Date(mFilterOptions.endDate)
        binding.tvEndDate.text = dateFormat.format(date)
    }

    // Container Fragment must implement this interface
    interface OnFilterOptionsSelectedListener {
        fun onFilterOptionsSelected(filterOptions: FilterOptions)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFilterOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onAttachToParentFragment(parentFragment!!)

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        mFilterOptions = arguments?.getParcelable(KEY_FILTER_OPTIONS)!!

        // Initialize Transactions direction
        when (mFilterOptions.transactionsDirection) {
            0 -> binding.rbTransactionAll.isChecked = true
            1 -> binding.rbTransactionSent.isChecked = true
            2 -> binding.rbTransactionReceived.isChecked = true
        }

        // Initialize Date range
        binding.cbDateRange.setOnCheckedChangeListener { _, isChecked ->
            binding.llDateRange.visibility = if (isChecked) View.GONE else View.VISIBLE
        }
        binding.cbDateRange.isChecked = mFilterOptions.dateRangeAll

        binding.tvStartDate.setOnClickListener(mDateClickListener)

        binding.tvEndDate.setOnClickListener(mDateClickListener)

        updateDateTextViews()

        // Initialize Asset
        binding.cbAsset.setOnCheckedChangeListener { _, isChecked ->
            binding.sAsset.visibility = if (isChecked) View.GONE else View.VISIBLE
        }
        binding.cbAsset.isChecked = mFilterOptions.assetAll

        // Configure BalanceDetailViewModel to obtain the user's Balances
        mBalanceDetailViewModel =
            ViewModelProviders.of(this).get(BalanceDetailViewModel::class.java)

        mBalanceDetailViewModel.getAll()
            .observe(this, Observer<List<BalanceDetail>> { balancesDetails ->
                mBalanceDetails.clear()
                mBalanceDetails.addAll(balancesDetails)
                mBalanceDetails.sortWith(
                    Comparator { a, b -> a.toString().compareTo(b.toString(), true) }
                )
                mBalancesDetailsAdapter = BalancesDetailsAdapter(
                    context!!,
                    android.R.layout.simple_spinner_item,
                    mBalanceDetails
                )
                binding.sAsset.adapter = mBalancesDetailsAdapter

                // Try to select the selectedAssetSymbol
                for (i in 0 until mBalancesDetailsAdapter!!.count) {
                    if (mBalancesDetailsAdapter!!.getItem(i)!!.symbol == mFilterOptions.asset) {
                        binding.sAsset.setSelection(i)
                        break
                    }
                }
            })

        // Initialize Equivalent Value
        binding.cbEquivalentValue.setOnCheckedChangeListener { _, isChecked ->
            binding.llEquivalentValue.visibility = if (isChecked) View.GONE else View.VISIBLE
        }
        binding.cbEquivalentValue.isChecked = mFilterOptions.equivalentValueAll

        val currencyCode = Helper.getCoingeckoSupportedCurrency(Locale.getDefault())
        mCurrency = Currency.getInstance(currencyCode)

        val fromEquivalentValue = mFilterOptions.fromEquivalentValue /
                Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()
        binding.etFromEquivalentValue.setText("$fromEquivalentValue", TextView.BufferType.EDITABLE)

        val toEquivalentValue = mFilterOptions.toEquivalentValue /
                Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()
        binding.etToEquivalentValue.setText("$toEquivalentValue", TextView.BufferType.EDITABLE)

        binding.tvEquivalentValueSymbol.text = currencyCode.toUpperCase(Locale.getDefault())

        // Initialize transaction network fees
        binding.switchAgoriseFees.isChecked = mFilterOptions.agoriseFees

        // Setup cancel and filter buttons
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnFilter.setOnClickListener { validateFields() }
    }

    override fun onResume() {
        super.onResume()

        // Force dialog fragment to use the full width of the screen
        // TODO use the same width as standard fragments
        val dialogWindow = dialog?.window
        dialogWindow?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
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
        mFilterOptions.transactionsDirection = when {
            binding.rbTransactionAll.isChecked -> 0
            binding.rbTransactionSent.isChecked -> 1
            binding.rbTransactionReceived.isChecked -> 2
            else -> {
                0
            }
        }

        mFilterOptions.dateRangeAll = binding.cbDateRange.isChecked

        mFilterOptions.assetAll = binding.cbAsset.isChecked

        val symbol = (binding.sAsset.selectedItem as BalanceDetail?)?.symbol
        // If there are no assets in the spinner (the account has 0 balances or the app has not yet
        // fetched the account balances) symbol will be null, make sure that does not create a crash.
        if (symbol != null)
            mFilterOptions.asset = symbol
        else
            mFilterOptions.assetAll = true

        mFilterOptions.equivalentValueAll = binding.cbEquivalentValue.isChecked

        mFilterOptions.fromEquivalentValue =
            binding.etFromEquivalentValue.text.toString().toLong() *
                    Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()

        mFilterOptions.toEquivalentValue = binding.etToEquivalentValue.text.toString().toLong() *
                Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()

        // Make sure ToEquivalentValue is at least 50 units bigger than FromEquivalentValue
        mFilterOptions.toEquivalentValue =
            Math.max(mFilterOptions.toEquivalentValue, mFilterOptions.fromEquivalentValue + 50)

        mFilterOptions.agoriseFees = binding.switchAgoriseFees.isChecked

        mCallback!!.onFilterOptionsSelected(mFilterOptions)
        dismiss()
    }
}