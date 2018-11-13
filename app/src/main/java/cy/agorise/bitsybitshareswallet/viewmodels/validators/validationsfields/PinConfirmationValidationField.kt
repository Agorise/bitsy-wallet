package cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields

import android.widget.EditText
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.viewmodels.validators.ValidationField

class PinConfirmationValidationField(private val pinField: EditText, private val pinConfirmationField: EditText) :
    ValidationField(pinConfirmationField) {

    override fun validate() {
        val newConfirmationValue = pinConfirmationField.text.toString()
        val newValue = pinField.text.toString()
        val mixedValue = newValue + "_" + newConfirmationValue

        if (newConfirmationValue != "") {
            if (mixedValue != this.lastValue) {
                this.lastValue = mixedValue
                this.startValidating()
                if (newConfirmationValue != newValue) {
                    this.setMessageForValue(
                        mixedValue,
                        this.validator.context.getResources().getString(R.string.mismatch_pin)
                    )
                    this.setValidForValue(mixedValue, false)
                } else {
                    this.setValidForValue(mixedValue, true)
                }
            }
        } else {
            this.lastValue = ""
            this.startValidating()
            this.setMessageForValue("", "")
            this.setValidForValue("", false)
        }
    }
}
