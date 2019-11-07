package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.models.FilterOptions
import cy.agorise.bitsybitshareswallet.repositories.TransferDetailRepository
import cy.agorise.bitsybitshareswallet.repositories.TransferRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val TAG = "TransactionsViewModel"
    }
    private var mRepository = TransferDetailRepository(application)
    private var mTransfersRepository = TransferRepository(application)

    /**
     * [FilterOptions] used to filter the list of [TransferDetail] taken from the database
     */
    private var mFilterOptions = FilterOptions()

    private lateinit var transactions : LiveData<List<TransferDetail>>

    /**
     * This [MediatorLiveData] is used to combine two sources of information into one, keeping the
     * client of this [ViewModel] receiving only one stream of data (a list of filtered [TransferDetail])
     */
    private val filteredTransactions = MediatorLiveData<List<TransferDetail>>()

    init {
        // Initialize the start and end dates for the FilterOptions
        val calendar = Calendar.getInstance()
        mFilterOptions.endDate = calendar.timeInMillis
        calendar.add(Calendar.MONTH, -2)
        mFilterOptions.startDate = calendar.timeInMillis
    }

    internal fun getFilteredTransactions(userId: String): LiveData<List<TransferDetail>> {
        val currency = Currency.getInstance(Locale.getDefault())
        mTransfersRepository.getSupportedCurrency(currency.currencyCode)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d(TAG,"Looking for currency with code: ${it}")
                transactions = mRepository.getAll(userId, it)

                filteredTransactions.addSource(transactions) { transactions ->
                    viewModelScope.launch {
                        filteredTransactions.value = filter(transactions, mFilterOptions)
                    }
                }
            },{
                Log.e(TAG,"Error while trying to obtain a filtered list of transactions. Msg: ${it.message}")
                for(element in it.stackTrace){
                    Log.e(ConnectedActivityViewModel.TAG,"${element.className}#${element.methodName}:${element.lineNumber}")
                }
            })

        return filteredTransactions
    }

    internal fun getFilterOptions(): FilterOptions {
        return mFilterOptions
    }

    internal fun applyFilterOptions(filterOptions: FilterOptions) = transactions.value?.let { transactions ->
        viewModelScope.launch {
            filteredTransactions.value = filter(transactions, filterOptions)
        }
    }.also { mFilterOptions = filterOptions }

    internal fun setFilterQuery(query: String) = transactions.value?.let { transactions ->
        mFilterOptions.query = query
        viewModelScope.launch {
            filteredTransactions.value = filter(transactions, mFilterOptions)
        }
    }

    internal fun getFilteredTransactionsOnce() = filteredTransactions.value

    /**
     * Filters the given list of [TransferDetail] given the [FilterOptions] and returns a filtered list
     * of [TransferDetail], doing all the work in a background thread using kotlin coroutines
     */
    private suspend fun filter(transactions: List<TransferDetail>, filterOptions: FilterOptions) : List<TransferDetail> {
        return withContext(Dispatchers.Default) {

            // Create a list to store the filtered transactions
            val filteredTransactions = ArrayList<TransferDetail>()

            // Make sure the filter dates use the same format as the transactions' dates
            val startDate = filterOptions.startDate / 1000
            val endDate = filterOptions.endDate / 1000

            for (transaction in transactions) {
                // Filter by transfer direction
                if (transaction.direction) { // Transfer sent
                    if (filterOptions.transactionsDirection == 1)
                    // Looking for received transfers only
                        continue
                } else { // Transfer received
                    if (filterOptions.transactionsDirection == 2)
                    // Looking for sent transactions only
                        continue
                }

                // Filter by date range
                if (!filterOptions.dateRangeAll && (transaction.date < startDate ||
                            transaction.date > endDate))
                    continue

                // Filter by asset
                if (!filterOptions.assetAll && transaction.assetSymbol != filterOptions.asset)
                    continue

                // Filter by equivalent value
                if (!filterOptions.equivalentValueAll && ((transaction.fiatAmount ?: -1 ) < filterOptions.fromEquivalentValue
                            || (transaction.fiatAmount ?: -1) > filterOptions.toEquivalentValue))
                    continue

                // Filter transactions sent to agorise
                if (filterOptions.agoriseFees && transaction.to.equals("agorise"))
                    continue

                // Filter by search query
                val text = "${transaction.from ?: ""} ${transaction.to ?: ""} ${transaction.memo}"
                if (text.contains(filterOptions.query, ignoreCase = true)) {
                    filteredTransactions.add(transaction)
                }
            }

            filteredTransactions
        }
    }
}