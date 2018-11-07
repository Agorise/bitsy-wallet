package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import androidx.room.*
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import java.util.ArrayList

//@Entity(tableName = "contact", indices = { @Index("id"), @Index(value = { "name" }, unique = true), @Index("email") })
class Contact {

    /**
     * The id on the database
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "email")
    var email: String? = null

    @ColumnInfo(name = "gravatar")
    var gravatar: String? = null

    @Ignore
    var mAddresses: MutableList<ContactAddress>? = null

    fun addressesCount(): Int {
        return this.mAddresses!!.size
    }

    fun getAddress(index: Int): ContactAddress {
        return this.mAddresses!![index]
    }

    fun clearAddresses() {
        if (this.mAddresses != null) {
            this.mAddresses!!.clear()
        }
    }

    fun addAddress(address: ContactAddress) {
        if (this.mAddresses == null) {
            this.mAddresses = ArrayList<ContactAddress>()
        }
        this.mAddresses!!.add(address)
        address.contactId = this.id
    }

    fun getCryptoNetAddress(cryptoNet: CryptoNet): ContactAddress? {
        if (this.mAddresses != null) {
            for (address in this.mAddresses!!) {
                if (address.cryptoNet === cryptoNet) {
                    return address
                }
            }
        }

        return null
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val contact = o as Contact?

        if (id != contact!!.id) return false
        if (name != contact.name) return false
        if (if (gravatar != null) gravatar != contact.gravatar else contact.gravatar != null)
            return false
        return if (mAddresses != null) mAddresses == contact.mAddresses else contact.mAddresses == null
    }

    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<Contact> = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(
                @NonNull oldContact: Contact, @NonNull newContact: Contact
            ): Boolean {
                return oldContact.id == newContact.id
            }

            override fun areContentsTheSame(
                @NonNull oldContact: Contact, @NonNull newContact: Contact
            ): Boolean {
                return oldContact == newContact
            }
        }
    }
}
