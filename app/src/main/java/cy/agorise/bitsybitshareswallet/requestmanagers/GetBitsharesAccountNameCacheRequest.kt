package cy.agorise.bitsybitshareswallet.requestmanagers

import android.content.Context
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin

class GetBitsharesAccountNameCacheRequest(val context: Context, val accountId: String) :
    CryptoNetInfoRequest(CryptoCoin.BITSHARES) {
    private var accountName: String? = null

    init {
        this.accountName = ""
    }

    fun setAccountName(accountName: String) {
        this.accountName = accountName
        this.validate()
    }

    fun validate() {
        if (this.accountName != "") {
            this._fireOnCarryOutEvent()
        }
    }

    fun getAccountName(): String? {
        return accountName
    }
}
