package cy.agorise.bitsybitshareswallet.viewmodels.validators

import android.app.Activity
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.interfaces.UIValidator
import cy.agorise.bitsybitshareswallet.interfaces.UIValidatorListener
import cy.agorise.bitsybitshareswallet.views.natives.CustomTextInputEditText

class PinDoubleConfirmationValidationField : CustomValidationField, UIValidator {

    /*
    * Contain the fields to validate
    * */
    private val pinField: CustomTextInputEditText
    private val pinConfirmationField: CustomTextInputEditText


    constructor(activity: Activity,
                pinField: CustomTextInputEditText,
                pinConfirmationField: CustomTextInputEditText,
                pinDoubleConfirmationInterface: UIValidatorListener
    ) : super(activity) {

        this.pinField = pinField
        this.pinConfirmationField = pinConfirmationField
        this.uiValidatorListener = pinDoubleConfirmationInterface
    }

    override fun validate() {

        val pin = pinField.text.toString().trim { it <= ' ' }
        val pinConfirmation = pinConfirmationField.text.toString().trim { it <= ' ' }

        /*
               * Contains the result for the validations
               * */
        var result = true

        /*
            Check if the two fields are equals theme selfs
        * */
        if (pin.length < 5) {

            /*
             *
             * False validation
             * */
            pinField.fieldValidatorModel.message = this.pinField.context.resources.getString(R.string.create_account_window_err_at_least_pin_characters)
            result = false

            /*
             * The current view for error
             * */
            this.currentView = pinField
        } else {

            /*Remove the error*/
            pinField.error = null

            /*
            * Same validation for PIN Confirmation
            * */
            if (pinConfirmation.length < 5) {

                /*
                 *
                 * False validation
                 * */
                pinConfirmationField.fieldValidatorModel.message = this.pinField.context.resources.getString(R.string.create_account_window_err_at_least_pin_characters)
                result = false

                /*
                 * The current view for error
                 * */
                this.currentView = pinConfirmationField
            } else {

                /*Remove the error*/
                pinField.error = null

                /*
                * Final validation, check if the PINs are equals
                *
                * */
                if (!pin.isEmpty() && !pinConfirmation.isEmpty() && pinConfirmation.compareTo(pin) != 0) {

                    /*
                     *
                     * False validation
                     * */
                    pinField.fieldValidatorModel.message = this.pinField.context.resources.getString(R.string.mismatch_pin)
                    pinConfirmationField.fieldValidatorModel.message = this.pinField.context.resources.getString(R.string.mismatch_pin)
                    result = false

                    /*
                     * The current view for error
                     * */
                    this.currentView = pinConfirmationField
                }
            }
        }

        /*
        * Passed validations
        * */
        if (result) {

            pinField.fieldValidatorModel.setValid()
            pinConfirmationField.fieldValidatorModel.setValid()

            /*
             * Connect response to controller
             * */
            if (uiValidatorListener != null) {
                uiValidatorListener.onValidationSucceeded(this)
            }
        } else {

            /*
             * Connect response to controller
             * */
            if (uiValidatorListener != null) {
                uiValidatorListener.onValidationFailed(this)
            }
        }/*
        * Not passed validations
        * */
    }
}