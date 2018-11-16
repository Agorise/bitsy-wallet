package cy.agorise.bitsybitshareswallet.views.natives

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import cy.agorise.bitsybitshareswallet.R

class TransactionOrderSpinnerAdapter(
    context: Context,
    resource: Int,
    private val data: List<TransactionOrderSpinnerItem>
) :
    ArrayAdapter<TransactionOrderSpinnerAdapter.TransactionOrderSpinnerItem>(context, resource, data) {
    class TransactionOrderSpinnerItem(val field: String, val label: String, var order: Int, var ascending: Boolean)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = getView(position, convertView, parent)
        v.visibility = View.VISIBLE
        return v
    }

    /*
     * Creates the view for every element of the spinner
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflater.inflate(R.layout.transactions_order_adapter_item, parent, false)
        val tvTransactionOrderLabel: TextView = v.findViewById(R.id.tvTransactionOrderLabel)

        val transactionOrderSpinnerItem = getItem(position)
        tvTransactionOrderLabel.setText(transactionOrderSpinnerItem!!.label)

        v.setVisibility(View.GONE)
        return v
    }
}
