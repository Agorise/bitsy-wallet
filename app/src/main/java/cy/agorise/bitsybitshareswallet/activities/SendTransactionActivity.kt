package cy.agorise.bitsybitshareswallet.activities

import android.app.Activity
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
import cy.agorise.bitsybitshareswallet.application.CrystalSecurityMonitor
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.dialogs.CrystalDialog
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.bitsybitshareswallet.interfaces.OnResponse
import cy.agorise.bitsybitshareswallet.models.*
import cy.agorise.bitsybitshareswallet.repository.RepositoryManager
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequestListener
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequests
import cy.agorise.bitsybitshareswallet.requestmanagers.ValidateBitsharesSendRequest
import cy.agorise.bitsybitshareswallet.requestmanagers.ValidateExistBitsharesAccountRequest
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.CryptoNetAccountAdapter
import cy.agorise.bitsybitshareswallet.viewmodels.CryptoNetAccountListViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.validators.SendTransactionValidator
import cy.agorise.bitsybitshareswallet.viewmodels.validators.UIValidatorListener
import cy.agorise.bitsybitshareswallet.viewmodels.validators.ValidationField
import cy.agorise.bitsybitshareswallet.views.natives.CryptoCurrencyAdapter
import cy.agorise.graphenej.Chains
import cy.agorise.graphenej.Invoice
import me.dm7.barcodescanner.zxing.ZXingScannerView
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class SendTransactionActivity : CustomActivity(), ZXingScannerView.ResultHandler{

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

    private var valid:Boolean = true

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
            this.cryptoNetAccount = RepositoryManager.getAccountsRepository(globalActivity).getCryptoNetLocalAcount()

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

        etTo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                validateAccount()
                validateAllFields()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
        })

        etAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                validateAmount()
                validateAllFields()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
        })

        etMemo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                validateAllFields()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
        })

        scanning.setOnClickListener(){
            
            var intent:Intent = Intent(globalActivity,QRCodeActivity::class.java)
            startActivityForResult(intent,CAMERA_OPEN)
            
        }

        btnSend.setOnClickListener(){

            val thisFragment = this

            val fromAccountSelected = RepositoryManager.getAccountsRepository(globalActivity).getCryptoNetLocalAcount()

            /*
         * this is only for graphene accounts.
         *
         **/
            val grapheneAccountSelected = GrapheneAccount(fromAccountSelected!!)
            grapheneAccountSelected.loadInfo(db!!.grapheneAccountInfoDao().getByAccountId(fromAccountSelected.id))

            val toAccount:String = etTo.text.trim().toString()
            val amount:String = this.etAmount.text.toString()

            if (valid) {

                val request = ValidateExistBitsharesAccountRequest(toAccount)
                request.listener = object : CryptoNetInfoRequestListener {
                    override fun onCarryOut() {
                        if (!request.getAccountExists()) {
                            Toast.makeText(
                                globalActivity,
                                globalActivity.getString(R.string.account_name_not_exist),
                                Toast.LENGTH_LONG
                            )
                            valid = false
                        } else {


                            val amountFromEditText = java.lang.Double.parseDouble(amount)
                            val amount = Math.floor(
                                amountFromEditText * Math.round(
                                    Math.pow(
                                        10.0,
                                        CryptoCoin.BITSHARES.precision.toDouble()
                                    )
                                )
                            ).toLong()

                            val sendRequest = ValidateBitsharesSendRequest(
                                globalActivity,
                                grapheneAccountSelected,
                                toAccount,
                                amount,
                                CryptoCoin.BITSHARES.name!!,
                                etMemo.text.toString()
                            )

                            sendRequest.listener  = object : CryptoNetInfoRequestListener {
                                override fun onCarryOut() {
                                    if (sendRequest.status.equals(ValidateBitsharesSendRequest.StatusCode.SUCCEEDED)) {
                                        try {
                                            crystalDialog!!.dismiss()
                                            finish()
                                        } catch (throwable: Throwable) {
                                            throwable.printStackTrace()
                                        }

                                    } else {
                                        Toast.makeText(
                                            globalActivity,
                                            globalActivity.getString(R.string.unable_to_send_amount),
                                            Toast.LENGTH_LONG
                                        )
                                    }
                                }
                            }


                        }
                    }
                }
                CryptoNetInfoRequests.getInstance()!!.addRequest(request)
            }
            
        }
        
    }


    fun validateAmount():Boolean{

        etAmountError.setText("")

        try {
            val newAmountValue = java.lang.Float.parseFloat(etAmount.text.toString())

            var cryptoCurrency:CryptoCurrency = CryptoCurrency()
            cryptoCurrency.id = 1
            cryptoCurrency.name = "BITCOIN";
            cryptoCurrency.precision = 1

            //val cryptoCurrency = assetSpinner.selectedItem as CryptoCurrency

            /*
            * Validation for the money
            * */
            if (cryptoCurrency == null) {
                etAmountError.setText(globalActivity.getString(R.string.send_assets_error_invalid_cypto_coin_selected))
                return false
            }

            val idCurrency = if (cryptoCurrency == null) "null " else java.lang.Long.toString(cryptoCurrency!!.id)
            val mixedValues = newAmountValue.toString() + "_" + idCurrency

            val account = RepositoryManager.getAccountsRepository(globalActivity).getCryptoNetLocalAcount()

            val balance = BitsyDatabase.getAppDatabase(globalActivity)!!.cryptoCoinBalanceDao()
                .getBalanceFromAccount(account!!.id, cryptoCurrency!!.id)

            var balanceDouble = 0.0
            if (balance != null) {
                balanceDouble = balance!!.balance!!.toDouble()
            }

            if (newAmountValue > balanceDouble) {

                etAmountError.setText(globalActivity.getString(R.string.insufficient_amount))
                return false

            } else if (newAmountValue == 0f) {

                etAmountError.setText(globalActivity.getString(R.string.amount_should_be_greater_than_zero))
                return false

            } else {
               return true
            }
        } catch (e: NumberFormatException) {

            etAmountError.setText(globalActivity.getString(R.string.please_enter_valid_amount))
            return false
        }
    }



    fun validateAccount():Boolean{

        etToError.setText("")

        val fromAccountSelected = RepositoryManager.getAccountsRepository(globalActivity).getCryptoNetLocalAcount()
        val toNewValue:String = etTo.text.toString().trim()

        if (fromAccountSelected!!.name == toNewValue) {
            etToError.setText(globalActivity.getString(R.string.warning_msg_same_account))
            return false
        } else {
            return true
        }

    }

    fun validateAllFields(){

        valid = true

        etMemoError.setText("")

        if(etTo.text.isEmpty()){
            valid = false
        }
        else if(etAmount.text.isEmpty()){
            valid = false
        }
        else if(etMemo.text.isEmpty()){
            valid = false
        }
        else if(!validateAccount()){
            valid = false
        }
        else if(!validateAmount()){
            valid = false
        }

        if(valid){
            btnSend.isEnabled = true
        }
        else{
            btnSend.isEnabled = false
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


    fun onValidationSucceeded(field: ValidationField) {
        val fragment = this

        globalActivity.runOnUiThread(Runnable {

            if (btnSend != null) {
                if (valid) {
                    btnSend.isEnabled = true
                } else {
                    btnSend.isEnabled = false
                }
            }
        })
    }

    fun onValidationFailed(field: ValidationField) {
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