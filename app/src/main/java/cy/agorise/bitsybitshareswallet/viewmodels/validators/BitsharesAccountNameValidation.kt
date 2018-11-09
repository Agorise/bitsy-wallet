package cy.agorise.bitsybitshareswallet.viewmodels.validators

import android.app.Activity
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.interfaces.UIValidator
import cy.agorise.bitsybitshareswallet.interfaces.UIValidatorListener
import cy.agorise.bitsybitshareswallet.views.natives.CustomTextInputEditText

class BitsharesAccountNameValidation : CustomValidationField, UIValidator {

    /*
    * Contains the field to validate
    * */
    private val accountNameField: CustomTextInputEditText

    /*
     * Interface to validate when an account exist an take over control it
     * */
    private var onAccountExist: OnAccountExist? = null


    constructor (activity: Activity,
                 accountNameField: CustomTextInputEditText,
                 uiValidatorListener: UIValidatorListener
    ) : super(activity) {

        this.accountNameField = accountNameField
        this.uiValidatorListener = uiValidatorListener

        /*
        * The current view for errors will be this
        * */
        this.currentView = this.accountNameField
    }

    override fun validate() {

        val newValue = accountNameField.text.toString()

        /*
        Contains the validation result
        */
        var result = true

        /*
         * Validate empty field
         * */
        if (newValue == "") {

            /*
            * Validation not passed
            * */
            result = false
            accountNameField.fieldValidatorModel.setInvalid()
            accountNameField.fieldValidatorModel.message = this.accountNameField.resources.getString(R.string.create_account_window_err_account_empty)
        } else {

            /*
            * Remove error
            * */
            accountNameField.error = null
            accountNameField.fieldValidatorModel.setValid()

            /*
                Validate at least min length
            */
            if (newValue.length < 10) {

                /*
                 * Validation not passed
                 * */
                result = false
                accountNameField.fieldValidatorModel.setInvalid()
                accountNameField.fieldValidatorModel.message = this.accountNameField.resources.getString(R.string.create_account_window_err_min_account_name_len)
            } else {

                /*
                 * Remove error
                 * */
                accountNameField.error = null
                accountNameField.fieldValidatorModel.setValid()

                /*
                    Validate at least one character
                */
                if (!newValue.matches(".*[a-zA-Z]+.*".toRegex())) {

                    /*
                     * Validation not passed
                     * */
                    result = false
                    accountNameField.fieldValidatorModel.setInvalid()
                    accountNameField.fieldValidatorModel.message = this.accountNameField.resources.getString(R.string.create_account_window_err_at_least_one_character)

                } else {

                    /*
                     * Remove error
                     * */
                    accountNameField.error = null
                    accountNameField.fieldValidatorModel.setValid()

                    /*
                        Validate at least one number for the account string
                    */
                    if (!newValue.matches(".*\\d+.*".toRegex())) {

                        /*
                         * Validation not passed
                         * */
                        result = false
                        accountNameField.fieldValidatorModel.setInvalid()
                        accountNameField.fieldValidatorModel.message = this.accountNameField.resources.getString(R.string.create_account_window_err_at_least_one_number)

                    } else {

                        /*
                         * Remove error
                         * */
                        accountNameField.error = null
                        accountNameField.fieldValidatorModel.setValid()


                        /*
                            Validate at least one middle script
                        */
                        if (!newValue.contains("-")) {

                            /*
                             * Validation not passed
                             * */
                            result = false
                            accountNameField.fieldValidatorModel.setInvalid()
                            accountNameField.fieldValidatorModel.message = this.accountNameField.resources.getString(R.string.create_account_window_err_at_least_one_script)

                        } else {

                            /*
                             * Remove error
                             * */
                            accountNameField.error = null
                            accountNameField.fieldValidatorModel.setValid()
                        }
                    }
                }
            }
        }

        /*
        * If passed first validations
        * */
        if (!result) {

            /*
             * Deliver result
             * */
            if (uiValidatorListener != null) {
                uiValidatorListener.onValidationFailed(this)
            }
        } else {

            /*
             * Deliver result
             * */
            if (uiValidatorListener != null) {
                uiValidatorListener.onValidationSucceeded(this)
            }
        }
        /*
        * Passed initial validations, next final validations
        * */
    }

    /*
    * Setters and getters
    * */
    fun setOnAccountExist(onAccountExist: OnAccountExist) {
        this.onAccountExist = onAccountExist
    }
    /*
     * End of setters and getters
     * */

    /*
    * Interface to validate when an account exist an take over control it
    * */
    open interface OnAccountExist {
        fun onAccountExists()
    }
}