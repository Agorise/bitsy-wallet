package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import java.util.*

@Entity
class CryptoCoinTransactionExtended {

    @Embedded
    var cryptoCoinTransaction: CryptoCoinTransaction? = null

    @ColumnInfo(name = "user_account_name")
    var userAccountName: String? = null

    @ColumnInfo(name = "contact_name")
    var contactName: String? = null

    @ColumnInfo(name = "bitshares_account_name")
    var bitsharesAccountName: String? = null

    val from: String
        get() = this.cryptoCoinTransaction!!.from!!

    val to: String
        get() = this.cryptoCoinTransaction!!.to!!

    val accountId: Long
        get() = this.cryptoCoinTransaction!!.accountId

    val account: CryptoNetAccount
        get() = this.cryptoCoinTransaction!!.account

    val id: Long
        get() = this.cryptoCoinTransaction!!.id

    val date: Date
        get() = this.cryptoCoinTransaction!!.date!!

    val input: Boolean
        get() = this.cryptoCoinTransaction!!.input

    val isConfirmed: Boolean
        get() = this.cryptoCoinTransaction!!.isConfirmed

    val amount: Long
        get() = this.cryptoCoinTransaction!!.amount

    val idCurrency: Int
        get() = this.cryptoCoinTransaction!!.idCurrency

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as CryptoCoinTransactionExtended?

        if (if (this.cryptoCoinTransaction != null) !this.cryptoCoinTransaction!!.equals(that!!.cryptoCoinTransaction) else that!!.cryptoCoinTransaction != null) return false
        if (if (this.userAccountName != null) this.userAccountName != that.userAccountName else that.userAccountName != null) return false
        return if (if (this.contactName != null) this.contactName != that.contactName else that.contactName != null) false else true

    }

    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<CryptoCoinTransactionExtended> =
            object : DiffUtil.ItemCallback<CryptoCoinTransactionExtended>() {
                override fun areItemsTheSame(
                    @NonNull oldTransaction: CryptoCoinTransactionExtended, @NonNull newTransaction: CryptoCoinTransactionExtended
                ): Boolean {
                    return oldTransaction.id == newTransaction.id
                }

                override fun areContentsTheSame(
                    @NonNull oldTransaction: CryptoCoinTransactionExtended, @NonNull newTransaction: CryptoCoinTransactionExtended
                ): Boolean {
                    return oldTransaction == newTransaction
                }
            }
    }
}
