package cy.agorise.bitsybitshareswallet.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.models.AccountSeed
import cy.agorise.bitsybitshareswallet.viewmodels.AccountSeedViewModel
import kotlinx.android.synthetic.main.activity_copybrainkey.*

class CopyBrainkey:CustomActivity(){

    internal lateinit var accountSeedViewModel: AccountSeedViewModel




    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_copybrainkey)

        /*
        *   If comes from new account creation hide the cancel button
        * */
        val b = intent.extras
        val newAccount = b.getBoolean("newAccount")
        if (newAccount) {
            val layout= btnOK.getParent() as ViewGroup
            layout?.removeView(btnOK)
            //btnOk.setVisibility(View.INVISIBLE);
        }

        val seedId = intent.getLongExtra("SEED_ID", -1)

        if (seedId > -1) {
            /*accountSeedViewModel = ViewModelProviders.of(this).get(AccountSeedViewModel::class.java)
            accountSeedViewModel.loadSeed(seedId)
            val liveDataAccountSeed = accountSeedViewModel.getAccountSeed()
            liveDataAccountSeed!!.observe(this, object : Observer<AccountSeed> {
                override fun onChanged(@Nullable accountSeed: AccountSeed) {
                    textfieldBrainkey.setText(accountSeed.masterSeed)
                }
            })*/

            textfieldBrainkey.setText(intent.getStringExtra("SEED_ID_TMP")) //testing only
        }

        btnCancel.setOnClickListener(){

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnOK.setOnClickListener(){

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnCopy.setOnClickListener(){

            /*
         *  Save to clipboard the brainkey chain
         * */
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(textfieldBrainkey.text, textfieldBrainkey.text.toString())
            clipboard.primaryClip = clip

            /*
         * Success message
         * */
            Toast.makeText(
                this.baseContext,
                resources.getString(R.string.window_seed_toast_clipboard),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}