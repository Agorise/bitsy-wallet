package cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields

import android.widget.Spinner
import cy.agorise.bitsybitshareswallet.models.CryptoCurrency
import cy.agorise.bitsybitshareswallet.viewmodels.validators.ValidationField

class AssetValidationField(private val assetField: Spinner) : ValidationField(assetField) {

    override fun validate() {
        val cryptoCurrencySelected = this.assetField.selectedItem as CryptoCurrency
        if (cryptoCurrencySelected != null) {
            val newValue = "" + cryptoCurrencySelected!!.id
            this.lastValue = newValue
            setValidForValue(newValue, true)
        } else {
            val newValue = "" + -1
            setMessageForValue(newValue, "Select a currency")
            this.lastValue = newValue
            setValidForValue(newValue, false)
        }
    }
}
