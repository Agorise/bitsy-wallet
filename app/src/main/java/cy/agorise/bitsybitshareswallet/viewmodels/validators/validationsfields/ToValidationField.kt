package cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields

import android.widget.EditText
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequestListener
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequests
import cy.agorise.bitsybitshareswallet.requestmanagers.ValidateExistBitsharesAccountRequest
import cy.agorise.bitsybitshareswallet.viewmodels.validators.ValidationField

class ToValidationField(private val fromField:String, private val toField: EditText) :
    ValidationField(toField) {

    override fun validate() {
        val fromNewValue: String = fromField
        val toNewValue = toField.text.toString()
        val mixedValue = fromNewValue + "_" + toNewValue
        this.lastValue = mixedValue
        this.startValidating()
        val field = this

        if (fromNewValue == toNewValue) {
            setMessageForValue(
                mixedValue,
                validator.context.getResources().getString(R.string.warning_msg_same_account)
            )
            setValidForValue(mixedValue, false)
        } else {

            val request = ValidateExistBitsharesAccountRequest(toNewValue)
            request.listener = object : CryptoNetInfoRequestListener {
                override fun onCarryOut() {
                    if (!request.getAccountExists()) {
                        setMessageForValue(
                            mixedValue, validator.context.getResources().getString(
                                R.string.account_name_not_exist,
                                "'$toNewValue'"
                            )
                        )
                        setValidForValue(mixedValue, false)
                    } else {
                        setValidForValue(mixedValue, true)
                    }
                }
            }
            CryptoNetInfoRequests.getInstance()!!.addRequest(request)
        }
    }
}
