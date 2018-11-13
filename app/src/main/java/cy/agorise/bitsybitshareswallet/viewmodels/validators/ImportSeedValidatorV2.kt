package cy.agorise.bitsybitshareswallet.viewmodels.validators

import android.content.Context
import android.widget.EditText
import cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields.PinConfirmationValidationField
import cy.agorise.bitsybitshareswallet.viewmodels.validators.validationsfields.PinValidationField

class ImportSeedValidatorV2(
    context: Context,
    pinEdit: EditText,
    pinConfirmationEdit: EditText
) : UIValidator(context) {

    init {
        this.addField(PinValidationField(pinEdit))
        this.addField(PinConfirmationValidationField(pinEdit, pinConfirmationEdit))
    }
}
