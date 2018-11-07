package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.BitcoinAddress

@Dao
interface BitcoinAddressDao {

    @get:Query("SELECT * FROM bitcoin_address")
    val all: LiveData<BitcoinAddress>

    @Query("SELECT COUNT(*) FROM bitcoin_address ba WHERE ba.address = :address")
    fun addressExists(address: String): Boolean?

    @Query("SELECT * FROM bitcoin_address ba WHERE ba.address = :address")
    fun getdadress(address: String): BitcoinAddress

    @Query("SELECT * FROM bitcoin_address ba WHERE ba.address_index = :index and ba.is_change = 'true'")
    fun getChangeByIndex(index: Long): BitcoinAddress

    @Query("SELECT * FROM bitcoin_address ba WHERE ba.address_index = :index and ba.is_change = 'false'")
    fun getExternalByIndex(index: Long): BitcoinAddress

    @Query("SELECT MAX(ba.address_index) FROM bitcoin_address ba WHERE ba.account_id = :accountId and ba.is_change = 'true' ")
    fun getLastChangeAddress(accountId: Long): Long

    @Query("SELECT MAX(ba.address_index) FROM bitcoin_address ba WHERE ba.account_id = :accountId and ba.is_change = 'false' ")
    fun getLastExternalAddress(accountId: Long): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBitcoinAddresses(vararg addresses: BitcoinAddress): LongArray
}
