package cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields

import android.widget.EditText
import android.widget.Spinner
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.models.CryptoCurrency
import cy.agorise.bitsybitshareswallet.models.CryptoNetAccount
import cy.agorise.bitsybitshareswallet.viewmodels.validators.ValidationField

class AmountValidationField(
    private val amountField: EditText,
    private val assetSpinner: Spinner,
    private val account: CryptoNetAccount
) : ValidationField(amountField) {

    override fun validate() {
        try {
            val newAmountValue = java.lang.Float.parseFloat(amountField.text.toString())
            val cryptoCurrency = assetSpinner.selectedItem as CryptoCurrency

            /*
            * Validation for the money
            * */
            if (cryptoCurrency == null) {
                setMessageForValue(
                    "",
                    amountField.context.getString(R.string.send_assets_error_invalid_cypto_coin_selected)
                )
                setValidForValue("", false)
                return
            }

            val idCurrency = if (cryptoCurrency == null) "null " else java.lang.Long.toString(cryptoCurrency!!.id)
            val mixedValues = newAmountValue.toString() + "_" + idCurrency
            this.lastValue = mixedValues
            this.startValidating()
            val field = this

            val balance = BitsyDatabase.getAppDatabase(amountField.context)!!.cryptoCoinBalanceDao()
                .getBalanceFromAccount(this.account.id, cryptoCurrency!!.id)

            var balanceDouble = 0.0
            if (balance != null) {
                balanceDouble = balance!!.balance!!.toDouble()
            }

            if (newAmountValue > balanceDouble) {
                setMessageForValue(
                    mixedValues,
                    validator.context.getResources().getString(R.string.insufficient_amount)
                )
                setValidForValue(mixedValues, false)
            } else if (newAmountValue == 0f) {
                setMessageForValue(
                    mixedValues,
                    validator.context.getResources().getString(R.string.amount_should_be_greater_than_zero)
                )
                setValidForValue(mixedValues, false)
            } else {
                setValidForValue(mixedValues, true)
            }
        } catch (e: NumberFormatException) {
            lastValue = ""
            setMessageForValue("", validator.context.getResources().getString(R.string.please_enter_valid_amount))
            setValidForValue("", false)
        }

    }
}
