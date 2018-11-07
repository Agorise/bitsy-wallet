package cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models

class Vin {
    /**
     * The original transaction id where this transaction is an output
     */
    var txid: String? = null
    /**
     *
     */
    var vout: Int = 0
    /**
     * Sequence fo the transaction
     */
    var sequence: Long = 0
    /**
     * Order of the transasction input on the transasction
     */
    var n: Int = 0
    /**
     * The script signature
     */
    var scriptSig: ScriptSig? = null
    /**
     * The addr of this transaction
     */
    var addr: String? = null
    /**
     * Value in satoshi
     */
    var valueSat: Long = 0
    /**
     * Calue of this transaction
     */
    var value: Double = 0.toDouble()
    /**
     *
     */
    var doubleSpentTxID: String? = null
}
