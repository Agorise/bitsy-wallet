package cy.agorise.bitsybitshareswallet.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.database.entities.Asset

@Dao
interface AssetDao {
    @Insert
    fun insert(asset: Asset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(assets: List<Asset>)

    @Query("""
        SELECT
            id,
            symbol,
            precision,
            description,
            issuer
        FROM
            assets
        INNER JOIN
            balances
        WHERE
            assets.id = balances.asset_id AND
            balances.asset_amount > 0
    """)
    fun getAllNonZero(): LiveData<List<Asset>>

    @Query("SELECT * FROM assets WHERE id = :assetId")
    fun getAssetDetails(assetId: String): Asset
}
