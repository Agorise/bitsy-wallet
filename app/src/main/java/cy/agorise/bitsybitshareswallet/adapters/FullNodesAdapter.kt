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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.graphenej.network.FullNode
import java.util.*


/**
 * Adapter used to populate the elements of the Bitshares nodes dialog in order to show a list of
 * nodes with their latency.
 */
class FullNodesAdapter(private val context: Context) : RecyclerView.Adapter<FullNodesAdapter.ViewHolder>() {
    val TAG: String = this.javaClass.name

    private val mComparator =
        Comparator<FullNode> { a, b -> java.lang.Double.compare(a.latencyValue, b.latencyValue) }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNodeName: TextView = itemView.findViewById(R.id.tvNodeName)
        val ivNodeStatus: ImageView = itemView.findViewById(R.id.ivNodeStatus)
    }

    private val mSortedList = SortedList(FullNode::class.java, object : SortedList.Callback<FullNode>() {
        override fun onInserted(position: Int, count: Int) {
            notifyItemRangeInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            notifyItemRangeRemoved(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int) {
            notifyItemRangeChanged(position, count)
        }

        override fun compare(a: FullNode, b: FullNode): Int {
            return mComparator.compare(a, b)
        }

        override fun areContentsTheSame(oldItem: FullNode, newItem: FullNode): Boolean {
            return oldItem.latencyValue == newItem.latencyValue
        }

        override fun areItemsTheSame(item1: FullNode, item2: FullNode): Boolean {
            return item1.url == item2.url
        }
    })

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullNodesAdapter.ViewHolder {
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
            latency < 400 -> ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPrimary))
            latency < 800 -> ForegroundColorSpan(Color.rgb(255,136,0)) // Holo orange
            else -> ForegroundColorSpan(Color.rgb(204,0,0)) // Holo red
        }

        // Create a string with the latency number colored according to their amount
        val ssb = SpannableStringBuilder()
        ssb.append(fullNode.url.replace("wss://", ""), StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.append(" (")

        // 2000 ms is the timeout of the websocket used to calculate the latency, therefore if the
        // received latency is greater than such value we can assume the node was not reachable.
        val ms = if(latency < 2000) "%.0f ms".format(latency) else "??"

        ssb.append(ms, colorSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.append(")")

        viewHolder.tvNodeName.text = ssb
    }

    /**
     * Functions that adds/updates a FullNode to the SortedList
     */
    fun add(fullNode: FullNode) {
        // Remove the old instance of the FullNode before adding a new one. My understanding is that
        // the sorted list should be able to automatically find repeated elements and update them
        // instead of adding duplicates but it wasn't working so I opted for manually removing old
        // instances of FullNodes before adding the updated ones.
        var removed = 0
        for (i in 0 until mSortedList.size())
            if (mSortedList[i-removed].url == (fullNode.url))
                mSortedList.removeItemAt(i-removed++)

        mSortedList.add(fullNode)
    }

    /**
     * Function that adds a whole list of nodes to the SortedList. It should only be used at the
     * moment of populating the SortedList for the first time.
     */
    fun add(fullNodes: List<FullNode>) {
        mSortedList.addAll(fullNodes)
    }

    fun remove(fullNode: FullNode) {
        mSortedList.remove(fullNode)
    }

    override fun getItemCount(): Int {
        return mSortedList.size()
    }
}