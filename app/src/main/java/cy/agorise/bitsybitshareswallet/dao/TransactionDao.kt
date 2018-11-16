package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.CryptoCoinTransaction
import cy.agorise.bitsybitshareswallet.models.CryptoCoinTransactionExtended
import java.util.*


@Dao
interface TransactionDao {

    @get:Query("SELECT * FROM crypto_coin_transaction")
    val all: LiveData<List<CryptoCoinTransaction>>

    @Query("$transactionsQuery ORDER BY date DESC")
    fun transactionsByDate(search: String): DataSource.Factory<Int, CryptoCoinTransactionExtended>

    @Query("$transactionsQuery ORDER BY amount DESC")
    fun transactionsByAmount(search: String): DataSource.Factory<Int, CryptoCoinTransactionExtended>

    @Query("$transactionsQuery ORDER BY is_input DESC")
    fun transactionsByIsInput(search: String): DataSource.Factory<Int, CryptoCoinTransactionExtended>

    @Query("$transactionsQuery ORDER BY `from` DESC")
    fun transactionsByFrom(search: String): DataSource.Factory<Int, CryptoCoinTransactionExtended>

    @Query("$transactionsQuery ORDER BY `to` DESC")
    fun transactionsByTo(search: String): DataSource.Factory<Int, CryptoCoinTransactionExtended>

    @Query("SELECT * FROM crypto_coin_transaction WHERE account_id = :idAccount ORDER BY date DESC")
    fun getByIdAccountLiveData(idAccount: Long): LiveData<List<CryptoCoinTransaction>>

    @Query("SELECT * FROM crypto_coin_transaction WHERE account_id = :idAccount ORDER BY date DESC")
    fun getByIdAccount(idAccount: Long): List<CryptoCoinTransaction>

    @Query("SELECT * FROM crypto_coin_transaction WHERE id = :id")
    fun getByIdLiveData(id: Long): LiveData<CryptoCoinTransaction>

    @Query("SELECT * FROM crypto_coin_transaction WHERE id = :id")
    fun getById(id: Long): CryptoCoinTransaction

    @Query("SELECT * FROM crypto_coin_transaction WHERE date = :date and 'from' = :from and 'to' = :to and amount = :amount ")
    fun getByTransaction(date: Date, from: String, to: String, amount: Long): CryptoCoinTransaction

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransaction(vararg transactions: CryptoCoinTransaction): LongArray

    @Query("DELETE FROM crypto_coin_transaction")
    fun deleteAllTransactions()

    @Query("DELETE FROM crypto_coin_transaction")
    fun nukeTable()

    companion object {

        const val transactionsQuery =
            "SELECT cct.*, cna.name AS user_account_name, c.name AS contact_name, banc.name AS bitshares_account_name FROM crypto_coin_transaction cct " +
                    "LEFT JOIN crypto_net_account cna ON cct.account_id = cna.id " +
                    "LEFT JOIN contact c ON c.id =  (SELECT ca.contact_id FROM contact_address ca WHERE ca.address LIKE (CASE is_input WHEN 1 THEN cct.\"from\" ELSE cct.\"to\" END) LIMIT 1) " +
                    "LEFT JOIN bitshares_account_name_cache banc ON banc.account_id =  (CASE is_input WHEN 1 THEN cct.\"from\" ELSE cct.\"to\" END) " +
                    "WHERE user_account_name LIKE '%'||:search||'%' OR contact_name LIKE '%'||:search||'%' OR cct.\"from\" LIKE '%'||:search||'%' OR cct.\"to\" LIKE '%'||:search||'%' OR banc.name LIKE '%'||:search||'%'"
    }
}
