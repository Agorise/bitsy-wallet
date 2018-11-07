package cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models

class Vout {
    /**
     * The amount of coin
     */
    var value: Double = 0.toDouble()
    /**
     * the order of this transaciton output on the transaction
     */
    var n: Int = 0
    /**
     * The script public key
     */
    var scriptPubKey: ScriptPubKey? = null
    /**
     * If this transaciton output was spent what txid it belongs
     */
    var spentTxId: String? = null
    /**
     * The index on the transaction that this transaction was spent
     */
    var spentIndex: String? = null
    /**
     * The block height of the transaction this output was spent
     */
    var spentHeight: String? = null
}
