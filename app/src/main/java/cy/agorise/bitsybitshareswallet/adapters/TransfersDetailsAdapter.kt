package cy.agorise.bitsybitshareswallet.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.fragments.TransactionsFragmentDirections
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransfersDetailsAdapter(private val context: Context) :
    RecyclerView.Adapter<TransfersDetailsAdapter.ViewHolder>() {

    private val mComparator =
        Comparator<TransferDetail> { a, b ->
            getTransferNumber(b.id).compareTo(getTransferNumber(a.id))
        }

    /** A transferId has the format 1.11.x where x is the identifier of the transfer, this identifier is converted
     * to Long and returned */
    private fun getTransferNumber(transferId: String): Long {
        val transferNumber = transferId.split(".").last()
        return transferNumber.toLong()
    }

    private val mSortedList =
        SortedList<TransferDetail>(TransferDetail::class.java, object : SortedList.Callback<TransferDetail>() {
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

            override fun compare(a: TransferDetail, b: TransferDetail): Int {
                return mComparator.compare(a, b)
            }

            override fun areContentsTheSame(oldItem: TransferDetail, newItem: TransferDetail): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(item1: TransferDetail, item2: TransferDetail): Boolean {
                return item1.id == item2.id
            }
        })

    private val dateFormat: SimpleDateFormat
    private val timeFormat: SimpleDateFormat

    init {
        val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
        dateFormat = SimpleDateFormat("dd MMM, yyyy", locale)
        timeFormat = SimpleDateFormat("HH:mm:ss z", locale)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootView: ConstraintLayout  = itemView.findViewById(R.id.rootView)
        val vPaymentDirection: View     = itemView.findViewById(R.id.vPaymentDirection)
        val tvFrom: TextView            = itemView.findViewById(R.id.tvFrom)
        val ivDirectionArrow: ImageView = itemView.findViewById(R.id.ivDirectionArrow)
        val tvTo: TextView              = itemView.findViewById(R.id.tvTo)
        val llMemo: LinearLayout        = itemView.findViewById(R.id.llMemo)
        val tvMemo: TextView            = itemView.findViewById(R.id.tvMemo)
        val tvDate: TextView            = itemView.findViewById(R.id.tvDate)
        val tvTime: TextView            = itemView.findViewById(R.id.tvTime)
        val tvCryptoAmount: TextView    = itemView.findViewById(R.id.tvCryptoAmount)
        val tvFiatEquivalent: TextView  = itemView.findViewById(R.id.tvFiatEquivalent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransfersDetailsAdapter.ViewHolder {
        val inflater = LayoutInflater.from(context)

        val transactionView = inflater.inflate(R.layout.item_transaction, parent, false)

        return ViewHolder(transactionView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val transferDetail = mSortedList.get(position)

        viewHolder.vPaymentDirection.setBackgroundColor(ContextCompat.getColor(context,
            if(transferDetail.direction) R.color.colorReceive else R.color.colorSend
        ))

        viewHolder.tvFrom.text = transferDetail.from ?: ""
        viewHolder.tvTo.text = transferDetail.to ?: ""

        // Shows memo if available
        val memo = transferDetail.memo
        if (memo == "") {
            viewHolder.tvMemo.text = ""
            viewHolder.llMemo.visibility = View.GONE
        } else {
            viewHolder.tvMemo.text = memo
            viewHolder.llMemo.visibility = View.VISIBLE
        }

        // Format date and time
        val date = Date(transferDetail.date * 1000)

        viewHolder.tvDate.text = dateFormat.format(date)
        viewHolder.tvTime.text = timeFormat.format(date)

        // Show the crypto amount correctly formatted
        // TODO lift the DecimalFormat declaration to other place to make things more efficient
        val df = DecimalFormat("####."+("#".repeat(transferDetail.assetPrecision)))
        df.roundingMode = RoundingMode.CEILING
        df.decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())

        val amount = transferDetail.assetAmount.toDouble() /
                Math.pow(10.0, transferDetail.assetPrecision.toDouble())
        val cryptoAmount = "${df.format(amount)} ${transferDetail.getUIAssetSymbol()}"
        viewHolder.tvCryptoAmount.text = cryptoAmount

        // Fiat equivalent
        if (transferDetail.fiatAmount != null && transferDetail.fiatSymbol != null) {
            val numberFormat = NumberFormat.getNumberInstance()
            val currency = Currency.getInstance(transferDetail.fiatSymbol)
            val fiatEquivalent = transferDetail.fiatAmount.toDouble() /
                    Math.pow(10.0, currency.defaultFractionDigits.toDouble())

            val equivalentValue = "${numberFormat.format(fiatEquivalent)} ${currency.currencyCode}"
            viewHolder.tvFiatEquivalent.text = equivalentValue
        } else {
            viewHolder.tvFiatEquivalent.text = "-"
        }

        // Give the correct direction arrow color depending on the direction of the transaction
        viewHolder.ivDirectionArrow.setImageDrawable(context.getDrawable(
            if(transferDetail.direction) R.drawable.ic_arrow_receive else R.drawable.ic_arrow_send
        ))

        // Open the eReceipt when a transaction is tapped
        viewHolder.rootView.setOnClickListener { v ->
            val action = TransactionsFragmentDirections.eReceiptAction(transferDetail.id)
            try {
                v.findNavController().navigate(action)
            } catch (ignored: IllegalArgumentException) {
                // This exception is being ignored because it happens when a user taps two or
                // more items on the list at the same time, only the first navigates correctly and
                // the remaining produce an exception.
                Log.d("TransfersDetailsAdapter", "Ignoring IllegalArgumentException")
            }
        }
    }

    fun add(transferDetail: TransferDetail) {
        mSortedList.add(transferDetail)
    }

    fun remove(transferDetail: TransferDetail) {
        mSortedList.remove(transferDetail)
    }

    fun add(transfersDetails: List<TransferDetail>) {
        mSortedList.addAll(transfersDetails)
    }

    fun remove(transfersDetails: List<TransferDetail>) {
        mSortedList.beginBatchedUpdates()
        for (transferDetail in transfersDetails) {
            mSortedList.remove(transferDetail)
        }
        mSortedList.endBatchedUpdates()
    }

    fun replaceAll(transfersDetails: List<TransferDetail>) {
        mSortedList.beginBatchedUpdates()
        for (i in mSortedList.size() - 1 downTo 0) {
            val transferDetail = mSortedList.get(i)
            if (!transfersDetails.contains(transferDetail)) {
                mSortedList.remove(transferDetail)
            }
        }
        mSortedList.addAll(transfersDetails)
        mSortedList.endBatchedUpdates()
    }

    override fun getItemCount(): Int {
        return mSortedList.size()
    }
}
