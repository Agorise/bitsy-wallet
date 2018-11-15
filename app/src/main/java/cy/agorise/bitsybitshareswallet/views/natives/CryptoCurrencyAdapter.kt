package cy.agorise.bitsybitshareswallet.views.natives

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.models.CryptoCurrency

class CryptoCurrencyAdapter(context: Context, resource: Int, private val data: List<CryptoCurrency>) :
    ArrayAdapter<CryptoCurrency>(context, resource, data) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }

    /*
     * Creates the view for every element of the spinner
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflater.inflate(R.layout.crypto_currency_adapter_item, parent, false)
        val tvCryptoCurrencyName:TextView = v.findViewById(R.id.tvCryptoCurrencyName)

        val cryptoCurrency = getItem(position)
        tvCryptoCurrencyName.setText(cryptoCurrency!!.name)

        return v
    }
}
