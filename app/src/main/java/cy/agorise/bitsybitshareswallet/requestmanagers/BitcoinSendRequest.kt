package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.models.CryptoNetAccount
import cy.agorise.bitsybitshareswallet.models.GrapheneAccount

class BitcoinSendRequest(// The app context
    val context: Context, // The source account used to transfer fund from
    val sourceAccount: CryptoNetAccount,
    // The destination account id
    val toAccount: String, // The amount of the transaction
    val amount: Long, // The asset id of the transaction
    val cryptoCoin: CryptoCoin, // The memo, can be null
    val memo: String?
) : CryptoNetInfoRequest(cryptoCoin) {
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
        NO_BALANCE,
        NO_FEE,
        PETITION_FAILED
    }

    constructor(
        context: Context, sourceAccount: GrapheneAccount,
        toAccount: String, amount: Long, cryptoCoin: CryptoCoin
    ) : this(context, sourceAccount, toAccount, amount, cryptoCoin, null) {
    }

    fun validate() {
        if (this.status != StatusCode.NOT_STARTED) {
            this._fireOnCarryOutEvent()
        }
    }
}
