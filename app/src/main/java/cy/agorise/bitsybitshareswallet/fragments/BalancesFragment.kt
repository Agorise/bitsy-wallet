package cy.agorise.bitsybitshareswallet.fragments

import androidx.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.activities.QRCodeActivity
import cy.agorise.bitsybitshareswallet.activities.SendTransactionActivity
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.BalancesViewModel
import de.bitshares_munich.smartcoinswallet.ReceiveTransactionActivity
import kotlinx.android.synthetic.main.fragment_balances.*

class BalancesFragment : Fragment() {

    companion object {
        fun newInstance() = BalancesFragment()
    }

    private lateinit var viewModel: BalancesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_balances, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BalancesViewModel::class.java)
        // TODO: Use the ViewModel

        // Sets the theme to night mode if it has been selected by the user
        if (PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)
        ) {
            activity!!.setTheme(R.style.AppTheme_Dark)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnReceive.setOnClickListener {
            val intent = Intent(view.context, ReceiveTransactionActivity::class.java)
            intent.putExtra("to","dtvvdtvv-12345") //Testing porpouse, fix it
            intent.putExtra("account_id","1") //Testing porpouse, fix it
            intent.putExtra("price","0")
            startActivity(intent)
        }

        btnSend.setOnClickListener {
            val intent = Intent(view.context, SendTransactionActivity::class.java)
            startActivity(intent)
        }

        btnReadQR.setOnClickListener {
            val intent = Intent(view.context, QRCodeActivity::class.java)
            intent.putExtra("id", 1)
            startActivity(intent)
        }
    }

}
