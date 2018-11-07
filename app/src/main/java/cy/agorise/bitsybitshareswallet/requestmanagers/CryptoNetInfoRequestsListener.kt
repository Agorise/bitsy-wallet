package cy.agorise.bitsybitshareswallet.requestmanagers

interface CryptoNetInfoRequestsListener {
    /**
     * A new request for the manager
     * @param request The request, we can query of the class of this object to know if the request is from a particular manager
     */
    fun onNewRequest(request: CryptoNetInfoRequest)
}
