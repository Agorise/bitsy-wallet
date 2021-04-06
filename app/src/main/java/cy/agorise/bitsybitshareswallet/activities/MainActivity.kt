package cy.agorise.bitsybitshareswallet.activities

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.databinding.ActivityMainBinding
import cy.agorise.bitsybitshareswallet.utils.Constants

/**
 * Uses the AAC Navigation Component with a NavHostFragment which is the place where all Fragments are shown,
 * following the philosophy of using a single Activity.
 */
class MainActivity : ConnectedActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    // Handler and Runnable used to add a timer for user inaction and close the app if enough time has passed
    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sets the theme to night mode if it has been selected by the user
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)
        ) {
            setTheme(R.style.Theme_Bitsy_Dark)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment? ?: return

        // Set up Action Bar with Navigation's controller
        val navController = host.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)

        // Sets up the ActionBar with the navigation controller so that it automatically responds to clicks on toolbar
        // menu items and shows the up navigation button on all fragments except home (Balances)
        setupActionBarWithNavController(navController, appBarConfiguration)

        mHandler = Handler()

        // When this runnable finishes it first verifies if the auto close feature is enabled and if it is then it
        // closes the app, if not then it just restarts the Handler (timer)
        mRunnable = Runnable {
            if (PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(Constants.KEY_AUTO_CLOSE_ACTIVATED, true)
            ) {
                finish()
                android.os.Process.killProcess(android.os.Process.myPid())
            } else
                restartHandler()
        }
        startHandler()
    }

    /**
     * Restarts the Handler (timer) each time there is user's interaction
     */
    override fun onUserInteraction() {
        super.onUserInteraction()

        Log.d("MainActivity", "onUserInteraction")
        restartHandler()
    }

    /**
     * Stops and then restarts the Handler
     */
    private fun restartHandler() {
        stopHandler()
        startHandler()
    }

    private fun stopHandler() {
        mHandler.removeCallbacks(mRunnable)
    }

    private fun startHandler() {
        mHandler.postDelayed(mRunnable, 3 * 60 * 1000) //for 3 minutes
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Have the NavigationUI look for an action or destination matching the menu
        // item id and navigate there if found.
        // Otherwise, bubble up to the parent.
        return item.onNavDestinationSelected(findNavController(R.id.navHostFragment))
                || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        // Allows NavigationUI to support proper up navigation or the drawer layout
        // drawer menu, depending on the situation
        return findNavController(R.id.navHostFragment).navigateUp(appBarConfiguration)
    }

    override fun onBackPressed() {
        // Trick used to avoid crashes when the user is in the License or ImportBrainkey and presses the back button
        val currentDestination = binding.navHostFragment.findNavController().currentDestination
        when (currentDestination?.id) {
            R.id.license_dest, R.id.import_brainkey_dest -> finish()
            else -> super.onBackPressed()
        }
    }
}
