package cy.agorise.bitsybitshareswallet.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class Transfer (
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "block_number") val blockNumber: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "fee_amount") val feeAmount: Long,
    @ColumnInfo(name = "fee_asset_id") val feeAssetId: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "destination") val destination: String,
    @ColumnInfo(name = "transfer_amount") val transferAmount: Long,
    @ColumnInfo(name = "transfer_asset_id") val transferAssetId: String,
    @ColumnInfo(name = "memo") val memo: String,
    @ColumnInfo(name = "bts_value") var btsValue: Long? = NOT_CALCULATED
    ){

    init {
        if(transferAssetId == "1.3.0"){
            // If the transferred asset is BTS, we can fill the btsValue field immediately
            btsValue = transferAmount
        }
    }

    companion object {
        // Constant used to specify an uninitialized BTS equivalent value
        const val NOT_CALCULATED: Long = -1L
        // Constant used to specify a BTS equivalent value whose calculation returned an error
        const val ERROR: Long = -2L
    }
}