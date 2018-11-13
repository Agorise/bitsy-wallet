package cy.agorise.bitsybitshareswallet.viewmodels.validators

import android.content.Context
import java.util.ArrayList

abstract class UIValidator(context: Context) {
    var context: Context
        protected set
    lateinit var listener: UIValidatorListener
    protected var validationFields: MutableList<ValidationField>

    val isValid: Boolean
        get() {
            for (i in this.validationFields.indices) {
                val nextField = this.validationFields[i]

                if (!nextField.getValid()) {
                    return false
                }
            }

            return true
        }

    init {
        this.context = context
        this.validationFields = ArrayList()
    }

    fun addField(newField: ValidationField) {
        this.validationFields.add(newField)
        newField.validator = this
    }

    fun validate() {
        for (i in this.validationFields.indices) {
            this.validationFields[i]!!.validate()
        }
    }

    fun validationFieldsCount(): Int {
        return this.validationFields.size
    }

    fun getValidationField(index: Int): ValidationField {
        return this.validationFields[index]
    }

    fun validationFailed(field: ValidationField) {
        this._fireOnValidationFailedEvent(field)
    }

    fun validationSucceeded(field: ValidationField) {
        this._fireOnValidationSucceededEvent(field)
    }

    fun _fireOnValidationFailedEvent(field: ValidationField) {
        this.listener.onValidationFailed(field)
    }

    fun _fireOnValidationSucceededEvent(field: ValidationField) {
        this.listener.onValidationSucceeded(field)
    }
}
