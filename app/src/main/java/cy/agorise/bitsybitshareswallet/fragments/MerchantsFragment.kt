package cy.agorise.bitsybitshareswallet.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.viewmodels.MerchantsViewModel
import kotlinx.android.synthetic.main.fragment_merchants.*

class MerchantsFragment : Fragment() {

    private val REQUEST_LOCATION_PERMISSION = 1

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


        /*
         * Check for ACCESS_FINE_LOCATION permission
         * */
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkPermission()) {
                // Code for above or equal 23 API Oriented Device
                // Your Permission granted already .Do next code

                val ft = activity!!.getSupportFragmentManager().beginTransaction()
                var mapFragment:MapFragment = MapFragment()
                mapFragment.edtText = edtText
                ft.replace(R.id.map, mapFragment)
                ft.commit()

            } else {
                requestPermission() // Code for permission
            }
        } else {

            // Code for Below 23 API Oriented Device
            // Do next code

            val ft = activity!!.getSupportFragmentManager().beginTransaction()
            var mapFragment:MapFragment = MapFragment()
            mapFragment.edtText = edtText
            ft.replace(R.id.map, mapFragment)
            ft.commit()

        }

        return rootView
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this!!.activity!!, Manifest.permission.ACCESS_FINE_LOCATION)
        return if (result == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            false
        }
    }

    private fun requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this!!.activity!!, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(activity, activity!!.getString(R.string.permission_denied_map), Toast.LENGTH_LONG).show()

            /*
             * Disable the button of the ACCESS_FINE_LOCATION visibility
             * */
            //disableVisibilityCamera()

        } else {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {

            /*
                 * Check for ACCESS_FINE_LOCATION permission
                 * */
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkPermission()) {
                    val ft = activity!!.getSupportFragmentManager().beginTransaction()
                    var mapFragment:MapFragment = MapFragment()
                    mapFragment.edtText = edtText
                    ft.replace(R.id.map, mapFragment)
                    ft.commit()

                } else {
                    requestPermission() // Code for permission
                }
            } else {

                // Code for Below 23 API Oriented Device
                // Do next code

                val ft = activity!!.getSupportFragmentManager().beginTransaction()
                var mapFragment:MapFragment = MapFragment()
                mapFragment.edtText = edtText
                ft.replace(R.id.map, mapFragment)
                ft.commit()

            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MerchantsViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
