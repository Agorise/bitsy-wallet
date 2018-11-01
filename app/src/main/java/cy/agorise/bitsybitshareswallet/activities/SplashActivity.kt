package cy.agorise.bitsybitshareswallet.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.fragments.BalancesFragment


class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler().postDelayed({ checkWhereToGo() }, 1500)
    }

    private fun checkWhereToGo() {
        val i = Intent(this@SplashActivity, BalancesFragment::class.java)
        startActivity(i)
        finish()
    }
}
