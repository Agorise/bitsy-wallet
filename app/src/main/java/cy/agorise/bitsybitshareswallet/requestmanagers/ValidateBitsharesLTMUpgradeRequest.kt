package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.models.GrapheneAccount

class ValidateBitsharesLTMUpgradeRequest(// The app context
    val context: Context, // The source account used to transfer fund from
    val sourceAccount: GrapheneAccount
) : CryptoNetInfoRequest(CryptoCoin.BITSHARES) {
    // The state of this request
    var status = StatusCode.NOT_STARTED
        set(code) {
            field = code
            this.validate()
        }

    //TODO change asset
    val idAsset = "1.3.0" //default to bTS

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
        NO_FUNDS,
        PETITION_FAILED
    }

    fun validate() {
        if (this.status != StatusCode.NOT_STARTED) {
            this._fireOnCarryOutEvent()
        }
    }
}
