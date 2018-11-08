package cy.agorise.bitsybitshareswallet.requestmanagers

import java.util.ArrayList

class FileServiceRequests
/**
 * Private constructor for singleton pattern
 */
private constructor() {

    private var requests: MutableList<FileServiceRequest>? = null
    private var listeners: MutableList<FileServiceRequestsListener>? = null

    fun addRequest(request: FileServiceRequest) {
        this.requests!!.add(request)

        this._fireNewRequestEvent(request)
    }

    fun removeRequest(request: FileServiceRequest) {
        this.requests!!.remove(request)
    }

    fun addListener(listener: FileServiceRequestsListener) {
        this.listeners!!.add(listener)
    }

    private fun _fireNewRequestEvent(request: FileServiceRequest) {
        for (i in this.listeners!!.indices) {
            this.listeners!![i].onNewRequest(request)
        }
    }

    companion object {
        private var instance: FileServiceRequests? = null

        /**
         * Gets an instance of this manager
         * @return the instance to manage the cryptonetinforequest
         */
        fun getInstance(): FileServiceRequests? {
            if (FileServiceRequests.instance == null) {
                FileServiceRequests.instance = FileServiceRequests()
                FileServiceRequests.instance!!.requests = ArrayList()
                FileServiceRequests.instance!!.listeners = ArrayList()
            }

            return FileServiceRequests.instance
        }
    }
}
