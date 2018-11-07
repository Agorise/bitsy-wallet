package cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models

class AddressTxi {
    /**
     * The total number of items
     */
    var totalItems: Int = 0
    /**
     * The start index of the current txi
     */
    var from: Int = 0
    /**
     * the last index of the current txi
     */
    var to: Int = 0
    /**
     * The arrays of txi of this response
     */
    var items: Array<Txi>? = null

}
