package cy.agorise.bitsybitshareswallet.models

import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import java.util.*

class GeneralTransaction {
    /**
     * The id on the database
     */
    var id: Long = -1
    /**
     * The Tx id of this transaciton
     */
    var txid: String? = null
    /**
     * the type of crypto coin fo this transaction
     */
    var type: CryptoCoin? = null
    /**
     * If this is confirmed, the block where it belongs, 0 means this hasn't be included in any block
     */
    var block: Long = 0
    /**
     * The amount of fee of this transaction
     */
    var fee: Long = 0
    /**
     * the number of confirmations of this transacion, 0 means it hasn't been included in any block
     */
    var confirm: Int = 0
    /**
     * The date of this transaction first broadcast
     */
    var date: Date? = null
    /**
     * The height of this transaction on the block
     */
    var blockHeight: Int = 0
    /**
     * The memo of this transaciton
     */
    var memo: String? = null
    /**
     * The account that this transaction belong as input or output.
     */
    var account: GeneralCoinAccount? = null
    /**
     * The inputs of this transactions
     */
    var txInputs: List<GTxIO> = ArrayList()
    /**
     * the outputs of this transasctions
     */
    var txOutputs: List<GTxIO> = ArrayList()


    /**
     * Returns how this transaction changes the balance of the account
     * @return The amount of balance this transasciton adds to the total balance of the account
     */
    val accountBalanceChange: Double
        get() {
            var balance = 0.0
            var theresAccountInput = false

            for (txInputs in this.txInputs) {
                if (txInputs.isOut && txInputs.address != null) {
                    balance += -txInputs.amount
                    theresAccountInput = true
                }
            }

            for (txOutput in this.txOutputs) {
                if (!txOutput.isOut && txOutput.address != null) {
                    balance += txOutput.amount
                }
            }

            if (theresAccountInput) {
                balance += (-this.fee).toDouble()
            }

            return balance
        }

    /**
     * empty constructor
     */
    constructor() {}

    /**
     * Constructor form the database
     * @param id the id on the database
     * @param txid the txid of this transaction
     * @param type The cryptocoin type
     * @param block The block where this transaction is, 0 means this hasn't be confirmed
     * @param fee the fee of this transaction
     * @param confirm the number of confirmations of this transasciton
     * @param date the date of this transaction
     * @param blockHeight the height on the block where this transasciton is
     * @param memo the memo of this transaction
     * @param account The account to this transaction belongs, as input or output
     */
    constructor(
        id: Long,
        txid: String,
        type: CryptoCoin,
        block: Long,
        fee: Long,
        confirm: Int,
        date: Date,
        blockHeight: Int,
        memo: String,
        account: GeneralCoinAccount
    ) {
        this.id = id
        this.txid = txid
        this.type = type
        this.block = block
        this.fee = fee
        this.confirm = confirm
        this.date = date
        this.blockHeight = blockHeight
        this.memo = memo
        this.account = account
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as GeneralTransaction?

        return if (if (txid != null) txid != that!!.txid else that!!.txid != null) false else type === that.type

    }

    override fun hashCode(): Int {
        var result = if (txid != null) txid!!.hashCode() else 0
        result = 31 * result + type!!.hashCode()
        return result
    }
}
