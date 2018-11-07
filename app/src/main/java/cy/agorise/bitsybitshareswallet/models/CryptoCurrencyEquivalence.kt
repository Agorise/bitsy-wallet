package cy.agorise.bitsybitshareswallet.models

import androidx.room.*
import java.util.*

/*@Entity(tableName = "crypto_currency_equivalence", indices = {
    @Index(value = { "from_crypto_currency_id", "to_crypto_currency_id" }, unique = true),
    @Index(value = { "from_crypto_currency_id" }),
    @Index(value = { "to_crypto_currency_id" }),
}, foreignKeys = {
    @ForeignKey(
        entity = CryptoCurrency.class,
                parentColumns = " id",
    childColumns = "from_crypto_currency_id",
    onDelete = ForeignKey.CASCADE
    ),
    @ForeignKey(
        entity = CryptoCurrency.class,
                parentColumns = " id",
    childColumns = "to_crypto_currency_id",
    onDelete = ForeignKey.CASCADE
    )
})*/
class CryptoCurrencyEquivalence(
    @field:ColumnInfo(name = "from_crypto_currency_id")
    var fromCurrencyId: Long, @field:ColumnInfo(name = "to_crypto_currency_id")
    var toCurrencyId: Long, @field:ColumnInfo(name = "value")
    var value: Int, @field:ColumnInfo(name = "last_checked")
    var lastChecked: Date?
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0
}
