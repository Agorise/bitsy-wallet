package cy.agorise.bitsybitshareswallet.views.natives

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.PagedListAdapter
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.models.CryptoCoinTransactionExtended

class TransactionListAdapter(internal var fragment: Fragment) :
    PagedListAdapter<CryptoCoinTransactionExtended, TransactionViewHolder>(CryptoCoinTransactionExtended.DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.transaction_list_item, parent, false)


        return TransactionViewHolder(v, this.fragment)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        if (transaction != null) {
            holder.bindTo(transaction)
        } else {
            holder.clear()
        }
    }
}