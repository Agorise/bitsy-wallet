package cy.agorise.bitsybitshareswallet.fragments

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.viewmodels.MerchantsViewModel
import kotlinx.android.synthetic.main.fragment_merchants.*

class MerchantsFragment : Fragment() {

    companion object {
        fun newInstance() = MerchantsFragment()
    }

    private lateinit var viewModel: MerchantsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_merchants, container, false)

        var edtText:EditText = rootView.findViewById(R.id.edtText)

        val ft = activity!!.getSupportFragmentManager().beginTransaction()
        var mapFragment:MapFragment = MapFragment()
        mapFragment.edtText = edtText
        ft.replace(R.id.map, mapFragment)
        ft.commit()

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MerchantsViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
