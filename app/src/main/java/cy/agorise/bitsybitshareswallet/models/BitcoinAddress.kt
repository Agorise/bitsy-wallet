package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE

@Entity(tableName = "bitcoin_address",
    foreignKeys =  arrayOf(
        ForeignKey(entity = CryptoNetAccount::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("account_id"),
            onDelete = CASCADE)),
    primaryKeys = arrayOf("account_id", "address_index"))

class BitcoinAddress {

    /**
     * The id of the account associated
     */
    @ColumnInfo(name = "account_id")
    var accountId: Long = 0

    /**
     * The index of this address
     */
    @ColumnInfo(name = "address_index")
    @NonNull
    @get:NonNull
    var index: Long = 0

    /**
     * Whether or not this address is a change one
     */
    @ColumnInfo(name = "is_change")
    @NonNull
    var isChange: Boolean = false

    /**
     * Address
     */
    @ColumnInfo(name = "address")
    @NonNull
    lateinit var address: String

    constructor(accountId: Long, @NonNull index: Long, isChange: Boolean, address: String) {
        this.accountId = accountId
        this.index = index
        this.isChange = isChange
        this.address = address
    }

    constructor() {}
}
