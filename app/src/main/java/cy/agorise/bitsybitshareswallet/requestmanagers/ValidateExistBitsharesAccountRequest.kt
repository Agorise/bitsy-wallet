package cy.agorise.bitsybitshareswallet.requestmanagers

import cy.agorise.bitsybitshareswallet.enums.CryptoCoin

class ValidateExistBitsharesAccountRequest(// The account name to validate
    val accountName: String
) : CryptoNetInfoRequest(CryptoCoin.BITSHARES) {
    // The result of the validation, or null if there isn't a response
    private var accountExists: Boolean? = null

    fun getAccountExists(): Boolean {
        return this.accountExists!!
    }

    fun setAccountExists(value: Boolean) {
        this.accountExists = value
        this.validate()
    }

    fun validate() {
        if (this.accountExists != null) {
            this._fireOnCarryOutEvent()
        }
    }

}
