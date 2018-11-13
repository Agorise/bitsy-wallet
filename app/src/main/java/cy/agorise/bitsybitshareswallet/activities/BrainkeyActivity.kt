package cy.agorise.bitsybitshareswallet.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.dialogs.*
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequestListener
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequests
import cy.agorise.bitsybitshareswallet.requestmanagers.ImportBitsharesAccountRequest
import cy.agorise.bitsybitshareswallet.viewmodels.AccountSeedViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.validators.ImportSeedValidator
import cy.agorise.bitsybitshareswallet.viewmodels.validators.ImportSeedValidatorV2
import cy.agorise.bitsybitshareswallet.viewmodels.validators.UIValidatorListener
import cy.agorise.bitsybitshareswallet.viewmodels.validators.ValidationField
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.android.synthetic.main.activity_brainkey.*

class BrainkeyActivity: CustomActivity(), UIValidatorListener {

    internal lateinit var accountSeedViewModel: AccountSeedViewModel
    internal lateinit var importSeedValidator: ImportSeedValidatorV2

    internal val activity: Activity = this

    /*
    * Flag to check correct PIN equality
    * */
    private var pinsOK = false





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_brainkey)

        /*
        * Initially the button CREATE WALLET should be disabled
        * */
        disableCreate()

        /*
        * When a text change in any of the fields
        * */
        etPin__.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {

                /*
                 * Validate that PINs are equals
                 * */
                validatePINS()

                /*
                 * If all is ready to continue enable the button, contrarie case disable it
                 * */
                if (allFieldsAreOK()) {
                    enableCreate()
                } else {
                    disableCreate()
                }
            }
        })
        etPinConfirmation__.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {

                /*
                 * Validate that PINs are equals
                 * */
                validatePINS()

                /*
                 * If all is ready to continue enable the button, contrarie case disable it
                 * */
                if (allFieldsAreOK()) {
                    enableCreate()
                } else {
                    disableCreate()
                }
            }
        })
        etSeedWords__.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {

                /*
                 * Validate that PINs are equals
                 * */
                validatePINS()

                /*
                 * If all is ready to continue enable the button, contrarie case disable it
                 * */
                if (allFieldsAreOK()) {
                    enableCreate()
                } else {
                    disableCreate()
                }

                /*
                * Hide error field
                * */
                clearErrors()
            }
        })

        btnCancel.setOnClickListener(){
            finish()
        }

        btnImport.setOnClickListener(){

            val thisActivity = this

            if (pinsOK) {

                /*
             * Question if continue
             * */
                val questionDialog = QuestionDialog(activity)
                questionDialog.setText(R.string.question_continue)
                questionDialog.setOnNegative(object : NegativeResponse {
                    override fun onNegative(dialogMaterial: DialogMaterial) {}
                })
                questionDialog.setOnPositive(object : PositiveResponse {
                    override fun onPositive() {

                        /*
                            * Loading dialog
                        * */
                        val crystalLoading = Loading(activity)
                        crystalLoading.show()

                        /*
                     * Final service connection
                     * */
                        finalStep(crystalLoading)

                        /*
                     * Validate mnemonic with the server
                     * */
                        /*final ImportBitsharesAccountRequest request = new ImportBitsharesAccountRequest(etSeedWords.getText().toString().trim(),activity);
                    request.setListener(new CryptoNetInfoRequestListener() {
                        @Override
                        public void onCarryOut() {
                            if(request.getStatus().equals(ImportBitsharesAccountRequest.StatusCode.SUCCEEDED)){

                                //Correct

                                finalStep(crystalLoading);

                            }
                            else{

                                crystalLoading.dismiss();

                                txtErrorAccount__.setVisibility(View.VISIBLE);
                                txtErrorAccount__.setText(activity.getResources().getString(R.string.error_invalid_account));
                            }
                        }
                    });
                    CryptoNetInfoRequests.getInstance().addRequest(request);*/

                    }
                })
                questionDialog.show()
            }
        }

        accountSeedViewModel = ViewModelProviders.of(this).get(AccountSeedViewModel::class.java)
        importSeedValidator = ImportSeedValidatorV2(activity, etPin__, etPinConfirmation__)
        importSeedValidator.listener = this
    }


    private fun clearErrors() {
        txtErrorPIN__.setVisibility(View.INVISIBLE)
        txtErrorAccount__.setVisibility(View.INVISIBLE)
    }

    /*
    * Validate that PINs are equals
    * */
    private fun validatePINS() {

        val pin = etPin__.getText().toString().trim({ it <= ' ' })
        val confirmoPIN = etPinConfirmation__.getText().toString().trim({ it <= ' ' })
        if (!pin.isEmpty() && !confirmoPIN.isEmpty()) {
            if (pin.compareTo(confirmoPIN) != 0) {
                pinsOK = false
                txtErrorPIN__.setVisibility(View.VISIBLE)
            } else {
                pinsOK = true
                clearErrors()
            }
        } else {
            pinsOK = false
            clearErrors()
        }
    }


    /*
    *   Method to validate if all the fields are fill and correctly
    * */
    private fun allFieldsAreOK(): Boolean {

        var complete = false
        if (etPin__.getText().toString().trim().compareTo("") != 0 &&
            etPinConfirmation__.getText().toString().trim().compareTo("") != 0 &&
            etSeedWords__.text.toString().trim().compareTo("") != 0 /*&&
                etAccountName.getText().toString().trim().compareTo("")!=0*/) {
            if (pinsOK) {
                complete = true
            }
        }
        return complete
    }

    private fun finalStep(crystalLoading: Loading) {

        val thisActivity = this

        /*val validatorRequest = ImportBitsharesAccountRequest(etSeedWords__.text.toString(), applicationContext, true)

        validatorRequest.listener = object : CryptoNetInfoRequestListener {
            override fun onCarryOut() {

                /*
                 * Hide the loading dialog
                 * */
                crystalLoading.dismiss()

                if (!validatorRequest.status.equals(ImportBitsharesAccountRequest.StatusCode.SUCCEEDED)) {

                    when (validatorRequest.status) {
                        ImportBitsharesAccountRequest.StatusCode.PETITION_FAILED, ImportBitsharesAccountRequest.StatusCode.NO_INTERNET, ImportBitsharesAccountRequest.StatusCode.NO_SERVER_CONNECTION -> thisActivity.runOnUiThread(Runnable {
                            Toast.makeText(
                                thisActivity, thisActivity.getResources().getString(R.string.NO_SERVER_CONNECTION),
                                Toast.LENGTH_LONG
                            ).show()
                        })
                        ImportBitsharesAccountRequest.StatusCode.ACCOUNT_DOESNT_EXIST -> thisActivity.runOnUiThread(Runnable {

                            Toast.makeText(
                                thisActivity, thisActivity.getResources().getString(R.string.ACCOUNT_DOESNT_EXIST),
                                Toast.LENGTH_LONG
                            ).show()
                        })
                        ImportBitsharesAccountRequest.StatusCode.BAD_SEED -> thisActivity.runOnUiThread(Runnable {

                            Toast.makeText(
                                thisActivity, thisActivity.getResources().getString(R.string.BAD_SEED),
                                Toast.LENGTH_LONG
                            ).show()
                        })
                        ImportBitsharesAccountRequest.StatusCode.NO_ACCOUNT_DATA -> thisActivity.runOnUiThread(Runnable {

                            Toast.makeText(
                                thisActivity, thisActivity.getResources().getString(R.string.NO_ACCOUNT_DATA),
                                Toast.LENGTH_LONG
                            ).show()


                        })

                        else -> {

                            Toast.makeText(
                                thisActivity, thisActivity.getResources().getString(R.string.ERROR_UNRECOGNIZABLE),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    thisActivity.runOnUiThread(Runnable {

                        txtErrorAccount__.setVisibility(View.VISIBLE)
                    })

                    //Toast.makeText(thisActivity.getApplicationContext(),errorText,Toast.LENGTH_LONG).show();

                } else {
                    val intent = Intent(thisActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }

        CryptoNetInfoRequests.getInstance()!!.addRequest(validatorRequest)*/

        finish()

        val intent = Intent(thisActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onValidationSucceeded(field: ValidationField) {
        val activity = this

        activity.runOnUiThread {
            if (field.view === etPin_) {
                //tvPinError.setText("");
            } else if (field.view === etPinConfirmation__) {
                //tvPinConfirmationError.setText("");
            } else if (field.view === etAccountName) {
                //tvAccountNameError.setText("");
            } else if (field.view === etSeedWords__) {
                //tvSeedWordsError.setText("");
            }

            if (activity.importSeedValidator.isValid) {
                enableCreate()
            } else {
                disableCreate()
            }
        }
    }
    
    override fun onValidationFailed(field: ValidationField) {
        if (field.view === etPin_) {
            //tvPinError.setText(field.getMessage());
        } else if (field.view === etPinConfirmation__) {
            //tvPinConfirmationError.setText(field.getMessage());
        } else if (field.view === etSeedWords__) {
            //tvSeedWordsError.setText(field.getMessage());
        }
    }
    
    /*
     * Enable create button
     * */
    private fun enableCreate() {
        runOnUiThread {
            //btnImport.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            btnImport.setEnabled(true)
        }
    }

    /*
     * Disable create button
     * */
    private fun disableCreate() {
        runOnUiThread {
            btnImport.setEnabled(false)
            //btnImport.setBackground(getDrawable(R.drawable.disable_style));
        }
    }
}