package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import cy.agorise.bitsybitshareswallet.enums.CryptoNet

@Entity
class CryptoNetBalance {

    /**
     * The id of the account of this balance
     */
    @ColumnInfo(name = "account_id")
    var accountId: Long = 0

    /**
     * The crypto net of the account
     */
    @ColumnInfo(name = "crypto_net")
    var cryptoNet: CryptoNet? = null

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as CryptoNetBalance?
        return accountId == that!!.accountId

    }

    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<CryptoNetBalance> =
            object : DiffUtil.ItemCallback<CryptoNetBalance>() {
                override fun areItemsTheSame(
                    @NonNull oldBalance: CryptoNetBalance, @NonNull newBalance: CryptoNetBalance
                ): Boolean {
                    return oldBalance.accountId == newBalance.accountId
                }

                override fun areContentsTheSame(
                    @NonNull oldBalance: CryptoNetBalance, @NonNull newBalance: CryptoNetBalance
                ): Boolean {
                    return oldBalance == newBalance
                }
            }
    }
}
