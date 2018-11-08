package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import cy.agorise.bitsybitshareswallet.enums.CryptoNetAccount

/*@Entity(
    tableName = "graphene_account",
    primaryKeys = { "crypto_net_account_id" },
    foreignKeys = ForeignKey(
        entity = CryptoNetAccount::class,
        parentColumns = "id",
        childColumns = "crypto_net_account_id"
    )
)*/
class GrapheneAccountInfo {

    /**
     * The database id of the cryptonetAccount
     */
    @ColumnInfo(name = "crypto_net_account_id")
    var cryptoNetAccountId: Long = 0

    /**
     * The account name
     */
    @ColumnInfo(name = "account_name")
    lateinit var name: String

    /**
     * The bitshares id of this account
     */
    @ColumnInfo(name = "account_id")
    lateinit var accountId: String

    /**
     * If the bitshares account is upgraded to LTM
     */
    @ColumnInfo(name = "upgraded_to_ltm")
    var upgradedToLtm: Boolean = false

    /**
     * Baisc constructor
     * @param cryptoNetAccountId The database ud of the CryptoNetAccount
     */
    constructor(cryptoNetAccountId: Long) {
        this.cryptoNetAccountId = cryptoNetAccountId
    }

    /**
     * Constructor used to save in the database
     * @param account a complete graphene account with its info
     */
    constructor(account: GrapheneAccount) {
        this.cryptoNetAccountId = account.id
        this.name = account.name!!
        this.accountId = account.accountId
    }
}
