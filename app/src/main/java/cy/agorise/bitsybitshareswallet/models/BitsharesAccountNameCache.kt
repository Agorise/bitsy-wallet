package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull
import androidx.room.*

/*@Entity(tableName = "bitshares_account_name_cache", indices = {
    @Index("id"),
    @Index(value = { "account_id" }, unique = true)
})*/
class BitsharesAccountNameCache {

    /**
     * The id on the database
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0

    /**
     * The id of the account
     */
    @ColumnInfo(name = "account_id")
    @NonNull
    var accountId: String? = null

    /*
     * The name of the account
     */
    @ColumnInfo(name = "name")
    var name: String? = null

    constructor() {}

    @Ignore
    constructor(id: Long, accountId: String, name: String) {
        this.id = id
        this.accountId = accountId
        this.name = name
    }
}

