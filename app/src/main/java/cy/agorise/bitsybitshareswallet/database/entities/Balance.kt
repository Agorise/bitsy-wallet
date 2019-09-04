package cy.agorise.bitsybitshareswallet.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balances")
data class Balance(
    @PrimaryKey
    @ColumnInfo(name = "asset_id") val assetId: String,
    @ColumnInfo(name = "asset_amount") val assetAmount: Long,
    @ColumnInfo(name = "last_update") val lastUpdate: Long
)