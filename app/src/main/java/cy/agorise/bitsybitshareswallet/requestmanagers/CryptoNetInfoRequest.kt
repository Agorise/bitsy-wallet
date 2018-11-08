package cy.agorise.bitsybitshareswallet.requestmanagers

import cy.agorise.bitsybitshareswallet.enums.CryptoCoin

abstract class CryptoNetInfoRequest protected constructor(coin: CryptoCoin) {
    /**
     * The cryptocoin this request belongs
     */
    var coin: CryptoCoin
        protected set
    /**
     * The listener for the answer of this petition
     */
    var listener: CryptoNetInfoRequestListener? = null

    init {
        this.coin = coin
    }

    protected fun _fireOnCarryOutEvent() {
        if (listener != null) {
            listener!!.onCarryOut()
        }
        //CryptoNetInfoRequests.getInstance_()!!.removeRequest(this)
    }
}
