package cy.agorise.bitsybitshareswallet.viewmodels.validators

import android.view.View

abstract class ValidationField(var view: View) {

    protected lateinit var lastValue: String
    lateinit var message: String
    protected var validating: Boolean = false
    protected var valid: Boolean? = null
    lateinit var validator: UIValidator


    fun ValidationField(view: View) {
        this.lastValue = ""
        this.message = ""
        this.validating = false
        this.valid = null
        this.view = view
    }


    fun startValidating() {
        this.valid = null
        this.validating = true
    }

    abstract fun validate()

    fun stopValidating() {
        this.validating = false
    }

    fun setValidForValue(value: String, isValid: Boolean) {
        if (this.lastValue == value) {
            this.validating = false
            this.valid = isValid

            if (isValid) {
                validator.validationSucceeded(this)
            } else {
                validator.validationFailed(this)
            }
        }
    }

    fun setMessageForValue(value: String, message: String) {
        if (this.lastValue == value) {
            this.message = message
        }
    }

    fun getValid(): Boolean {
        return if (this.valid != null) this.valid!! else false
    }
}
