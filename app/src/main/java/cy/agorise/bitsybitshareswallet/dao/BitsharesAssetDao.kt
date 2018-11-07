package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.BitsharesAssetInfo

@Dao
interface BitsharesAssetDao {

    @get:Query("SELECT * FROM bitshares_asset")
    val all: LiveData<List<BitsharesAssetInfo>>

    @Query("SELECT * FROM bitshares_asset WHERE crypto_curreny_id = :cryptoCurrencyId")
    fun getBitsharesAssetInfo(cryptoCurrencyId: Long): BitsharesAssetInfo

    @Query("SELECT * FROM bitshares_asset WHERE crypto_curreny_id = :cryptoCurrencyId")
    fun getBitsharesAssetInfoFromCurrencyId(cryptoCurrencyId: Long): BitsharesAssetInfo

    @Query("SELECT * FROM bitshares_asset WHERE bitshares_id = :bitsharesId")
    fun getBitsharesAssetInfoById(bitsharesId: String): BitsharesAssetInfo

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertBitsharesAssetInfo(vararg accounts: BitsharesAssetInfo): LongArray
}
