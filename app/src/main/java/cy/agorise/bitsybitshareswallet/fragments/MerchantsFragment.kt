package cy.agorise.bitsybitshareswallet.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.maps.android.MarkerManager
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.jakewharton.rxbinding3.appcompat.queryTextChangeEvents
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import cy.agorise.bitsybitshareswallet.database.entities.Teller
import cy.agorise.bitsybitshareswallet.databinding.FragmentMerchantsBinding
import cy.agorise.bitsybitshareswallet.models.MapObject
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.MerchantClusterRenderer
import cy.agorise.bitsybitshareswallet.utils.TellerClusterRenderer
import cy.agorise.bitsybitshareswallet.utils.toast
import cy.agorise.bitsybitshareswallet.viewmodels.MerchantViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger
import java.util.concurrent.TimeUnit


class MerchantsFragment : Fragment(), OnMapReadyCallback, SearchView.OnSuggestionListener {

    companion object {
        private const val TAG = "MerchantsFragment"

        // Camera Permission
        private const val REQUEST_LOCATION_PERMISSION = 1

        // SearchView suggestions
        private const val SUGGEST_COLUMN_ID = "_id"
        private const val SUGGEST_COLUMN_LAT = "suggest_lat"
        private const val SUGGEST_COLUMN_LON = "suggest_lon"
        private const val SUGGEST_COLUMN_NAME = "suggest_name"
        private const val SUGGEST_COLUMN_ADDRESS = "suggest_address"
        private const val SUGGEST_COLUMN_IS_MERCHANT = "suggest_is_merchant"
        private const val SUGGEST_COLUMN_IMAGE_RESOURCE = "suggest_image_resource"
    }

    private val viewModel: MerchantViewModel by viewModels()

    private var _binding: FragmentMerchantsBinding? = null
    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null

    private var mMarkerManager: MarkerManager? = null

    /** Keeps track of all RxJava disposables, to make sure they are all disposed when the fragment is destroyed */
    private var mDisposables = CompositeDisposable()

    private var mSearchView: SearchView? = null

    // Cluster managers to create custom merchants and tellers clusters with a custom behavior too
    private var mMerchantClusterManager: ClusterManager<Merchant>? = null
    private var mTellerClusterManager: ClusterManager<Teller>? = null

    // Variables to keep track of the currently selected merchant and teller
    private var selectedMerchant: Merchant? = null
    private var selectedTeller: Teller? = null

    // Variables used to create a custom popup menu to show the merchants and tellers switches
    private var mPopupWindow: PopupWindow? = null
    private var screenWidth: Int = 0

    // Variables used to decide whether or not to display the merchants and tellers markers on the map
    private var merchants = ArrayList<Merchant>()
    private var tellers = ArrayList<Teller>()
    private var showMerchantsMarkers = true
    private var showTellerMarkers = true

    // Variables used to dynamically obtain the status bar and navigation bar height, to automatically and correctly
    // place the Toolbar and Map UI controllers
    private var statusBarSize = 0
    private var navigationBarSize = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Hide the activity's Toolbar so that we can make the trick of the translucent navigation and status bars
        val activityToolbar: Toolbar? = activity?.findViewById(R.id.toolbar)
        activityToolbar?.visibility = View.GONE

        // Sets the Navigation and Status bars translucent so that the map can be viewed through them
        val window = activity?.window
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        )

        _binding = FragmentMerchantsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        // Dynamically obtain status bar and navigation bar heights, and account for the status bar height to add
        // the correct top margin to the Toolbar and place it just below the status bar
        view.setOnApplyWindowInsetsListener { v, insets ->
            statusBarSize = insets.systemWindowInsetTop
            navigationBarSize = insets.systemWindowInsetBottom
            val layoutParams = binding.toolbar.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = statusBarSize
            insets
        }

        // Set the fragment's toolbar as the activity toolbar just for this fragment
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        binding.toolbar.setOnClickListener { dismissPopupWindow() }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupPopupWindow()

        // Gets the screen width to correctly place the merchants and tellers popup menu
        val display = activity?.windowManager?.defaultDisplay
        screenWidth = display?.width ?: screenWidth
    }

    private fun setupPopupWindow() {
        val popupView = layoutInflater.inflate(R.layout.popup_menu_merchants, null, false)

        val switchMerchants = popupView?.findViewById<SwitchCompat>(R.id.switchMerchants)
        switchMerchants?.isChecked = showMerchantsMarkers
        switchMerchants?.setOnCheckedChangeListener { _, isChecked ->
            showMerchantsMarkers = isChecked
            showHideMerchantsMarkers()
        }

        val switchTellers = popupView?.findViewById<SwitchCompat>(R.id.switchTellers)
        switchTellers?.isChecked = showTellerMarkers
        switchTellers?.setOnCheckedChangeListener { _, isChecked ->
            showTellerMarkers = isChecked
            showHideTellersMarkers()
        }

        val tvAbout = popupView?.findViewById<TextView>(R.id.tvAbout)
        tvAbout?.setOnClickListener {
            val context = context ?: return@setOnClickListener
            MaterialDialog(context).show {
                customView(R.layout.dialog_merchants_tellers_info, scrollable = true)
                positiveButton(android.R.string.ok) { dismiss() }
            }
        }

        mPopupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_merchants, menu)

        // Adds listener for the SearchView
        val searchItem = menu.findItem(R.id.menu_search)
        mSearchView = searchItem.actionView as SearchView
        mSearchView?.suggestionsAdapter = SimpleCursorAdapter(
            context, R.layout.item_merchant_suggestion, null,
            arrayOf(SUGGEST_COLUMN_NAME, SUGGEST_COLUMN_ADDRESS, SUGGEST_COLUMN_IMAGE_RESOURCE),
            intArrayOf(R.id.tvName, R.id.tvAddress, R.id.ivMarkerPin)
        )

        // Add listener to changes in the SearchView's text to update the suggestions
        mSearchView?.queryTextChangeEvents()
            ?.skipInitialValue()
            ?.debounce(200, TimeUnit.MILLISECONDS)
            ?.map { it.queryText.toString().toLowerCase() }
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe {
                if (it.length < 2)
                    mSearchView?.suggestionsAdapter?.changeCursor(null)
                else
                    updateSearchViewSuggestions(it)
            }?.let {
                mDisposables.add(it)
            }

        mSearchView?.setOnSuggestionListener(this)

        mSearchView?.setOnSearchClickListener { dismissPopupWindow() }

        // Adjust SearchView width to avoid pushing other menu items out of the screen
        mSearchView?.maxWidth = screenWidth * 7 / 10
    }

    private fun updateSearchViewSuggestions(query: String) {
        // Obtain observable of the list of merchants matching the query
        val merchantsObs = viewModel.queryMerchants(query)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).toObservable()

        // Obtain observable of the list of tellers matching the query
        val tellerObs = viewModel.queryTellers(query)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).toObservable()

        //Combine the results of both the merchants and teller queries
        mDisposables.add(Observable.zip(merchantsObs, tellerObs,
            BiFunction<List<Merchant>, List<Teller>, List<MapObject>> { t1, t2 ->
                val mapObjects = ArrayList<MapObject>()

                // Show merchant suggestions only if merchants are enabled
                if (showMerchantsMarkers) {
                    for (merchant in t1) {
                        val mapObject = MapObject(
                            merchant._id,
                            merchant.lat,
                            merchant.lon,
                            merchant.name,
                            merchant.address,
                            1
                        )
                        mapObjects.add(mapObject)
                    }
                }

                // Show teller suggestions only if tellers are enabled
                if (showTellerMarkers) {
                    for (teller in t2) {
                        val mapObject = MapObject(
                            teller._id,
                            teller.lat,
                            teller.lon,
                            teller.gt_name,
                            teller.address,
                            0
                        )
                        mapObjects.add(mapObject)
                    }
                }

                mapObjects
            }
        ).subscribe({ mapObjects ->
            run {
                Log.d(TAG, "list with ${mapObjects.size} elements")
                val cursor = MatrixCursor(
                    arrayOf(
                        SUGGEST_COLUMN_ID,
                        SUGGEST_COLUMN_LAT,
                        SUGGEST_COLUMN_LON,
                        SUGGEST_COLUMN_NAME,
                        SUGGEST_COLUMN_ADDRESS,
                        SUGGEST_COLUMN_IS_MERCHANT,
                        SUGGEST_COLUMN_IMAGE_RESOURCE
                    )
                )
                for (mapObject in mapObjects) {
                    cursor.addRow(
                        arrayOf(
                            BigInteger(mapObject._id, 16).toLong(),
                            mapObject.lat,
                            mapObject.lon,
                            mapObject.name,
                            mapObject.address,
                            mapObject.isMerchant,
                            if (mapObject.isMerchant == 1) R.drawable.ic_merchant_pin else R.drawable.ic_teller_pin
                        )
                    )
                }
                mSearchView?.suggestionsAdapter?.changeCursor(cursor)
            }
        },
            { error ->
                val message = "Error while retrieving autocomplete suggestions. Msg: $error"
                Log.e(TAG, message)
            })
        )
    }

    override fun onSuggestionSelect(position: Int): Boolean {
        return onSuggestionClick(position)
    }

    override fun onSuggestionClick(position: Int): Boolean {
        val cursor = mSearchView?.suggestionsAdapter?.getItem(position) as Cursor?
        val id = cursor?.getString(cursor.getColumnIndex(SUGGEST_COLUMN_ID))
        val lat = cursor?.getString(cursor.getColumnIndex(SUGGEST_COLUMN_LAT))?.toDoubleOrNull()
        val lon = cursor?.getString(cursor.getColumnIndex(SUGGEST_COLUMN_LON))?.toDoubleOrNull()
        val name = cursor?.getString(cursor.getColumnIndex(SUGGEST_COLUMN_NAME)) ?: ""
        val isMerchant = cursor?.getInt(cursor.getColumnIndex(SUGGEST_COLUMN_IS_MERCHANT))
        cursor?.close()

        if (lat != null && lon != null) {
            try {
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 15f))
            } catch (e: Exception) {
                Log.d(TAG, e.message)
            }
        }

        mSearchView?.clearFocus()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_filter) {
            // Try to show or dismiss the custom popup window with the merchants and tellers switches
            if (mPopupWindow?.isShowing == false) {
                mPopupWindow?.showAsDropDown(binding.toolbar, screenWidth, 8)
                if (mMap?.isMyLocationEnabled == true)
                    mMap?.uiSettings?.isMyLocationButtonEnabled = false
            } else
                dismissPopupWindow()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /** Handles the result from the location permission request */
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                mMap?.isMyLocationEnabled = true
            } else {
                context?.toast(getString(R.string.msg__location_permission_necessary))
            }
            return
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add padding to move the controls out of the toolbar/status bar and navigation bar.
        mMap?.setPadding(0, binding.toolbar.height + statusBarSize, 0, navigationBarSize)

        applyMapTheme()

        verifyLocationPermission()

        // User MarkerManager to be able to use Map events in more than one ClusterManager
        mMarkerManager = MarkerManager(mMap)

        initMerchantsCluster()

        initTellersCluster()

        // Point the map's listeners at the listeners implemented by the marker manager.
        mMap?.setOnMarkerClickListener(mMarkerManager)

        mMap?.setOnCameraIdleListener {
            mMerchantClusterManager?.onCameraIdle()
            mTellerClusterManager?.onCameraIdle()
        }

        mMap?.setInfoWindowAdapter(mMarkerManager)

        // Try to dismiss the
        mMap?.setOnMapClickListener {
            dismissPopupWindow()
        }
    }

    private fun applyMapTheme() {
        val nightMode = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)

        if (nightMode) {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = mMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context, R.raw.map_style_night
                )
            )

            if (success != null && success != false) {
                Log.e(TAG, "Style parsing failed.")
            }
        }
    }

    private fun verifyLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not already granted
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // Permission is already granted
            mMap?.isMyLocationEnabled = true
        }
    }

    private fun initMerchantsCluster() {
        // Setup clusters to group markers when possible using a custom renderer
        mMerchantClusterManager = ClusterManager(context, mMap, mMarkerManager)
        val merchantRenderer = MerchantClusterRenderer(context, mMap, mMerchantClusterManager)
        mMerchantClusterManager?.renderer = merchantRenderer

        mMerchantClusterManager?.setOnClusterClickListener {
            dismissPopupWindow()
            onClusterClick(it as Cluster<ClusterItem>)
        }
        mMerchantClusterManager?.setOnClusterItemClickListener { merchant ->
            dismissPopupWindow()
            selectedMerchant = merchant
            false
        }

        // Force marker to use a custom info window
        mMerchantClusterManager?.markerCollection?.setOnInfoWindowAdapter(MerchantInfoWindowAdapter())

        viewModel.getAllMerchants().observe(viewLifecycleOwner, { merchants ->
            this.merchants.clear()
            this.merchants.addAll(merchants)
            showHideMerchantsMarkers()
        })
    }

    private fun initTellersCluster() {
        // Setup clusters to group markers when possible using a custom renderer
        mTellerClusterManager = ClusterManager(context, mMap, mMarkerManager)
        val tellerRenderer = TellerClusterRenderer(context, mMap, mTellerClusterManager)
        mTellerClusterManager?.renderer = tellerRenderer

        mTellerClusterManager?.setOnClusterClickListener {
            dismissPopupWindow()
            onClusterClick(it as Cluster<ClusterItem>)
        }
        mTellerClusterManager?.setOnClusterItemClickListener { teller ->
            dismissPopupWindow()
            selectedTeller = teller
            false
        }

        // Force marker to use a custom info window
        mTellerClusterManager?.markerCollection?.setOnInfoWindowAdapter(TellerInfoWindowAdapter())

        viewModel.getAllTellers().observe(viewLifecycleOwner, { tellers ->
            this.tellers.clear()
            this.tellers.addAll(tellers)
            showHideTellersMarkers()
        })
    }

    private fun showHideMerchantsMarkers() {
        mMerchantClusterManager?.clearItems()
        mMerchantClusterManager?.cluster()
        if (showMerchantsMarkers) {
            mMerchantClusterManager?.addItems(merchants)
            mMerchantClusterManager?.cluster()
        }
    }

    private fun showHideTellersMarkers() {
        mTellerClusterManager?.clearItems()
        mTellerClusterManager?.cluster()
        if (showTellerMarkers) {
            mTellerClusterManager?.addItems(tellers)
            mTellerClusterManager?.cluster()
        }
    }

    /** Animates the camera update to focus on an area that shows all the items from the cluster that was tapped. */
    private fun onClusterClick(cluster: Cluster<ClusterItem>?): Boolean {
        val builder = LatLngBounds.builder()
        val items = cluster?.items

        if (items != null) {
            for (item in items) {
                val position = item.position
                builder.include(position)
            }

            val bounds = builder.build()

            try {
                mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                Log.d(TAG, e.message)
            }
        }
        return true
    }

    /** Creates a custom view for the Merchant's Info Window, when a merchant marker is selected */
    inner class MerchantInfoWindowAdapter : GoogleMap.InfoWindowAdapter {

        override fun getInfoWindow(marker: Marker?): View {
            val infoWindowLayout: View = LayoutInflater.from(context)
                .inflate(R.layout.marker_merch_info_window, null)

            val tvName = infoWindowLayout.findViewById<TextView>(R.id.tvName)
            val tvAddress = infoWindowLayout.findViewById<TextView>(R.id.tvAddress)
            val tvPhone = infoWindowLayout.findViewById<TextView>(R.id.tvPhone)
            val tvTelegram = infoWindowLayout.findViewById<TextView>(R.id.tvTelegram)
            val tvWebsite = infoWindowLayout.findViewById<TextView>(R.id.tvWebsite)

            if (selectedMerchant != null) {
                tvName.text = selectedMerchant?.name

                if (!selectedMerchant?.address.equals(""))
                    tvAddress.text = selectedMerchant?.address
                else
                    tvAddress.visibility = View.GONE

                if (!selectedMerchant?.phone.equals(""))
                    tvPhone.text = selectedMerchant?.phone
                else
                    tvPhone.visibility = View.GONE

                if (!selectedMerchant?.telegram.equals("")) {
                    val telegram = "Telegram: ${selectedMerchant?.telegram}"
                    tvTelegram.text = telegram
                } else
                    tvTelegram.visibility = View.GONE

                if (!selectedMerchant?.website.equals(""))
                    tvWebsite.text = selectedMerchant?.website
                        ?.removePrefix("http://")?.removePrefix("https://")
                else
                    tvWebsite.visibility = View.GONE
            }
            return infoWindowLayout
        }

        override fun getInfoContents(marker: Marker?): View? {
            return null
        }
    }

    /** Creates a custom view for the Teller's Info Window, when a teller marker is selected */
    inner class TellerInfoWindowAdapter : GoogleMap.InfoWindowAdapter {

        override fun getInfoWindow(marker: Marker?): View {
            val infoWindowLayout: View = LayoutInflater.from(context)
                .inflate(R.layout.marker_teller_info_window, null)

            val tvName = infoWindowLayout.findViewById<TextView>(R.id.tvName)
            val tvAddress = infoWindowLayout.findViewById<TextView>(R.id.tvAddress)
            val tvPhone = infoWindowLayout.findViewById<TextView>(R.id.tvPhone)
            val tvTelegram = infoWindowLayout.findViewById<TextView>(R.id.tvTelegram)
            val tvKeybase = infoWindowLayout.findViewById<TextView>(R.id.tvKeybase)
            val tvWhatsapp = infoWindowLayout.findViewById<TextView>(R.id.tvWhatsapp)
            val tvViber = infoWindowLayout.findViewById<TextView>(R.id.tvViber)
            val tvEmail = infoWindowLayout.findViewById<TextView>(R.id.tvEmail)
            val tvWebsite = infoWindowLayout.findViewById<TextView>(R.id.tvWebsite)

            if (selectedTeller != null) {
                tvName.text = selectedTeller?.gt_name

                if (!selectedTeller?.address.equals(""))
                    tvAddress.text = selectedTeller?.address
                else
                    tvAddress.visibility = View.GONE

                if (!selectedTeller?.phone.equals(""))
                    tvPhone.text = selectedTeller?.phone
                else
                    tvPhone.visibility = View.GONE

                if (!selectedTeller?.telegram.equals("")) {
                    val telegram = "Telegram: ${selectedTeller?.telegram}"
                    tvTelegram.text = telegram
                } else
                    tvTelegram.visibility = View.GONE

                if (!selectedTeller?.keybase.equals("")) {
                    val keybase = "Keybase: ${selectedTeller?.keybase}"
                    tvKeybase.text = keybase
                } else
                    tvKeybase.visibility = View.GONE

                if (!selectedTeller?.whatsapp.equals("")) {
                    val whatsapp = "WhatsApp: ${selectedTeller?.whatsapp}"
                    tvWhatsapp.text = whatsapp
                } else
                    tvWhatsapp.visibility = View.GONE

                if (!selectedTeller?.viber.equals("")) {
                    val viber = "Viber: ${selectedTeller?.viber}"
                    tvViber.text = viber
                } else
                    tvViber.visibility = View.GONE

                if (!selectedTeller?.email.equals("")) {
                    val email = "Email: ${selectedTeller?.email}"
                    tvEmail.text = email
                } else
                    tvEmail.visibility = View.GONE

                if (!selectedTeller?.url.equals(""))
                    tvWebsite.text = selectedTeller?.url
                        ?.removePrefix("http://")?.removePrefix("https://")
                else
                    tvWebsite.visibility = View.GONE
            }
            return infoWindowLayout
        }

        override fun getInfoContents(marker: Marker?): View? {
            return null
        }
    }

    private fun dismissPopupWindow() {
        if (mPopupWindow?.isShowing == true) {
            mPopupWindow?.dismiss()
            if (mMap?.isMyLocationEnabled == true)
                mMap?.uiSettings?.isMyLocationButtonEnabled = true
        }
    }

    override fun onPause() {
        super.onPause()

        dismissPopupWindow()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!mDisposables.isDisposed) mDisposables.dispose()
    }
}
