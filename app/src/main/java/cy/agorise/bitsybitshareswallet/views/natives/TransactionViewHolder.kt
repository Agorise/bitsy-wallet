package cy.agorise.bitsybitshareswallet.views.natives

import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.models.CryptoCoinTransactionExtended
import cy.agorise.bitsybitshareswallet.models.GeneralSetting
import cy.agorise.bitsybitshareswallet.viewmodels.GeneralSettingListViewModel
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewHolder(itemView: View, private val fragment: Fragment) : RecyclerView.ViewHolder(itemView) {
    /*
     * The view holding the transaction "from"
     */
    private val tvFrom: TextView
    /*
     * The view holding the transaction "to"
     */
    private val tvTo: TextView
    /*
     * The view holding the transaction amount
     */
    private val tvAmount: TextView
    private val tvEquivalent: TextView
    private val tvTransactionDate: TextView
    private val tvTransactionHour: TextView
    private val rootView: View

    private var cryptoCoinTransactionId: Long = 0

    init {
        //TODO: use ButterKnife to load this
        this.cryptoCoinTransactionId = -1

        rootView = itemView.findViewById(R.id.rlTransactionItem)
        tvFrom = itemView.findViewById(R.id.fromText) as TextView
        tvTo = itemView.findViewById(R.id.toText) as TextView
        tvAmount = itemView.findViewById(R.id.tvAmount) as TextView
        tvEquivalent = itemView.findViewById(R.id.tvEquivalent) as TextView
        tvTransactionDate = itemView.findViewById(R.id.tvTransactionDate) as TextView
        tvTransactionHour = itemView.findViewById(R.id.tvTransactionHour) as TextView

        rootView.setOnClickListener { ereceiptOfThisTransaction() }
    }

    /*
     * dispatch the user to the receipt activity using this transaction
     */
    fun ereceiptOfThisTransaction() {
        //if the transaction was loaded
        if (this.cryptoCoinTransactionId >= 0) {
            /*val context = fragment.getContext()
            val startActivity = Intent()
            startActivity.setClass(context, CryptoCoinTransactionReceiptActivity::class.java!!)
            startActivity.action = CryptoCoinTransactionReceiptActivity::class.java!!.getName()
            //Pass the transaction id as an extra parameter to the receipt activity
            startActivity.putExtra("CRYPTO_COIN_TRANSACTION_ID", this.cryptoCoinTransactionId)
            startActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            context!!.startActivity(startActivity)*/
        }
    }

    /*
     * Clears all info in this element view
     */
    fun clear() {
        tvFrom.text = "loading..."
        tvTo.text = ""
        tvAmount.text = ""
        tvEquivalent.text = ""
        tvTransactionDate.text = ""
        tvTransactionHour.text = ""
    }

    /*
     * Binds a transaction object with this element view
     */
    fun bindTo(transaction: CryptoCoinTransactionExtended?/*, final OnTransactionClickListener listener*/) {
        if (transaction == null) {
            clear()
        } else {
            /*------this.cryptoCoinTransactionId = transaction!!.id
            val cryptoCurrencyViewModel = ViewModelProviders.of(this.fragment).get(CryptoCurrencyViewModel::class.java)
            val cryptoCurrency = cryptoCurrencyViewModel.getCryptoCurrencyById(transaction!!.idCurrency)
            val cryptoNetAccountViewModel =
                ViewModelProviders.of(this.fragment).get(CryptoNetAccountViewModel::class.java)
            cryptoNetAccountViewModel.loadCryptoNetAccount(transaction!!.accountId)

            val amountString =
                String.format("%.2f", transaction!!.amount / Math.pow(10.0, cryptoCurrency.getPrecision()))

            val generalSettingListViewModel =
                ViewModelProviders.of(this.fragment).get(GeneralSettingListViewModel::class.java)
            val timeZoneSetting =
                generalSettingListViewModel.getGeneralSettingByName(GeneralSetting.SETTING_NAME_TIME_ZONE)

            val userTimeZone: TimeZone
            if (timeZoneSetting != null) {
                userTimeZone = TimeZone.getTimeZone(timeZoneSetting!!.value)
            } else {
                userTimeZone = TimeZone.getTimeZone("cet")
            }

            val dateFormat = SimpleDateFormat("dd MMM")
            dateFormat.timeZone = userTimeZone
            val hourFormat = SimpleDateFormat("HH:mm:ss")
            hourFormat.timeZone = userTimeZone

            tvTransactionDate.text = dateFormat.format(transaction!!.date)
            tvTransactionHour.text = hourFormat.format(transaction!!.date)

            tvFrom.setText(transaction!!.from)
            tvTo.setText(transaction!!.to)

            val cryptoNetAccountLiveData = cryptoNetAccountViewModel.getCryptoNetAccount()---------*/

            //cryptoNetAccountLiveData.observe(this.fragment, new Observer<CryptoNetAccount>() {
            //    @Override
            //    public void onChanged(@Nullable CryptoNetAccount cryptoNetAccount) {
            /*---------if (transaction!!.input) {
                tvTo.setText(transaction!!.userAccountName)

                if (transaction!!.contactName != null && !transaction!!.equals("")) {
                    tvFrom.setText(transaction!!.contactName)
                }
            } else {
                tvFrom.setText(transaction!!.userAccountName)

                if (transaction!!.contactName != null && !transaction!!.equals("")) {
                    tvTo.setText(transaction!!.contactName)
                }
            }----------*/
            //    }
            //});

            /*-----var finalAmountText = ""
            if (transaction!!.input) {
                tvAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.green))
                finalAmountText = ("+ " + amountString
                        + " "
                        + cryptoCurrency.getName())
            } else {
                tvAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.red))
                finalAmountText = (amountString
                        + " "
                        + cryptoCurrency.getName())
            }
            tvAmount.text = finalAmountText----------*/
            //This will load the transaction receipt when the user clicks this view
            /*itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onUserClick(user);
                }
            });*/
        }
    }
}
