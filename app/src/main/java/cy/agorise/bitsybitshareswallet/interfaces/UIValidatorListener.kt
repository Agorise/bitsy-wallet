package cy.agorise.bitsybitshareswallet.interfaces

import cy.agorise.bitsybitshareswallet.viewmodels.validators.CustomValidationField

interface UIValidatorListener {

    fun onValidationFailed(customValidationField: CustomValidationField)
    fun onValidationSucceeded(customValidationField: CustomValidationField)
}