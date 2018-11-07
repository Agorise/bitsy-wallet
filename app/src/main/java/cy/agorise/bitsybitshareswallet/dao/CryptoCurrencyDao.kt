package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.CryptoCurrency

@Dao
interface CryptoCurrencyDao {

    @get:Query("SELECT * FROM crypto_currency")
    val all: List<CryptoCurrency>

    @Query("SELECT * FROM crypto_currency WHERE id = :id")
    fun getById(id: Long): CryptoCurrency

    @Query("SELECT * FROM crypto_currency WHERE id = :id")
    fun getLDById(id: Long): LiveData<CryptoCurrency>

    @Query("SELECT * FROM crypto_currency WHERE name = :name AND crypto_net = :cryptoNet")
    fun getByNameAndCryptoNet(name: String, cryptoNet: String): CryptoCurrency

    @Query("SELECT * FROM crypto_currency WHERE id IN (:ids)")
    fun getByIds(ids: List<Long>): List<CryptoCurrency>

    @Query("SELECT * FROM crypto_currency WHERE name = :name")
    fun getLiveDataByName(name: String): LiveData<CryptoCurrency>

    @Query("SELECT * FROM crypto_currency WHERE name = :name and crypto_net = :cryptoNet")
    fun getByName(name: String, cryptoNet: String): CryptoCurrency

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCryptoCurrency(vararg currencies: CryptoCurrency): LongArray

}