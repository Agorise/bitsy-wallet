package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.BalancesAdapter
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceDetailViewModel
import kotlinx.android.synthetic.main.fragment_balances.*

class BalancesFragment: Fragment() {

    private lateinit var mBalanceDetailViewModel: BalanceDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_balances, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure BalanceDetailViewModel to show the current balances
        mBalanceDetailViewModel = ViewModelProviders.of(this).get(BalanceDetailViewModel::class.java)

        val balancesAdapter = BalancesAdapter(context!!)
        rvBalances.adapter = balancesAdapter
        rvBalances.layoutManager = LinearLayoutManager(context!!)
        rvBalances.addItemDecoration(DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL))

        mBalanceDetailViewModel.getAll().observe(this, Observer<List<BalanceDetail>> { balancesDetails ->
            balancesAdapter.replaceAll(balancesDetails)
        })
    }
}