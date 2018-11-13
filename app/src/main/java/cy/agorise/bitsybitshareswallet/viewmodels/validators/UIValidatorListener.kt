package cy.agorise.bitsybitshareswallet.viewmodels.validators

interface UIValidatorListener {

    fun onValidationSucceeded(field: ValidationField)
    fun onValidationFailed(field: ValidationField)
}