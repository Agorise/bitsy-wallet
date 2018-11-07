package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.bitsybitshareswallet.models.AccountSeed

class CreateBitcoinAccountRequest(val accountSeed: AccountSeed, val context: Context, val accountCryptoNet: CryptoNet) :
    CryptoNetInfoRequest(CryptoCoin.BITSHARES) {

    // The state of this request
    var status = StatusCode.NOT_STARTED
        set(code) {
            field = code
            this.validate()
        }

    /**
     * The status code of this request
     */
    enum class StatusCode {
        NOT_STARTED,
        SUCCEEDED,
        ACCOUNT_EXIST
    }

    fun validate() {
        if (this.status != StatusCode.NOT_STARTED)
            this._fireOnCarryOutEvent()
    }
}
