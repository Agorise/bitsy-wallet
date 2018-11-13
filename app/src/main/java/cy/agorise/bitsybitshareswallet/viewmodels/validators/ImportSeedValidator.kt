package cy.agorise.bitsybitshareswallet.viewmodels.validators

import android.content.Context
import android.widget.EditText
import cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields.PinConfirmationValidationField
import cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields.PinValidationField

class ImportSeedValidator(
    context: Context,
    pinEdit: EditText,
    pinConfirmationEdit: EditText,
    bitsharesAccountNameEdit: EditText,
    mnemonicEdit: EditText
) : UIValidator(context) {

    init {
        this.addField(PinValidationField(pinEdit))
        this.addField(PinConfirmationValidationField(pinEdit, pinConfirmationEdit))
        //this.addField(new BitsharesAccountNameValidationField(bitsharesAccountNameEdit));
        //this.addField(new BitsharesAccountMnemonicValidationField(mnemonicEdit,bitsharesAccountNameEdit));
    }
}
