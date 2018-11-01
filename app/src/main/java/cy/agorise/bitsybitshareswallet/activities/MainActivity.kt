package cy.agorise.bitsybitshareswallet.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

import androidx.fragment.app.FragmentPagerAdapter
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import cy.agorise.bitsybitshareswallet.utils.BuildConfig
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.fragments.BalancesFragment
import cy.agorise.bitsybitshareswallet.fragments.MerchantsFragment
import cy.agorise.bitsybitshareswallet.fragments.TransactionsFragment

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    /**
     * The [androidx.fragment.app.FragmentPagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [androidx.fragment.app.FragmentStatePagerAdapter].
     */
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        viewPager.adapter = mSectionsPagerAdapter
        tabLayout.setupWithViewPager(viewPager)

        // Force first tab to show BTS icon
        tabLayout.getTabAt(0)?.setIcon(R.drawable.tab_home_selector)

        initBottomBar()

        ivSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initBottomBar() {
        // Show app version number in bottom bar
        tvBuildVersion.text = String.format("v%s", BuildConfig.VERSION_NAME)

        // Show block number in bottom bar
        tvBlockNumber.text = getString(R.string.block_number_bottom_bar, "-----")

        // TODO add listener to update block number
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0       -> BalancesFragment()
                1       -> TransactionsFragment()
                else    -> MerchantsFragment()
            }
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0       -> ""
                1       -> getString(R.string.title_transactions)
                else    -> getString(R.string.title_merchants)
            }
        }
    }
}
