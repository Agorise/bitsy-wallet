package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.BitcoinTransaction
import cy.agorise.bitsybitshareswallet.models.BitcoinTransactionExtended
import cy.agorise.bitsybitshareswallet.models.BitcoinTransactionGTxIO

@Dao
interface BitcoinTransactionDao {

    @get:Query("SELECT * FROM crypto_coin_transaction cct, bitcoin_transaction bt WHERE bt.crypto_coin_transaction_id = cct.id")
    val all: LiveData<BitcoinTransactionExtended>

    @Query("SELECT * FROM bitcoin_transaction bt WHERE bt.tx_id = :txid")
    fun getTransactionsByTxid(txid: String): List<BitcoinTransaction>

    @Query("SELECT * FROM bitcoin_transaction bt WHERE bt.crypto_coin_transaction_id = :idCryptoCoinTransaction")
    fun getBitcoinTransactionByCryptoCoinTransaction(idCryptoCoinTransaction: Long): BitcoinTransaction

    @Query("SELECT * FROM bitcoin_transaction_gt_io bt WHERE bt.bitcoin_transaction_id= :idBitcoinTransaction")
    fun getGtxIOByTransaction(idBitcoinTransaction: Long): List<BitcoinTransactionGTxIO>

    @Query("SELECT * FROM bitcoin_transaction_gt_io bt WHERE bt.address= :address")
    fun getGtxIOByAddress(address: String): List<BitcoinTransactionGTxIO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBitcoinTransaction(vararg transactions: BitcoinTransaction): LongArray

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBitcoinTransactionGTxIO(vararg transactiongtxios: BitcoinTransactionGTxIO): LongArray
}
