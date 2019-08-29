package cy.agorise.bitsybitshareswallet.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.SortedListAdapterCallback
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.graphenej.network.FullNode


/**
 * Adapter used to populate the elements of the Bitshares nodes dialog in order to show a list of
 * nodes with their latency.
 */
class FullNodesAdapter(private val context: Context) :
    RecyclerView.Adapter<FullNodesAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "FullNodesAdapter"

        private const val MAX_LATENCY = 9999
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNodeName: TextView = itemView.findViewById(R.id.tvNodeName)
        val ivNodeStatus: ImageView = itemView.findViewById(R.id.ivNodeStatus)
    }

    private val mSortedList = SortedList(FullNode::class.java,
        object : SortedListAdapterCallback<FullNode>(this) {

            override fun compare(a: FullNode, b: FullNode): Int {
                return a.latencyValue.compareTo(b.latencyValue)
            }

            override fun areContentsTheSame(oldItem: FullNode, newItem: FullNode): Boolean {
                return oldItem.latencyValue == newItem.latencyValue &&
                        oldItem.isConnected == oldItem.isConnected &&
                        oldItem.isRemoved == oldItem.isRemoved
            }

            override fun areItemsTheSame(oldItem: FullNode, newItem: FullNode): Boolean {
                return oldItem.url == newItem.url
            }
        })

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)

        val transactionView = inflater.inflate(R.layout.item_node, parent, false)

        return ViewHolder(transactionView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val fullNode = mSortedList[position]

        // Show the green check mark before the node name if that node is the one being used
        if (fullNode.isConnected)
            viewHolder.ivNodeStatus.setImageResource(R.drawable.ic_connected)
        else
            viewHolder.ivNodeStatus.setImageDrawable(null)

        val latency = fullNode.latencyValue

        // Select correct color span according to the latency value
        val colorSpan = when {
            latency < 400 -> ForegroundColorSpan(ContextCompat.getColor(context, R.color.ppGreen))
            latency < 800 -> ForegroundColorSpan(Color.rgb(255,136,0)) // Holo orange
            else -> ForegroundColorSpan(Color.rgb(204,0,0)) // Holo red
        }

        // Create a string with the latency number colored according to their amount
        val ssb = SpannableStringBuilder()
        ssb.append(fullNode.url.replace("wss://", ""), StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.append(" (")

        // Make sure that the latency shown is no greater than MAX_LATENCY
        val ms = if(latency <= MAX_LATENCY) "%.0fms".format(latency) else ">%dms".format(MAX_LATENCY)

        ssb.append(ms, colorSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.append(")")

        viewHolder.tvNodeName.text = ssb
    }

    /**
     * Functions that adds/updates a FullNode to the SortedList
     */
    @Synchronized fun add(fullNode: FullNode) {
        var index = -1
        for (i in 0 until mSortedList.size()) {
            if (mSortedList[i].url == fullNode.url) {
                index = i
                break
            }
        }

        if (index == -1) {
            Log.d(TAG , "Adding node: ${fullNode.url}")
            mSortedList.add(fullNode) // Add it if it does not exist
        } else {
            Log.d(TAG , "Updating node: ${fullNode.url}")
            mSortedList.updateItemAt(index, fullNode) // Update it if it does exist
        }
    }

    /**
     * Function that adds a whole list of nodes to the SortedList. It should only be used at the
     * moment of populating the SortedList for the first time.
     */
    fun add(fullNodes: List<FullNode>) = fullNodes.forEach { fullNode ->
        // Make sure we are not adding duplicate nodes when the app returns from another activity
        if (fullNode.latencyValue < Long.MAX_VALUE-1)
            add(fullNode)
    }

    fun remove(fullNode: FullNode) {
        Log.d(TAG , "Removing node: ${fullNode.url}")
        mSortedList.remove(fullNode)
    }

    override fun getItemCount(): Int {
        return mSortedList.size()
    }
}