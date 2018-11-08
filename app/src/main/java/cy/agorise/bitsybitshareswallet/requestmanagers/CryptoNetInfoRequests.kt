package cy.agorise.bitsybitshareswallet.requestmanagers

import java.util.ArrayList

class CryptoNetInfoRequests {
    private var requests: MutableList<CryptoNetInfoRequest>? = null
    private var listeners: MutableList<CryptoNetInfoRequestsListener>? = null

    /**
     * Private constructor for singleton pattern
     */
    private fun CryptoNetInfoRequests() {}

    fun addRequest(request: CryptoNetInfoRequest) {
        this.requests!!.add(request)

        this._fireNewRequestEvent(request)
    }

    fun removeRequest(request: CryptoNetInfoRequest) {
        this.requests!!.remove(request)
    }

    fun addListener(listener: CryptoNetInfoRequestsListener) {
        this.listeners!!.add(listener)
    }

    private fun _fireNewRequestEvent(request: CryptoNetInfoRequest) {
        for (i in this.listeners!!.indices) {
            this.listeners!![i].onNewRequest(request)
        }
    }

    companion object {
        var instance: CryptoNetInfoRequests? = null

        /**
         * Gets an instance of this manager
         * @return the instance to manage the cryptonetinforequest
         */
        fun getInstance(): CryptoNetInfoRequests? {
            if (CryptoNetInfoRequests.instance == null) {
                CryptoNetInfoRequests.instance = CryptoNetInfoRequests()
                CryptoNetInfoRequests.instance!!.requests = ArrayList()
                CryptoNetInfoRequests.instance!!.listeners = ArrayList()
            }

            return CryptoNetInfoRequests.instance
        }
    }
}
