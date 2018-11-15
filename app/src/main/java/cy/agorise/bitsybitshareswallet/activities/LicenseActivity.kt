package cy.agorise.bitsybitshareswallet.activities

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.models.CryptoNetAccount
import cy.agorise.bitsybitshareswallet.repository.RepositoryManager
import cy.agorise.bitsybitshareswallet.utils.Constants
import kotlinx.android.synthetic.main.activity_license.*

class LicenseActivity : CustomActivity(){

    internal lateinit var db: BitsyDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_license)

        val cryptoNetAccount: CryptoNetAccount  = RepositoryManager.getAccountsRepository(this).getCryptoNetLocalAcount()!!

        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.KEY_LICENCE_AGREED, false)) {

            if(cryptoNetAccount != null) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            else{
                val intent = Intent(this, AccountActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        else{

            /*
            * Load the licence only if it is necesarry
            * */
            val html = getString(R.string.licence_html)
            wvEULA.loadData(html, "text/html", "UTF-8")
        }

        btnAgree.setOnClickListener(){

            PreferenceManager.getDefaultSharedPreferences(baseContext).edit()
                .putBoolean(Constants.KEY_LICENCE_AGREED, true).apply()

            val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnDisAgree.setOnClickListener(){
            System.exit(0)
        }
    }
}