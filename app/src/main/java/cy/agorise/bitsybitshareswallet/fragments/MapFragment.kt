package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import cy.agorise.bitsybitshareswallet.R
import android.widget.LinearLayout
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.gson.GsonBuilder
import cy.agorise.bitsybitshareswallet.manager.FeathersResponse
import cy.agorise.bitsybitshareswallet.models.Merchant
import cy.agorise.bitsybitshareswallet.service.AmbassadorService
import kotlinx.android.synthetic.main.fragment_merchants.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import javax.security.auth.callback.Callback


class MapFragment: retrofit2.Callback<FeathersResponse<Merchant>>, SupportMapFragment(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var location: LatLng? = null
    private var merchants: List<Merchant>? = null

    var edtText: EditText? = null




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        
        val rootView = super.onCreateView(inflater, container, savedInstanceState)

        getMapAsync(this);

        edtText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

                mMap!!.clear()
                for (mer in merchants!!) {
                    if (mer.name!!.contains(charSequence)) {
                        location = LatLng(mer.lat.toDouble(), mer.lon.toDouble())
                        mMap!!.addMarker(
                            MarkerOptions().position(location!!).title(mer.name).snippet(mer.address).icon(
                                BitmapDescriptorFactory.fromResource(R.drawable.star)
                            )
                        )
                        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 2f))
                    }
                }
            }

            override fun afterTextChanged(editable: Editable) {

            }
        })
        
        
        return rootView
    }

    override fun onMapReady(p0: GoogleMap?) {

        mMap = p0
        
        // Posicionar el mapa en una localización y con un nivel de zoom
        val latLng = LatLng(36.679582, -5.444791)
        // Un zoom mayor que 13 hace que el emulador falle, pero un valor deseado para
        // callejero es 17 aprox.
        val zoom = 13f
        p0!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))

        // Colocar un marcador en la misma posición
        p0.addMarker(MarkerOptions().position(latLng))

        val gson = GsonBuilder()
            .setLenient()
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://ambpay.palmpay.io/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val ambassadorService = retrofit.create<AmbassadorService>(AmbassadorService::class.java!!)
        val call = ambassadorService.allMerchants
        call.enqueue(this)
    }

    override fun onResponse(call: Call<FeathersResponse<Merchant>>, response: Response<FeathersResponse<Merchant>>) {
        if (response.isSuccessful) {
            var res_: FeathersResponse<Merchant>? = response.body()
            merchants = res_!!.data
            for (mer in merchants!!) {
                location = LatLng(mer.lat.toDouble(), mer.lon.toDouble())
                mMap!!.addMarker(
                    MarkerOptions().position(location!!).title(mer.name).snippet(mer.address).icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.star)
                    )
                )
            }
            res_ = null
        } else {
            try {
                Log.e("error_bitsy", response.errorBody().string())
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    override fun onFailure(call: Call<FeathersResponse<Merchant>>, t: Throwable) {
        t.printStackTrace()
    }


}