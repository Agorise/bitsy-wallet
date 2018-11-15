package cy.agorise.bitsybitshareswallet.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.room.InvalidationTracker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.Result
import kotlinx.android.synthetic.main.activity_send_transaction.*
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.dialogs.CrystalDialog
import cy.agorise.bitsybitshareswallet.models.AccountSeed
import cy.agorise.bitsybitshareswallet.models.CryptoCoinBalance
import cy.agorise.bitsybitshareswallet.models.CryptoNetAccount
import cy.agorise.bitsybitshareswallet.models.GrapheneAccount
import cy.agorise.bitsybitshareswallet.repository.RepositoryManager
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.CryptoNetAccountAdapter
import cy.agorise.bitsybitshareswallet.viewmodels.CryptoNetAccountListViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.validators.SendTransactionValidator
import cy.agorise.bitsybitshareswallet.viewmodels.validators.UIValidatorListener
import cy.agorise.bitsybitshareswallet.viewmodels.validators.ValidationField
import cy.agorise.bitsybitshareswallet.views.natives.CryptoCurrencyAdapter
import cy.agorise.graphenej.Invoice
import me.dm7.barcodescanner.zxing.ZXingScannerView
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class SendTransactionActivity : CustomActivity(), UIValidatorListener, ZXingScannerView.ResultHandler{

    internal lateinit var sendTransactionValidator: SendTransactionValidator
    internal lateinit var assetAdapter: CryptoCurrencyAdapter
    var CAMERA_OPEN:Int = 100


    /*
     * Flag to control when the camera is visible and when is hide
     * */
    private var cameraVisible = true

    private var cryptoNetAccountId: Long = 0
    private var cryptoNetAccount: CryptoNetAccount? = null
    private var grapheneAccount: GrapheneAccount? = null
    private var db: BitsyDatabase? = null
    private var fabSend: FloatingActionButton? = null
    private var builder: AlertDialog.Builder? = null

    /*
        Dialog for loading
    */
    private var crystalDialog: CrystalDialog? = null
    
    

    
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

        builder = AlertDialog.Builder(this, R.style.dialog_theme_full)

        this.cryptoNetAccountId = intent.getLongExtra("CRYPTO_NET_ACCOUNT_ID", -1)

        if (this.cryptoNetAccountId != -1.toLong()) {
            db = BitsyDatabase.getAppDatabase(this)
            this.cryptoNetAccount = db!!.cryptoNetAccountDao().getById(cryptoNetAccountId)

            /*
             * this is only for graphene accounts.
             *
             **/
            this.grapheneAccount = GrapheneAccount(this.cryptoNetAccount!!)
            this.grapheneAccount!!.loadInfo(db!!.grapheneAccountInfoDao().getByAccountId(this.cryptoNetAccountId))

            val balancesList = db!!.cryptoCoinBalanceDao().getBalancesFromAccount(cryptoNetAccountId)
            balancesList.observe(this, object : Observer<List<CryptoCoinBalance>> {
                override fun onChanged(@Nullable cryptoCoinBalances: List<CryptoCoinBalance>) {
                    val assetIds = ArrayList<Long>()
                    for (nextBalance in balancesList.value!!) {
                        assetIds.add(nextBalance.cryptoCurrencyId)
                    }
                    val cryptoCurrencyList = db!!.cryptoCurrencyDao().getByIds(assetIds)

                    /*
                     * Test
                     * */
                    /*CryptoCurrency crypto1 = new CryptoCurrency();
                    crypto1.setId(1);
                    crypto1.setName("BITCOIN");
                    crypto1.setPrecision(1);
                    cryptoCurrencyList.add(crypto1);*/

                    /*assetAdapter = CryptoCurrencyAdapter(
                        globalActivity,
                        android.R.layout.simple_spinner_item,
                        cryptoCurrencyList
                    )*/
                    //spAsset.setAdapter(assetAdapter)
                }
            })

            // TODO SendTransactionValidator to accept spFrom
            sendTransactionValidator = SendTransactionValidator(
                globalActivity,
                this.cryptoNetAccount!!,
                etTo,
                this.cryptoNetAccount!!.name!!,
                etAmount,
                etMemo
            )
            sendTransactionValidator.listener = this

            val cryptoNetAccountListViewModel =
                ViewModelProviders.of(this).get(CryptoNetAccountListViewModel::class.java)
            val cryptoNetAccounts = cryptoNetAccountListViewModel.cryptoNetAccountList
            val fromSpinnerAdapter =
                CryptoNetAccountAdapter(globalActivity, android.R.layout.simple_spinner_item, cryptoNetAccounts)
        }
        
        scanning.setOnClickListener{

            val intent = Intent(this, QRCodeActivity::class.java)
            intent.putExtra("id", 0)
            startActivityForResult(intent, 90)
        }

        etAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                sendTransactionValidator.validate()
            }
        })

        scanning.setOnClickListener(){
            
            var intent:Intent = Intent(globalActivity,QRCodeActivity::class.java)
            startActivityForResult(intent,CAMERA_OPEN)
            
        }

        btnSend.setOnClickListener(){
            
            
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


    override fun onValidationSucceeded(field: ValidationField) {
        val fragment = this

        globalActivity.runOnUiThread(Runnable {

            if (btnSend != null) {
                if (sendTransactionValidator.isValid) {
                    btnSend.isEnabled = true
                } else {
                    btnSend.isEnabled = false
                }
            }
        })
    }

    override fun onValidationFailed(field: ValidationField) {
        globalActivity.runOnUiThread(Runnable {
            if (field.view === etTo) {
                Toast.makeText(globalActivity, field.message,
                    Toast.LENGTH_LONG).show();
            } else if (field.view === etAmount) {
                Toast.makeText(globalActivity, field.message,
                    Toast.LENGTH_LONG).show();
            } else if (field.view === etMemo) {
                Toast.makeText(globalActivity, field.message,
                    Toast.LENGTH_LONG).show();
            }
        })
    }

    override fun handleResult(result: Result) {
        try {
            val invoice = Invoice.fromQrCode(result.text)

            etTo.setText(invoice.to)

            for (i in 0 until assetAdapter.count) {
                if (assetAdapter.getItem(i).name.equals(invoice.currency)) {
                    //spAsset.setSelection(i)
                    break
                }
            }
            etMemo.setText(invoice.memo)


            var amount = 0.0
            for (nextItem in invoice.lineItems) {
                amount += nextItem.quantity * nextItem.price
            }
            val df = DecimalFormat("####.####")
            df.roundingMode = RoundingMode.CEILING
            df.decimalFormatSymbols = DecimalFormatSymbols(Locale.ENGLISH)
            etAmount.setText(df.format(amount))
            Log.i("SendFragment", result.text)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}