package cy.agorise.bitsybitshareswallet.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import cy.agorise.bitsybitshareswallet.models.Contact
import cy.agorise.bitsybitshareswallet.models.ContactAddress
import javax.sql.DataSource

@Dao
interface ContactDao {

    @get:Query("SELECT * FROM contact")
    val all: LiveData<List<Contact>>

    @Query("SELECT * FROM contact ORDER BY name ASC")
    fun contactsByName(): DataSource.Factory<Int, Contact>

    @Query("SELECT c.* FROM contact c WHERE c.id IN (SELECT DISTINCT(ca.contact_id) FROM contact_address ca WHERE ca.crypto_net == :cryptoNet) ORDER BY name ASC, email ASC")
    fun contactsByNameAndCryptoNet(cryptoNet: String): DataSource.Factory<Int, Contact>

    @Query("SELECT * FROM contact WHERE id = :id")
    fun getById(id: Long): LiveData<Contact>

    @Query("SELECT count(*) FROM contact WHERE name = :name")
    fun existsByName(name: String): Boolean

    @Query("SELECT * FROM contact_address WHERE contact_id = :contactId")
    fun getContactAddresses(contactId: Long): LiveData<List<ContactAddress>>

    @Update(onConflict = OnConflictStrategy.ABORT)
    fun update(vararg contacts: Contact)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun add(vararg contacts: Contact): LongArray

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun addAddresses(vararg contactAddresses: ContactAddress)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateAddresses(vararg contactAddresses: ContactAddress)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateAddressesFields(vararg contactAddresses: ContactAddress)

    @Delete
    fun deleteContacts(vararg contacts: Contact)
}
