package cy.agorise.bitsybitshareswallet.activities

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants
import kotlinx.android.synthetic.main.activity_setting.*

/**
 * A simple activity for the user to select his preferences
 */
class SettingsActivity : CustomActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sets the theme to night mode if it has been selected by the user
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)
        ) {
            setTheme(R.style.AppTheme_Dark)
        }

        setContentView(R.layout.activity_setting)

        setupActionBar()

        initNightModeSwitch()

        initMinutesClose()

        txtAccount.text = "dtvvdtvv-123456"

        brainkey_ic.setOnClickListener(){

            var intent: Intent = Intent(globalActivity,CopyBrainkey::class.java)
            intent.putExtra("newAccount",false)
            startActivity(intent)
        }
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initMinutesClose() {
        val minutesClose = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(Constants.KEY_MINUTES_CLOSE_MODE_ACTIVATED, false)

        close_bitshare.isChecked = minutesClose

        close_bitshare.setOnCheckedChangeListener { checkbox, isChecked ->

            PreferenceManager.getDefaultSharedPreferences(baseContext).edit()
                .putBoolean(Constants.KEY_MINUTES_CLOSE_MODE_ACTIVATED, isChecked).apply()
        }
    }

    private fun initNightModeSwitch() {
        val nightModeOn = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)

        switchNightMode.isChecked = nightModeOn

        switchNightMode.setOnCheckedChangeListener { buttonView, isChecked ->

            PreferenceManager.getDefaultSharedPreferences(buttonView.context).edit()
                    .putBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, isChecked).apply()

            // Recreates the activity to apply the selected theme
            this.recreate()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
