package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/*@Entity(
    tableName = "bitcoin_transaction_gt_io",
    primaryKeys = { "bitcoin_transaction_id", "io_index", "is_output" },
    foreignKeys = {
        @ForeignKey(
            entity = BitcoinTransaction.class,
                    parentColumns = " crypto_coin_transaction_id",
        childColumns = "bitcoin_transaction_id",
        onDelete = ForeignKey.CASCADE
        )
    })*/
class BitcoinTransactionGTxIO {

    /**
     * The id of the bitcoin transaction
     */
    @ColumnInfo(name = "bitcoin_transaction_id")
    var bitcoinTransactionId: Long = 0

    /**
     * The index in the transaction
     */
    @ColumnInfo(name = "io_index")
    var index: Int = 0

    /**
     * The address of the input or output
     */
    @ColumnInfo(name = "address")
    lateinit var address: String

    /**
     * determines if this is an input or output
     */
    @ColumnInfo(name = "is_output")
    var isOutput: Boolean = false

    @ColumnInfo(name = "amount")
    var amount: Long = 0

    @ColumnInfo(name = "script_hex")
    lateinit var scriptHex: String

    @ColumnInfo(name = "original_txid")
    lateinit var originalTxId: String

    constructor() {}

    constructor(bitcoinTransactionId: Long, index: Int, address: String, isOutput: Boolean) {
        this.bitcoinTransactionId = bitcoinTransactionId
        this.index = index
        this.address = address
        this.isOutput = isOutput
    }
}
