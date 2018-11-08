package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cy.agorise.bitsybitshareswallet.enums.CryptoNet

@Entity(
    tableName = "contact_address",
    indices = arrayOf(Index(value = arrayOf("id"),
        unique = true),
        Index(value = arrayOf("contact_id", "crypto_net"), unique = true)))
class ContactAddress {

    /**
     * The id on the database
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0

    @ColumnInfo(name = "contact_id")
    var contactId: Long = 0

    /**
     * The crypto net of the address
     */
    @NonNull
    @ColumnInfo(name = "crypto_net")
    var cryptoNet: CryptoNet? = null

    @ColumnInfo(name = "address")
    var address: String? = null

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as ContactAddress?

        if (contactId != that!!.contactId) return false
        return if (cryptoNet !== that.cryptoNet) false else address == that.address
    }

    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<ContactAddress> = object : DiffUtil.ItemCallback<ContactAddress>() {
            override fun areItemsTheSame(
                @NonNull oldContactAddress: ContactAddress, @NonNull newContactAddress: ContactAddress
            ): Boolean {
                return oldContactAddress.contactId == newContactAddress.contactId && oldContactAddress.cryptoNet === newContactAddress.cryptoNet
            }

            override fun areContentsTheSame(
                @NonNull oldContactAddress: ContactAddress, @NonNull newContactAddress: ContactAddress
            ): Boolean {
                return oldContactAddress == newContactAddress
            }
        }
    }
}
