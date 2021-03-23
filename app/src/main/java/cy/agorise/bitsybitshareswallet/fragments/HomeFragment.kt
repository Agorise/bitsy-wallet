package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.firebase.crashlytics.FirebaseCrashlytics
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.databinding.FragmentHomeBinding
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.UserAccountViewModel


class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
    }

    private val viewModel: UserAccountViewModel by viewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        val nightMode = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)

        // Forces to show the Bitsy icon to the left of the toolbar and also fix the toolbar color and visibility after
        // returning from other fragments that change those properties, such as SendTransactionFragment (color) and
        // MerchantsFragment (visibility)
        val toolbar: Toolbar? = activity?.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        toolbar?.navigationIcon =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_bitsy_logo_2, null)
        toolbar?.setBackgroundResource(if (!nightMode) R.color.colorPrimary else R.color.colorToolbarDark)
        toolbar?.visibility = View.VISIBLE
        toolbar?.title = getString(R.string.app_name)

        // Makes sure the Navigation and Status bar are not translucent after returning from the MerchantsFragment
        val window = activity?.window
        window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        // Sets the status and navigation bars background color to a dark blue or just dark
        context?.let { context ->
            val statusBarColor = ContextCompat.getColor(
                context,
                if (!nightMode) R.color.colorPrimaryVariant else R.color.colorStatusBarDark
            )
            window?.statusBarColor = statusBarColor
            window?.navigationBarColor = statusBarColor
        }

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        // Get version number of the last agreed license version
        val agreedLicenseVersion = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Constants.KEY_LAST_AGREED_LICENSE_VERSION, 0)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "") ?: ""

        if (agreedLicenseVersion != Constants.CURRENT_LICENSE_VERSION || userId == "") {
            findNavController().navigate(R.id.license_action)
            return
        }

        // Configure UserAccountViewModel to show the current account
        viewModel.getUserAccount(userId).observe(viewLifecycleOwner, { userAccount ->
            if (userAccount != null) {
                binding.tvAccountName.text = userAccount.name
                if (userAccount.isLtm) {
                    // Add the lightning bolt to the start of the account name if it is LTM
                    binding.tvAccountName.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_ltm_account, 0, 0, 0
                    )
                    // Add some padding so that the lightning bolt icon is not too close to the account name text
                    binding.tvAccountName.compoundDrawablePadding = 12
                }
            }
        })

        // Navigate to the Receive Transaction Fragment
        binding.fabReceiveTransaction.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.receive_action)
        )

        // Navigate to the Send Transaction Fragment without activating the camera
        binding.fabSendTransaction.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.send_action)
        )

        // Navigate to the Send Transaction Fragment using Navigation's SafeArgs to activate the camera
        binding.fabSendTransactionCamera.setOnClickListener {
            val action = HomeFragmentDirections.sendAction(true)
            findNavController().navigate(action)
        }

        // Configure ViewPager with PagerAdapter and TabLayout to display the Balances/NetWorth section
        val pagerAdapter = PagerAdapter(childFragmentManager)
        binding.viewPager.adapter = pagerAdapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        // Set the pie chart icon for the third tab
        binding.tabLayout.getTabAt(2)?.setIcon(R.drawable.ic_pie_chart)
    }

    /**
     * Pager adapter to create the placeholder fragments
     */
    private inner class PagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            return if (position == 0)
                BalancesFragment()
            else
                NetWorthFragment()
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> getString(R.string.title_balances)
                1 -> getString(R.string.title_net_worth)
                else -> ""
            }
        }

        override fun getCount(): Int {
            return 3
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
    }
}
