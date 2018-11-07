package cy.agorise.bitsybitshareswallet.models

import cy.agorise.bitsybitshareswallet.enums.CryptoCoin

class GTxIO {
    /**
     * The id on the database
     */
    var id: Long = -1
    /**
     * The Coin type of this transaction
     */
    var type: CryptoCoin? = null
    /**
     * The index on the transaction Input/Output
     */
    var index: Int = 0
    /**
     * The address that this transaction Input/Output belongs
     */
    var address: GeneralCoinAddress? = null
    /**
     * The transaction that this Input/Output belongs
     */
    var transaction: GeneralTransaction? = null
    /**
     * The amount
     */
    var amount: Long = 0
    /**
     * If this transaction is output or input
     */
    var isOut: Boolean = false
    /**
     * The address of this transaction as String
     */
    var addressString: String? = null
    /**
     * The Script as Hex
     */
    var scriptHex: String? = null
    /**
     * If this is a transaction output, the original transaction where this is input
     */
    var originalTxid: String? = null

    /**
     * Empty Constructor
     */
    constructor() {

    }

    /**
     * General Constructor, used by the DB.
     *
     * @param id The id in the dataabase
     * @param type The coin mType
     * @param address The addres fo an account on the wallet, or null if the address is external
     * @param transaction The transaction where this belongs
     * @param amount The amount with the lowest precision
     * @param isOut if this is an output
     * @param addressString The string of the General Coin address, this can't be null
     * @param index The index on the transaction
     * @param scriptHex The script in hex String
     */
    constructor(
        id: Long,
        type: CryptoCoin,
        address: GeneralCoinAddress,
        transaction: GeneralTransaction,
        amount: Long,
        isOut: Boolean,
        addressString: String,
        index: Int,
        scriptHex: String
    ) {
        this.id = id
        this.type = type
        this.address = address
        this.transaction = transaction
        this.amount = amount
        this.isOut = isOut
        this.addressString = addressString
        this.index = index
        this.scriptHex = scriptHex
    }
}
