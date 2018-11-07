package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.BitsharesAccountNameCache

@Dao
 interface BitsharesAccountNameCacheDao {

@get:Query(
    "SELECT -1 AS id, cct.'to' AS account_id, '' AS name FROM crypto_coin_transaction AS cct WHERE cct.'to' NOT IN (SELECT account_id FROM bitshares_account_name_cache banc)" +
    " UNION " +
    "SELECT -1 AS id, cct.'from' AS account_id, '' AS name FROM crypto_coin_transaction AS cct WHERE cct.'from' NOT IN (SELECT account_id FROM bitshares_account_name_cache banc)"
)
 val uncachedBitsharesAccountName: LiveData<List<BitsharesAccountNameCache>>

@Query("SELECT * FROM bitshares_account_name_cache WHERE account_id = :account_id")
 fun getLDByAccountId(account_id:String):LiveData<BitsharesAccountNameCache>

@Query("SELECT * FROM bitshares_account_name_cache WHERE account_id = :account_id")
 fun getByAccountId(account_id:String):BitsharesAccountNameCache

@Query("SELECT * FROM bitshares_account_name_cache WHERE name = :account_name")
 fun getByAccountName(account_name:String):BitsharesAccountNameCache

@Insert(onConflict = OnConflictStrategy.REPLACE)
 fun insertBitsharesAccountNameCache(vararg accountsNames:BitsharesAccountNameCache):LongArray

}
