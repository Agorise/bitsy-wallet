package cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models

class ScriptPubKey {
    /**
     * The code to validate in hex
     */
    var hex: String? = null
    /**
     * the code to validate this transaction
     */
    var asm: String? = null
    /**
     * the acoin address involved
     */
    var addresses: Array<String>? = null
    /**
     * The type of the hash
     */
    var type: String? = null
}
