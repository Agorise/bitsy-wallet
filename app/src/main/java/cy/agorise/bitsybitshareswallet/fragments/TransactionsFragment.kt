package cy.agorise.bitsybitshareswallet.fragments

import android.R.*
import android.app.Activity
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.models.CryptoCoinTransactionExtended
import cy.agorise.bitsybitshareswallet.repository.RepositoryManager
import cy.agorise.bitsybitshareswallet.viewmodels.TransactionListViewModel
import cy.agorise.bitsybitshareswallet.views.natives.TransactionOrderSpinnerAdapter
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.transaction_list.*
import java.text.SimpleDateFormat
import java.util.*


class TransactionsFragment : Fragment() {

    internal lateinit var balanceRecyclerView: RecyclerView
    internal lateinit var etTransactionSearch: TextView
    internal lateinit var spTransactionsOrder: Spinner

    internal lateinit var transactionListViewModel: TransactionListViewModel
    internal var transactionsLiveData: LiveData<PagedList<CryptoCoinTransactionExtended>>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_transactions, container, false)

        // Gets the Balance RecyclerView
        balanceRecyclerView = view.findViewById(R.id.transactionListView)

        transactionListViewModel = ViewModelProviders.of(this).get(TransactionListViewModel::class.java)

        etTransactionSearch = view.findViewById(R.id.etTransactionSearch)
        etTransactionSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                changeTransactionList()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
        })
        spTransactionsOrder = view!!.findViewById(R.id.spTransactionsOrder)
        //initTransactionsOrderSpinner()
        //changeTransactionList()
        return view
    }

    fun changeTransactionList() {

        if(spTransactionsOrder!!.selectedItem != null){

            val orderSelected =
                spTransactionsOrder!!.selectedItem as TransactionOrderSpinnerAdapter.TransactionOrderSpinnerItem

            if (transactionsLiveData != null) {
                transactionsLiveData!!.removeObservers(this)
            }
            transactionListViewModel.initTransactionList(orderSelected.field, etTransactionSearch!!.text.toString())
            transactionsLiveData = transactionListViewModel.transactionList

            val fragment = this
            transactionsLiveData!!.observe(this, object : Observer<PagedList<CryptoCoinTransactionExtended>> {
                override fun onChanged(@Nullable cryptoCoinTransactions: PagedList<CryptoCoinTransactionExtended>) {
                    vTransactionListView.setData(cryptoCoinTransactions, fragment)
                }
            })
        }
    }

    private fun initTransactionsOrderSpinner() {
        val spinnerValues = ArrayList<TransactionOrderSpinnerAdapter.TransactionOrderSpinnerItem>()
        spinnerValues.add(TransactionOrderSpinnerAdapter.TransactionOrderSpinnerItem("date", "Date", 0, false))
        spinnerValues.add(TransactionOrderSpinnerAdapter.TransactionOrderSpinnerItem("amount", "Amount", 0, false))
        spinnerValues.add(TransactionOrderSpinnerAdapter.TransactionOrderSpinnerItem("is_input", "In/Out", 0, false))
        spinnerValues.add(TransactionOrderSpinnerAdapter.TransactionOrderSpinnerItem("from", "From", 0, false))
        spinnerValues.add(TransactionOrderSpinnerAdapter.TransactionOrderSpinnerItem("to", "To", 0, false))

        val transactionOrderSpinnerAdapter = TransactionOrderSpinnerAdapter(
            context!!, layout.simple_spinner_item, spinnerValues
        )

        spTransactionsOrder!!.adapter = transactionOrderSpinnerAdapter

        spTransactionsOrder!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                changeTransactionList()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {

            }
        }
    }

    companion object {

        fun newInstance(): TransactionsFragment {
            val fragment = TransactionsFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

}// Required empty public constructor