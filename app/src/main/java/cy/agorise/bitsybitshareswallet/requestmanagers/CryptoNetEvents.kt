package cy.agorise.bitsybitshareswallet.requestmanagers

import java.util.ArrayList

class CryptoNetEvents {
    private var listeners: MutableList<CryptoNetEventsListener>? = null

    /**
     * Private constructor for singleton pattern
     */
    private fun CryptoNetEvents() {}

    fun fireEvent(event: CryptoNetEvent) {
        for (i in this.listeners!!.indices) {
            this.listeners!![i].onCryptoNetEvent(event)
        }
    }

    fun removeListener(listener: CryptoNetEventsListener) {
        this.listeners!!.remove(listener)
    }

    fun addListener(listener: CryptoNetEventsListener) {
        this.listeners!!.add(listener)
    }

    companion object {
        var instance_: CryptoNetEvents? = null

        /**
         * Gets an instance_ of this manager
         * @return the instance_ to manage the cryptonetinforequest
         */
        fun getInstance(): CryptoNetEvents? {
            if (CryptoNetEvents.instance_ == null) {
                CryptoNetEvents.instance_ = CryptoNetEvents()
                CryptoNetEvents.instance_!!.listeners = ArrayList<CryptoNetEventsListener>()
            }

            return CryptoNetEvents.instance_
        }
    }
}
