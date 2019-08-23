package cy.agorise.bitsybitshareswallet.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nodes")
data class Node(
        @PrimaryKey
        @ColumnInfo(name = "url") var url: String,
        @ColumnInfo(name = "latency") var latency: Long = Long.MAX_VALUE,
        @ColumnInfo(name = "last_update") var lastUpdate: Long = 0L
)