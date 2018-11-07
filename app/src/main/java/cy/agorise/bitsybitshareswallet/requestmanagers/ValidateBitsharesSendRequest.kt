package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.models.GrapheneAccount

class ValidateBitsharesSendRequest @JvmOverloads constructor(// The app context
    val context: Context, // The source account used to transfer fund from
    val sourceAccount: GrapheneAccount,
    // The destination account id
    val toAccount: String, // The amount of the transaction
    val amount: Long, // The asset id of the transaction
    val asset: String, // The memo, can be null
    val memo: String? = null
) : CryptoNetInfoRequest(CryptoCoin.BITSHARES) {
    // The state of this request
    var status = StatusCode.NOT_STARTED
        set(code) {
            field = code
            this._fireOnCarryOutEvent()
        }

    /**
     * The status code of this request
     */
    enum class StatusCode {
        NOT_STARTED,
        SUCCEEDED,
        NO_INTERNET,
        NO_SERVER_CONNECTION,
        NO_ASSET_INFO_DB,
        NO_ASSET_INFO,
        NO_TO_USER_INFO,
        PETITION_FAILED
    }

    fun validate() {
        if (this.status != StatusCode.NOT_STARTED) {
            this._fireOnCarryOutEvent()
        }
    }
}
