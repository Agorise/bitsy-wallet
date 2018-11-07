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
        private var instance: CryptoNetEvents? = null

        /**
         * Gets an instance of this manager
         * @return the instance to manage the cryptonetinforequest
         */
        fun getInstance(): CryptoNetEvents? {
            if (CryptoNetEvents.instance == null) {
                CryptoNetEvents.instance = CryptoNetEvents()
                CryptoNetEvents.instance!!.listeners = ArrayList<CryptoNetEventsListener>()
            }

            return CryptoNetEvents.instance
        }
    }
}
