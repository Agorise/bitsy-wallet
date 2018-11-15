package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.CryptoNetAccount

@Dao
interface CryptoNetAccountDao {

    @get:Query("SELECT * FROM crypto_net_account")
    val all: LiveData<List<CryptoNetAccount>>

    @get:Query("SELECT cna.* FROM crypto_net_account cna")
    val allCryptoNetAccount: List<CryptoNetAccount>

    @get:Query("SELECT * FROM crypto_net_account WHERE crypto_net = 'BITSHARES'")
    val bitsharesAccounts: LiveData<List<CryptoNetAccount>>

    @Query("SELECT cna.* FROM crypto_net_account cna WHERE seed_id = :seedId")
    fun getAllCryptoNetAccountBySeed(seedId: Long): List<CryptoNetAccount>

    @Query("SELECT * FROM crypto_net_account WHERE id = :accountId")
    fun getByIdLiveData(accountId: Long): LiveData<CryptoNetAccount>

    @Query("SELECT * FROM crypto_net_account WHERE id = :accountId")
    fun getById(accountId: Long): CryptoNetAccount

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCryptoNetAccount(vararg accounts: CryptoNetAccount): LongArray
}
