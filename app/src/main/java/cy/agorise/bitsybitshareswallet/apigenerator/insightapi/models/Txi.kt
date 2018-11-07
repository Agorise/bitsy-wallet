package cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models

class Txi {
    /**
     * The id of this transaction
     */
    var txid: String? = null
    /**
     * the version number of this transaction
     */
    var version: Int = 0
    /**
     * Time to hold this transaction
     */
    var locktime: Long = 0
    /**
     * The array of the transaction inputs
     */
    var vin: Array<Vin>? = null
    /**
     * the array of the transactions outputs
     */
    var vout: Array<Vout>? = null
    /**
     * this block hash
     */
    var blockhash: String? = null
    /**
     * The blockheight where this transaction belongs, if 0 this transactions hasn't be included in any block yet
     */
    var blockheight: Int = 0
    /**
     * Number of confirmations
     */
    var confirmations: Int = 0
    /**
     * The time of the first broadcast fo this transaction
     */
    var time: Long = 0
    /**
     * The time which this transaction was included
     */
    var blocktime: Long = 0
    /**
     * Total value to transactions outputs
     */
    var valueOut: Double = 0.toDouble()
    /**
     * The size in bytes
     */
    var size: Int = 0
    /**
     * Total value of transactions inputs
     */
    var valueIn: Double = 0.toDouble()
    /**
     * Fee of this transaction has to be valueIn - valueOut
     */
    var fee: Double = 0.toDouble()
    /**
     * This is only for dash, is the instantsend state
     */
    var txlock = false

}
