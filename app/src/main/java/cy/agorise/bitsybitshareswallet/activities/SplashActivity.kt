package cy.agorise.bitsybitshareswallet.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import cy.agorise.bitsybitshareswallet.R


class SplashActivity : CustomActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler().postDelayed({ checkWhereToGo() }, 1500)
    }

    private fun checkWhereToGo() {

        openLicenceScreen()

        finish()
    }

    private fun openLicenceScreen(){

        val myIntent = Intent(this, LicenseActivity::class.java)
        startActivity(myIntent)
    }

    private fun openMainScreen(){

        val myIntent = Intent(this, MainActivity::class.java)
        startActivity(myIntent)
    }
}
