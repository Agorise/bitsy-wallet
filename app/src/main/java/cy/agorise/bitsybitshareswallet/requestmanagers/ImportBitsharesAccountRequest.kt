package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.enums.SeedType

class ImportBitsharesAccountRequest : CryptoNetInfoRequest {

    /**
     * The mnemonic words
     */
    val mnemonic: String

    /**
     * If this seed is BIP39 or Brainkey
     */
    var seedType: SeedType? = null

    /**
     * The status of this request
     */
    var status = StatusCode.NOT_STARTED
        set(status) {
            field = status
            this._fireOnCarryOutEvent()
        }

    var context: Context? = null
        private set

    /**
     * The status code of this request
     */
    enum class StatusCode {
        NOT_STARTED,
        SUCCEEDED,
        NO_INTERNET,
        NO_SERVER_CONNECTION,
        ACCOUNT_DOESNT_EXIST,
        BAD_SEED,
        NO_ACCOUNT_DATA,
        PETITION_FAILED
    }

    constructor(mnemonic: String, context: Context) : super(CryptoCoin.BITSHARES) {
        this.mnemonic = mnemonic
        this.context = context
    }

    constructor(mnemonic: String, context: Context, addAccountIfValid: Boolean) : super(CryptoCoin.BITSHARES) {
        this.mnemonic = mnemonic
        this.context = context
    }

    fun validate() {
        if (this.status != StatusCode.NOT_STARTED) {
            this._fireOnCarryOutEvent()
        }
    }
}
