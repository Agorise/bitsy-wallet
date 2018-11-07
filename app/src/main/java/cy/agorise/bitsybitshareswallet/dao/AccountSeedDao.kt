package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.AccountSeed

@Dao
interface AccountSeedDao {

    @get:Query("SELECT * FROM account_seed")
    val all: LiveData<List<AccountSeed>>

    @get:Query("SELECT * FROM account_seed")
    val allNoLiveData: List<AccountSeed>

    @Query("SELECT * FROM account_seed WHERE id = :id")
    fun findByIdLiveData(id: Long): LiveData<AccountSeed>

    @Query("SELECT * FROM account_seed WHERE id = :id")
    fun findById(id: Long): AccountSeed

    @Query("SELECT COUNT(*) from account_seed")
    fun countAccountSeeds(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAccountSeeds(vararg seeds: AccountSeed): LongArray

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAccountSeed(seed: AccountSeed): Long
}