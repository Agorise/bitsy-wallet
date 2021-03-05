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
import cy.agorise.bitsybitshareswallet.adapters.BalancesAdapter
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail
import cy.agorise.bitsybitshareswallet.databinding.FragmentBalancesBinding
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceDetailViewModel

class BalancesFragment : Fragment() {

    private var _binding: FragmentBalancesBinding? = null
    private val binding get() = _binding!!

    private lateinit var mBalanceDetailViewModel: BalanceDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        _binding = FragmentBalancesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure BalanceDetailViewModel to show the current balances
        mBalanceDetailViewModel =
            ViewModelProviders.of(this).get(BalanceDetailViewModel::class.java)

        val balancesAdapter = BalancesAdapter(context!!)
        binding.rvBalances.adapter = balancesAdapter
        binding.rvBalances.layoutManager = LinearLayoutManager(context!!)
        binding.rvBalances.addItemDecoration(
            DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        )

        mBalanceDetailViewModel.getAll()
            .observe(this, Observer<List<BalanceDetail>> { balancesDetails ->
                balancesAdapter.replaceAll(balancesDetails)
            })
    }
}
