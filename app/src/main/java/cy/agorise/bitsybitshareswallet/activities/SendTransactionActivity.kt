package cy.agorise.bitsybitshareswallet.activities

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_send_transaction.*
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants

class SendTransactionActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sets the theme to night mode if it has been selected by the user
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)
        ) {
            setTheme(R.style.AppTheme_Dark)
        }

        setContentView(R.layout.activity_send_transaction)

        setBackButton(true)

        setTitle(getResources().getString(R.string.send_screen_name))

        //tvAppVersion_send_screen_activity.text = "v" + BuildConfig.VERSION_NAME + getString(R.string.beta)

        scanning.setOnClickListener{

            val intent = Intent(this, QRCodeActivity::class.java)
            intent.putExtra("id", 0)
            startActivityForResult(intent, 90)
        }
    }


    fun setBackButton(isBackButton: Boolean?) {
        if (isBackButton!!) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home ->{
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}