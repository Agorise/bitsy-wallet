package cy.agorise.bitsybitshareswallet.models

import androidx.room.*
import cy.agorise.bitsybitshareswallet.dao.Converters
import cy.agorise.bitsybitshareswallet.enums.CryptoNet

@Entity(tableName = "crypto_currency", indices = arrayOf(Index(value = arrayOf("crypto_net", "name"), unique = true)))
open class CryptoCurrency {

    /**
     * The id on the database
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0

    /**
     * The name or tag of this currency
     */
    @ColumnInfo(name = "name")
    var name: String? = null

    /**
     * CryptoCoin network where this currency belongs to
     */
    @ColumnInfo(name = "crypto_net")
    @TypeConverters(Converters::class)
    var cryptoNet: CryptoNet? = null


    /**
     * The decimal point
     */
    @ColumnInfo(name = "precision")
    var precision: Int = 0

    constructor() {}

    constructor(name: String, cryptoNet: CryptoNet, precision: Int) {
        this.name = name
        this.cryptoNet = cryptoNet
        this.precision = precision
    }
}
