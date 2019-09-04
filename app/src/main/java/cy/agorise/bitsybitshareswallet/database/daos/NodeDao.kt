package cy.agorise.bitsybitshareswallet.database.daos

import androidx.room.*
import cy.agorise.bitsybitshareswallet.database.entities.Node

@Dao
abstract class NodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(nodes: Node)

    @Query("SELECT * FROM nodes ORDER BY latency ASC")
    abstract suspend fun getSortedNodes(): List<Node>

    @Query("UPDATE nodes SET latency = :newLatency WHERE url = :queryUrl")
    abstract fun updateLatency(newLatency: Long, queryUrl: String)

    @Query("UPDATE nodes SET last_update=:lastUpdate WHERE url=:url")
    abstract suspend fun updateNode(url: String, lastUpdate: Long)

    @Query("SELECT * FROM nodes WHERE url=:url")
    abstract suspend fun get(url: String): Node?

    @Query("DELETE FROM nodes WHERE last_update != :timestamp")
    abstract suspend fun deleteOutdatedNodes(timestamp: Long)

    /**
     * Updates the list of nodes stored in the database in two steps:
     * 1.   - If a node does not already exist in the database then it just creates(inserts) a new entry.
     *      - If a nodes does exist then it only updates its lastUpdate field, so that it does not get
     *      removed in the last step, leaving the latency untouched.
     * 2.   Deletes all the nodes that are stored in the database, but were not updated in the previous
     *      step.
     */
    @Transaction
    open suspend fun updateNodes(nodes: List<Node>, timestamp: Long) {
        for (node in nodes) {
            if (get(node.url) == null) {
                insert(node)
            } else {
                updateNode(node.url, node.lastUpdate)
            }
        }
        deleteOutdatedNodes(timestamp)
    }
}