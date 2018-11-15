package cy.agorise.bitsybitshareswallet.manager

import android.content.Context
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.models.CryptoNetAccount
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequest

class NextBitcoinAccountAddressRequest(
    val account: CryptoNetAccount,
    val cryptoCoin: CryptoCoin,
    val context: Context
) : CryptoNetInfoRequest(cryptoCoin) {

    var address: String? = null

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
        SUCCEEDED
    }

    fun validate() {
        if (this.status != StatusCode.NOT_STARTED)
            this._fireOnCarryOutEvent()
    }
}
