package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "authorities", foreignKeys =
    [ForeignKey(
        entity = UserAccount::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )]
)
data class Authority (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "encrypted_brainkey") val encryptedBrainkey: String,
    @ColumnInfo(name = "encrypted_sequence_number") val encryptedSequenceNumber: String, // TODO verify data type
    @ColumnInfo(name = "encrypted_wif") val encryptedWif: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "authority_type") val authorityType: Int
)