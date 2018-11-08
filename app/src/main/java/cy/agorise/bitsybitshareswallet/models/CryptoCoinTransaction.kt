package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import androidx.room.*
import cy.agorise.bitsybitshareswallet.enums.CryptoNetAccount
import java.util.*

@Entity(tableName = "crypto_coin_transaction", indices = arrayOf(
    Index(value = arrayOf("account_id")),
    Index(value = arrayOf("id_currency"))), foreignKeys = arrayOf(ForeignKey(
    entity = CryptoNetAccount::class,
    parentColumns = arrayOf("id"),
    childColumns = arrayOf("account_id"),
    onDelete = ForeignKey.CASCADE
),
    ForeignKey(
        entity = CryptoCurrency::class,
                parentColumns = arrayOf("id"),
childColumns = arrayOf("id_currency"),
onDelete = ForeignKey.CASCADE
)))
class CryptoCoinTransaction {

    /**
     * The account associated with this transaction
     */
    @Ignore
    lateinit var account: CryptoNetAccount

    /**
     * The id on the database
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0
    /**
     * The full date of this transaction
     */
    @ColumnInfo(name = "date")
    var date: Date? = null
    /**
     * If this transaction is input of the account associated with it
     */
    @ColumnInfo(name = "is_input")
    var input: Boolean = false
    /**
     * The id of the account assoiciated, this is used for the foreign key definition
     */
    @ColumnInfo(name = "account_id")
    var accountId: Long = -1
    /**
     * The amount of asset is moved in this transaction
     */
    @ColumnInfo(name = "amount")
    var amount: Long = 0

    /**
     * The id of the Crypto Currency to use in the database
     */
    @ColumnInfo(name = "id_currency")
    var idCurrency: Int = 0
    /**
     * If this transaction is confirmed
     */
    @ColumnInfo(name = "is_confirmed")
    var isConfirmed: Boolean = false

    /**
     * The address or account the amount of assets comes from
     */
    @ColumnInfo(name = "from")
    var from: String? = null

    /**
     * The address or account the amount of assets goes to
     */
    @ColumnInfo(name = "to")
    var to: String? = null


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as CryptoCoinTransaction?

        if (input != that!!.input) return false
        if (accountId != that.accountId) return false
        if (amount != that.amount) return false
        if (idCurrency != that.idCurrency) return false
        if (isConfirmed != that.isConfirmed) return false
        if (if (date != null) date != that.date else that.date != null) return false
        if (if (from != null) from != that.from else that.from != null) return false
        return if (to != null) to == that.to else that.to == null

    }

    companion object {


        val DIFF_CALLBACK: DiffUtil.ItemCallback<CryptoCoinTransaction> =
            object : DiffUtil.ItemCallback<CryptoCoinTransaction>() {
                override fun areItemsTheSame(
                    @NonNull oldTransaction: CryptoCoinTransaction, @NonNull newTransaction: CryptoCoinTransaction
                ): Boolean {
                    return oldTransaction.id == newTransaction.id
                }

                override fun areContentsTheSame(
                    @NonNull oldTransaction: CryptoCoinTransaction, @NonNull newTransaction: CryptoCoinTransaction
                ): Boolean {
                    return oldTransaction == newTransaction
                }
            }
    }
}
