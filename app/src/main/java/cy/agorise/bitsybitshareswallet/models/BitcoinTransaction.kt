package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/*@Entity(tableName = "bitcoin_transaction", primaryKeys = { "crypto_coin_transaction_id" }, foreignKeys = {
    @ForeignKey(
        entity = CryptoCoinTransaction.class,
                parentColumns = " id",
    childColumns = "crypto_coin_transaction_id",
    onDelete = ForeignKey.CASCADE
    )
})*/
class BitcoinTransaction {

    /**
     * The id of the base transaction
     */
    @ColumnInfo(name = "crypto_coin_transaction_id")
    var cryptoCoinTransactionId: Long = 0


    /**
     * The transaction id in the blockchain
     */
    @ColumnInfo(name = "tx_id")
    @NonNull
    lateinit var txId: String

    /**
     * The block id in the blockchain
     */
    @ColumnInfo(name = "block")
    var block: Long = 0

    /**
     * The fee of the transaction
     */
    @ColumnInfo(name = "fee")
    var fee: Long = 0
    /**
     * The confirmations of the transaction in the blockchain
     */
    @ColumnInfo(name = "confirmations")
    var confirmations: Int = 0

    constructor() {}

    constructor(cryptoCoinTransactionId: Long, txId: String, block: Long, fee: Long, confirmations: Int) {
        this.cryptoCoinTransactionId = cryptoCoinTransactionId
        this.txId = txId
        this.block = block
        this.fee = fee
        this.confirmations = confirmations
    }
}
