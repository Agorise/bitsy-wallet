package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import androidx.room.*

@Entity(
    tableName = "crypto_coin_balance",
    indices = arrayOf(Index("id"), Index("account_id"), Index(value = arrayOf("account_id", "crypto_currency_id"), unique = true)),
    foreignKeys = [ForeignKey(entity = CryptoNetAccount::class, parentColumns = arrayOf("id"), childColumns = arrayOf("account_id"))]
)
class CryptoCoinBalance {

    /**
     * The id on the database
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = 0

    @ColumnInfo(name = "account_id")
    var accountId: Long? = 0

    @ColumnInfo(name = "crypto_currency_id")
    var cryptoCurrencyId: Long = 0

    @ColumnInfo(name = "balance")
    var balance: Long? = 0

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as CryptoCoinBalance?

        if (accountId != that!!.accountId) return false
        return if (balance != that.balance) false else cryptoCurrencyId == that.cryptoCurrencyId

    }

    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<CryptoCoinBalance> =
            object : DiffUtil.ItemCallback<CryptoCoinBalance>() {
                override fun areItemsTheSame(
                    @NonNull oldBalance: CryptoCoinBalance, @NonNull newBalance: CryptoCoinBalance
                ): Boolean {
                    return oldBalance.cryptoCurrencyId == newBalance.cryptoCurrencyId
                }

                override fun areContentsTheSame(
                    @NonNull oldBalance: CryptoCoinBalance, @NonNull newBalance: CryptoCoinBalance
                ): Boolean {
                    return oldBalance == newBalance
                }
            }
    }
}
