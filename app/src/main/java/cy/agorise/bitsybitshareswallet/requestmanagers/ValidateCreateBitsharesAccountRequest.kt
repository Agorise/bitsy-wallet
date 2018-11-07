package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.models.GrapheneAccount

class ValidateCreateBitsharesAccountRequest(
    /**
     * The name of the account
     */
    var accountName: String?, val context: Context
) : CryptoNetInfoRequest(CryptoCoin.BITSHARES) {


    // The state of this request
    var status = StatusCode.NOT_STARTED
        set(code) {
            field = code
            this.validate()
        }

    var account: GrapheneAccount? = null
        set(account) {
            field = account
            this.validate()
        }

    /**
     * The status code of this request
     */
    enum class StatusCode {
        NOT_STARTED,
        SUCCEEDED,
        NO_INTERNET,
        NO_SERVER_CONNECTION,
        ACCOUNT_EXIST,
        NO_ACCOUNT_DATA

    }

    fun validate() {
        if (this.status != StatusCode.NOT_STARTED)
            this._fireOnCarryOutEvent()
    }
}
