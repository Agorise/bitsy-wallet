package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.TypeConverters
import cy.agorise.bitsybitshareswallet.dao.Converters

/*@Entity(
    tableName = "bitshares_asset",
    primaryKeys = { "crypto_curreny_id" },
    foreignKeys = ForeignKey(entity = CryptoCurrency::class, parentColumns = "id", childColumns = "crypto_curreny_id")
)*/
class BitsharesAssetInfo {
    //The crypto Currency representing this bitshares asset
    @ColumnInfo(name = "crypto_curreny_id")
    var cryptoCurrencyId: Long = 0
    // The bitshares internal id
    @ColumnInfo(name = "bitshares_id")
    var bitsharesId: String? = null
    // The bitshares type see the enum below
    @ColumnInfo(name = "asset_type")
    @TypeConverters(Converters::class)
    var assetType: BitsharesAsset.Type? = null

    constructor() {}

    constructor(symbol: String, precision: Int, bitsharesId: String, assetType: BitsharesAsset.Type) {
        this.bitsharesId = bitsharesId
        this.assetType = assetType
    }

    constructor(asset: BitsharesAsset) {
        this.cryptoCurrencyId = asset.id
        this.bitsharesId = asset.bitsharesId
        this.assetType = asset.assetType
    }
}
