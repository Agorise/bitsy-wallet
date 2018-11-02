package cy.agorise.bitsybitshareswallet.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

import androidx.fragment.app.FragmentPagerAdapter
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import cy.agorise.bitsybitshareswallet.utils.BuildConfig
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.fragments.BalancesFragment
import cy.agorise.bitsybitshareswallet.fragments.MerchantsFragment
import cy.agorise.bitsybitshareswallet.fragments.TransactionsFragment
import cy.agorise.bitsybitshareswallet.utils.Constants

import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    var SETTINGS_SELECTED:Int = 1

    private var timer: Timer? = null

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

        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)
        ) {
            setTheme(R.style.AppTheme_Dark)
        }

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
            startActivityForResult(intent,SETTINGS_SELECTED)
        }
    }


    override fun onPause() {
        super.onPause()

        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.KEY_MINUTES_CLOSE_MODE_ACTIVATED, false)
        ) {
            timer = Timer()
            val logoutTimeTask = LogOutTimerTask()
            timer!!.schedule(logoutTimeTask, 180000) //auto logout in 3 minutes
        }
    }

    override fun onResume() {
        super.onResume()
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == SETTINGS_SELECTED){
            recreate()
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

    private inner class LogOutTimerTask : TimerTask() {

        override fun run() {
            System.exit(0);
        }
    }
}
