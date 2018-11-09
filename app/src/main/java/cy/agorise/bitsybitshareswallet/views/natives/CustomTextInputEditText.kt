package cy.agorise.bitsybitshareswallet.views.natives

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.textfield.TextInputEditText
import cy.agorise.bitsybitshareswallet.interfaces.UIValidator
import cy.agorise.bitsybitshareswallet.models.FieldValidatorModel

class CustomTextInputEditText(context: Context, attrs: AttributeSet) : TextInputEditText(context, attrs), UIValidator {

    /*
    * Contains the field validator, this aid to validate the field
    * */
    var fieldValidatorModel = FieldValidatorModel()

    /*
    * Interface to validate the field
    * */
    private var uiValidator: UIValidator? = null

    /*
    * Contains the last input value
    * */
    var lastValue: String? = null
        private set


    init {

        /*
        * Set listener to get the last value of the control
        * */
        this.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(v: View, hasFocus: Boolean) {

                if (!hasFocus) {
                    lastValue = getText().toString()
                }
            }
        })
    }

    fun setUiValidator(uiValidator: UIValidator) {
        this.uiValidator = uiValidator
    }
    /*
     * End of setters and getters
     * */

    override fun validate() {
        uiValidator!!.validate()
    }
}
