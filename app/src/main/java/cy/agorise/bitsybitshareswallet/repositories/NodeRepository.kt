package cy.agorise.bitsybitshareswallet.repositories

import android.os.AsyncTask
import android.util.Log
import com.crashlytics.android.Crashlytics
import cy.agorise.bitsybitshareswallet.database.daos.NodeDao
import cy.agorise.bitsybitshareswallet.database.entities.Node
import cy.agorise.bitsybitshareswallet.network.BitsyWebservice
import cy.agorise.bitsybitshareswallet.network.ServiceGenerator
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.graphenej.network.FullNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NodeRepository(private val nodeDao: NodeDao) {

    companion object {
        private const val TAG = "NodeRepository"

        // Minimum number of nodes required to update the nodes db table.
        private const val MIN_NODES_SIZE = 3

        // List of BitShares nodes the app will try to connect to
        var BITSHARES_NODE_URLS = arrayOf(
            // PP private nodes
            "wss://nl.palmpay.io/ws",

            // Other public nodes
            "wss://btsws.roelandp.nl/ws",
            "wss://api.bts.mobi/ws",
            "wss://kimziv.com/ws",
            "wss://api.bts.ai")
    }

    private val mBitsyWebservice: BitsyWebservice?

    init {
        val sg = ServiceGenerator(Constants.BITSY_WEBSERVICE_URL)
        mBitsyWebservice = sg.getService(BitsyWebservice::class.java)
    }

    /**
     * Returns a Pair of items:
     * First.   A list of comma separated node urls in form of a string. The node urls come from the
     *          database if the nodes table is already populated, else a default list is used.
     * Second.  A Boolean that specifies if the app should try to autoConnect immediately, or wait
     *          for other event to launch the connect method.
     */
    suspend fun getFormattedNodes(): Pair<String, Boolean> {
        val nodes = nodeDao.getSortedNodes()

        // TODO verify if this is the best way to fire and forget launch a coroutine inside another coroutine
        // Launches a job to refresh the list of nodes into the database, without blocking the
        // execution of this function, so that the formatted nodes can be returned immediately
        // without waiting until the nodes have been updated in the database.
        CoroutineScope(Dispatchers.Default).launch {
            refreshNodes(nodes)
        }

        return if (nodes.size < MIN_NODES_SIZE) {
            // If the nodes db table is empty or very small, it could mean that the nodes have not
            // still been updated from the webservice, thus returning a default list of nodes as a fallback.
            // False is returned since we want to verify the node latencies before choosing the best
            // one and trying to connect to it.
            Pair(getDefaultFormattedNodes(), false)
        } else {
            // Use the list of nodes stored in the database. True is returned since the list of nodes
            // is already ordered by latency, and we don't need to wait to obtain the latency
            // readings, thus the app can immediately try to connect to the first node in the list.
            Pair(getDBFormattedNodes(nodes), true)
        }
    }

    /**
     * Verifies if the nodes information should be updated and if true, fetches the nodes
     * information from the webservice and updates the database
     */
    private suspend fun refreshNodes(nodes: List<Node>) {
        val now = System.currentTimeMillis() / 1000
        val lastUpdate: Long = if (nodes.size < MIN_NODES_SIZE) {
            0
        } else {
            nodes[0].lastUpdate
        }
        val updatePeriod = Constants.NODES_UPDATE_PERIOD
        // Verify if nodes list should be updated
        if (now - updatePeriod > lastUpdate) {
            try {
                val response = mBitsyWebservice?.getNodes()
                // Update the list of nodes only if we got at least MIN_NODES_SIZE nodes
                if (response?.isSuccessful == true && (response.body()?.size ?: 0) >= MIN_NODES_SIZE) {
                    val nodesWS = response.body() ?: return

                    val nodesDB = nodesWS.map {
                        Node(url = it.url, lastUpdate = now)
                    }

                    Log.d(TAG, "Updating the list of nodes.")
                    nodeDao.updateNodes(nodesDB, now)
                }
            } catch (e: Exception) {
                // Generic exception handling
                Crashlytics.logException(e)
            }
        }
    }

    private fun getDefaultFormattedNodes(): String {
        return BITSHARES_NODE_URLS.joinToString(separator = ",")
    }

    private fun getDBFormattedNodes(nodes: List<Node>): String {
        return nodes.joinToString(separator = ",") { it.url }
    }

    /**
     * Function that will receive an up-to-date list of FullNode instances and persist it on
     * the database.
     *
     * @param nodes List of nodes with fresh latency measurements.
     */
    fun updateNodeLatencies(nodes: List<FullNode>) {
        AsyncTask.execute {
            nodes.forEach {
                nodeDao.updateLatency(it.latencyValue.toLong(), it.url)
            }
        }
    }
}