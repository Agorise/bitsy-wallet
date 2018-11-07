package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.CryptoCurrencyEquivalence

@Dao
interface CryptoCurrencyEquivalenceDao {

    @get:Query("SELECT * FROM crypto_currency_equivalence")
    val all: List<CryptoCurrencyEquivalence>

    @Query("SELECT * FROM crypto_currency_equivalence WHERE id = :id")
    fun getById(id: Long): CryptoCurrencyEquivalence

    @Query("SELECT * FROM crypto_currency_equivalence WHERE from_crypto_currency_id = :fromCurrencyId AND to_crypto_currency_id = :toCurrencyId")
    fun getByFromTo(fromCurrencyId: Long, toCurrencyId: Long): LiveData<CryptoCurrencyEquivalence>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCryptoCurrencyEquivalence(vararg currenciesEquivalences: CryptoCurrencyEquivalence): LongArray

}