package cy.agorise.bitsybitshareswallet.viewmodels.validators

import android.content.Context
import android.widget.EditText
import android.widget.Spinner
import cy.agorise.bitsybitshareswallet.models.CryptoNetAccount
import cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields.AmountValidationField
import cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields.AssetValidationField
import cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields.MemoValidationField
import cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields.ToValidationField

class SendTransactionValidator(
    context: Context,
    val account: CryptoNetAccount,
    toEdit: EditText,
    fromEdit:String,
    amountEdit: EditText,
    memoEdit: EditText
) : UIValidator(context) {


    init {
        this.addField(ToValidationField(fromEdit, toEdit))
        //this.addField(AssetValidationField(assetSpinner))
        //this.addField(AmountValidationField(amountEdit, assetSpinner, this.account))
        this.addField(MemoValidationField(memoEdit))
    }
}
