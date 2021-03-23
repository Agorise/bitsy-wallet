package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import cy.agorise.bitsybitshareswallet.adapters.BalancesAdapter
import cy.agorise.bitsybitshareswallet.databinding.FragmentBalancesBinding
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceDetailViewModel

class BalancesFragment : Fragment() {

    private val viewModel: BalanceDetailViewModel by viewModels()

    private var _binding: FragmentBalancesBinding? = null
    private val binding get() = _binding!!

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
        val balancesAdapter = BalancesAdapter(requireContext())
        binding.rvBalances.adapter = balancesAdapter
        binding.rvBalances.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBalances.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )

        viewModel.getAll().observe(viewLifecycleOwner, { balancesDetails ->
            balancesAdapter.replaceAll(balancesDetails)
        })
    }
}
