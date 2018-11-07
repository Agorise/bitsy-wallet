package cy.agorise.bitsybitshareswallet.enums

import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import androidx.room.*

/*@Entity(
    tableName = "crypto_net_account",
    indices = { @Index("id"), @Index("seed_id"), @Index(value = { "seed_id", "crypto_net", "account_index" }, unique = true) },
    foreignKeys = ForeignKey(entity = AccountSeed::class, parentColumns = "id", childColumns = "seed_id")
)*/
open class CryptoNetAccount {

    /**
     * The id on the database
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0

    /**
     * The id of the seed used by this account
     */
    @ColumnInfo(name = "seed_id")
    var seedId: Long = 0

    /**
     * The account index on this wallet
     */
    @ColumnInfo(name = "account_index")
    var accountIndex: Int = 0

    /**
     * The crypto net of the account
     */
    @ColumnInfo(name = "crypto_net")
    var cryptoNet: CryptoNet? = null

    /*
     * The name of the account
     */
    @ColumnInfo(name = "name")
    var name: String? = null

    constructor() {}

    @Ignore
    constructor(mId: Long, mSeedId: Long, mAccountIndex: Int, mCryptoNet: CryptoNet) {
        this.id = mId
        this.seedId = mSeedId
        this.accountIndex = mAccountIndex
        this.cryptoNet = mCryptoNet
    }

    override fun toString(): String {
        return this.name!!
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as CryptoNetAccount?

        if (id != that!!.id) return false
        if (seedId != that.seedId) return false
        if (accountIndex != that.accountIndex) return false
        if (cryptoNet !== that.cryptoNet) return false
        return if (name != null) name == that.name else that.name == null
    }

    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<CryptoNetAccount> =
            object : DiffUtil.ItemCallback<CryptoNetAccount>() {
                override fun areItemsTheSame(
                    @NonNull oldAccount: CryptoNetAccount, @NonNull newAccount: CryptoNetAccount
                ): Boolean {
                    return oldAccount.id == newAccount.id
                }

                override fun areContentsTheSame(
                    @NonNull oldAccount: CryptoNetAccount, @NonNull newAccount: CryptoNetAccount
                ): Boolean {
                    return oldAccount == newAccount
                }
            }
    }
}
