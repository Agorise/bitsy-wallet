package cy.agorise.bitsybitshareswallet.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.dialogs.*
import kotlinx.android.synthetic.main.activity_account.*
import android.widget.Toast
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.interfaces.UIValidatorListener
import cy.agorise.bitsybitshareswallet.models.AccountSeed
import cy.agorise.bitsybitshareswallet.repository.RepositoryManager
import cy.agorise.bitsybitshareswallet.viewmodels.validators.BitsharesAccountNameValidation
import cy.agorise.bitsybitshareswallet.viewmodels.validators.CustomValidationField
import cy.agorise.bitsybitshareswallet.viewmodels.validators.PinDoubleConfirmationValidationField
import cy.agorise.bitsybitshareswallet.views.natives.CustomTextInputEditText


class AccountActivity: CustomActivity(){

    var activity:Activity? = null

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account)
        activity = this

        /*
         * Add the controls to the validator
         * */
        this.fieldsValidator.add(etAccountName)
        this.fieldsValidator.add(etPin_)
        this.fieldsValidator.add(etPinConfirmation)

        /*
        * Validations listener
        * */
        val uiValidatorListener = object : UIValidatorListener {

            override fun onValidationSucceeded(customValidationField: CustomValidationField) {

                try {

                    /*
                     * Remove error
                     * */
                    runOnUiThread {
                        val customTextInputEditText = customValidationField.currentView as CustomTextInputEditText
                        customTextInputEditText.error = null
                        customTextInputEditText.fieldValidatorModel.setValid()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onValidationFailed(customValidationField: CustomValidationField) {

                /*
                 * Set error label
                 * */
                runOnUiThread {
                    val customTextInputEditText = customValidationField.currentView as CustomTextInputEditText
                    customTextInputEditText.error = customTextInputEditText.fieldValidatorModel.message
                    customTextInputEditText.fieldValidatorModel.setInvalid()
                }
            }
        }

        /*
        * Create the pin double validation
        * */
        val pinDoubleConfirmationValidationField = PinDoubleConfirmationValidationField(this, etPin_, etPinConfirmation, uiValidatorListener)

        /*
        * Listener for the validation for success or fail
        * */
        etPin_?.setUiValidator(pinDoubleConfirmationValidationField) //Validator for the field
        etPinConfirmation?.setUiValidator(pinDoubleConfirmationValidationField) //Validator for the field

        /*
        * Account name validator
        * */
        val bitsharesAccountNameValidation = BitsharesAccountNameValidation(this, etAccountName, uiValidatorListener)
        val onAccountExist = object : BitsharesAccountNameValidation.OnAccountExist {
            override fun onAccountExists() {
                runOnUiThread {
                    Toast.makeText(globalActivity, resources.getString(R.string.account_name_already_exist), Toast.LENGTH_LONG).show()
                }
            }

        }
        bitsharesAccountNameValidation.setOnAccountExist(onAccountExist)
        etAccountName?.setUiValidator(bitsharesAccountNameValidation)

        /*This button should not be enabled till all the fields be correctly filled*/
        disableCreate()

        /*
        * Set the focus on the fisrt field and show keyboard
        * */
        etAccountName?.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etPinConfirmation, InputMethodManager.SHOW_IMPLICIT)

        tvImportAccount.setOnClickListener(){

            var intent:Intent = Intent(activity,BrainkeyActivity::class.java)
            startActivity(intent)
        }

        etPin_.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                fieldsValidator.validate()

                /*
                 * Validate continue to create account
                 * */
                validateFieldsToContinue()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        etPinConfirmation.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                fieldsValidator.validate()

                /*
                 * Validate continue to create account
                 * */
                validateFieldsToContinue()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        etAccountName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                fieldsValidator.validate()

                /*
                 * Validate continue to create account
                 * */
                validateFieldsToContinue()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        btnCreate.setOnClickListener(){

            /*
        * Question if continue or not
        * */
            var questionDialog: QuestionDialog = QuestionDialog(activity as Activity)
            questionDialog.setText(R.string.continue_question)
            questionDialog.setOnNegative(object : NegativeResponse {
                override fun onNegative(dialogMaterial: DialogMaterial) {
                    dialogMaterial.dismiss()
                }
            })
            questionDialog.setOnPositive(object : PositiveResponse {
                override fun onPositive() {

                    // Make request to create a bitshare account
                    /*var accountName: String = etAccountName?.getText().toString().trim()
                    val request = ValidateCreateBitsharesAccountRequest(accountName, activity as Activity)

                    //DTVV: Friday 27 July 2018
                    //Makes dialog to tell the user that the account is been created
                    val creatingAccountMaterialDialog = CrystalDialog(activity as Activity)
                    creatingAccountMaterialDialog.setText(R.string.window_create_seed_DialogMessage)
                    creatingAccountMaterialDialog.progress()
                    this@AccountActivity.runOnUiThread {
                        creatingAccountMaterialDialog.show()
                    }
                    request.listener = (object : CryptoNetInfoRequestListener {
                        override fun onCarryOut() {
                            creatingAccountMaterialDialog.dismiss()
                            if (request.status == ValidateCreateBitsharesAccountRequest.StatusCode.SUCCEEDED) {
                                val accountSeed = request.account
                                val intent = Intent(activity as Activity, BackupSeedActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra("SEED_ID", accountSeed!!.id)
                                intent.putExtra("newAccount", true)
                                startActivity(intent)
                            } else if (request.status == ValidateCreateBitsharesAccountRequest.StatusCode.ACCOUNT_EXIST) {
                                Toast.makeText(
                                    activity,
                                    (activity as Activity).getString(R.string.Account_already_exists),
                                    Toast.LENGTH_LONG
                                ).show()
                                disableCreate()
                            } else {
                                //fieldsValidator.validate()
                            }
                        }
                    })
                    (object : Thread() {
                        override fun run() {

                            /*
                            *
                            * Run thread*/
                            CryptoNetInfoRequests.getInstance()!!.addRequest(request)
                        }
                    }).start()*/

                    RepositoryManager.getAccountsRepository(globalActivity).addAccount(1,"dtvv-123456","allow clutch exhibit group citizen poverty draw help wage mail program safe")

                    finish()

                    var intent:Intent = Intent(activity,CopyBrainkey::class.java)
                    intent.putExtra("newAccount",true)
                    startActivity(intent)
                }
            })
            questionDialog.show()
        }
    }


    private fun validateFieldsToContinue() {

        var result = false //Contains the final result

        val pinValid: Boolean? = this.etPin_?.fieldValidatorModel?.isValid
        val pinConfirmationValid = this.etPinConfirmation?.fieldValidatorModel?.isValid
        val pinAccountNameValid = this.etAccountName?.fieldValidatorModel?.isValid

        if (pinValid!! &&
            pinConfirmationValid!! &&
            pinAccountNameValid!!) {
            result = true //Validation is correct
        }


        /*
        * If the result is true so the user can continue to the creation of the account
        * */
        if (result) {

            enableCreate()

        } else {

            /*
            * Disaible button create
            * */
            disableCreate()
        }
    }

    /*
    * Enable create button
    * */
    private fun enableCreate() {
        runOnUiThread(Runnable {
            //btnCreate?.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            btnCreate?.setEnabled(true)
        })
    }

    /*
     * Disable create button
     * */
    private fun disableCreate() {
        runOnUiThread(Runnable {
            btnCreate?.setEnabled(false)
            //btnCreate?.setBackground(resources.getDrawable(R.drawable.disable_style))
        })
    }
}