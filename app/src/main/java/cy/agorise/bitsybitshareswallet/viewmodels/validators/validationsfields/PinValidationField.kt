package cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields

import android.widget.EditText
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.viewmodels.validators.ValidationField

class PinValidationField(private val pinField: EditText) : ValidationField(pinField) {

    override fun validate() {
        val newValue = pinField.text.toString()
        if (newValue != "") {
            if (newValue != this.lastValue) {
                this.lastValue = newValue
                this.startValidating()

                if (newValue.length < 6) {
                    this.setMessageForValue(
                        newValue,
                        this.validator.context.getResources().getString(R.string.pin_number_warning)
                    )
                    this.setValidForValue(newValue, false)
                } else {
                    this.setValidForValue(newValue, true)
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
