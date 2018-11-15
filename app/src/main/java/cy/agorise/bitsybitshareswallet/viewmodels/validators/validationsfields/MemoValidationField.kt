package cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields

import android.widget.EditText
import cy.agorise.bitsybitshareswallet.viewmodels.validators.ValidationField

class MemoValidationField(private val memoField: EditText) : ValidationField(memoField) {

    override fun validate() {
        val memoNewValue = memoField.text.toString()
        this.lastValue = memoNewValue
        setValidForValue(memoNewValue, true)
        validator.validationSucceeded(this)
    }
}