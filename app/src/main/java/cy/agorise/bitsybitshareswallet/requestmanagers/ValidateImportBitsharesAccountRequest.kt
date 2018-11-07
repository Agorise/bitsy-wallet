package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.enums.SeedType

class ValidateImportBitsharesAccountRequest : CryptoNetInfoRequest {

    /**
     * The name of the account
     */
    val accountName: String

    /**
     * The mnemonic words
     */
    val mnemonic: String

    /**
     * True - the account must be added if the accountName and mnemonic are correct
     */
    private var addAccountIfValid = false

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

    constructor(accountName: String, mnemonic: String, context: Context) : super(CryptoCoin.BITSHARES) {
        this.accountName = accountName
        this.mnemonic = mnemonic
        this.context = context
    }

    constructor(
        accountName: String,
        mnemonic: String,
        context: Context,
        addAccountIfValid: Boolean
    ) : super(CryptoCoin.BITSHARES) {
        this.accountName = accountName
        this.mnemonic = mnemonic
        this.addAccountIfValid = addAccountIfValid
        this.context = context
    }

    fun validate() {
        if (this.status != StatusCode.NOT_STARTED) {
            this._fireOnCarryOutEvent()
        }
    }

    fun addAccountIfValid(): Boolean {
        return this.addAccountIfValid
    }
}
