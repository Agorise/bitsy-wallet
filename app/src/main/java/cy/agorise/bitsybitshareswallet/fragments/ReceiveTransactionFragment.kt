package cy.agorise.bitsybitshareswallet.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.AdapterView
import androidx.appcompat.widget.Toolbar
import androidx.collection.LongSparseArray
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.common.primitives.UnsignedLong
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jakewharton.rxbinding3.widget.textChanges
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.AssetsAdapter
import cy.agorise.bitsybitshareswallet.adapters.AutoSuggestAssetAdapter
import cy.agorise.bitsybitshareswallet.databinding.FragmentReceiveTransactionBinding
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.Helper
import cy.agorise.bitsybitshareswallet.utils.showKeyboard
import cy.agorise.bitsybitshareswallet.utils.toast
import cy.agorise.bitsybitshareswallet.viewmodels.ReceiveTransactionViewModel
import cy.agorise.graphenej.*
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.calls.ListAssets
import cy.agorise.graphenej.models.JsonRpcResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.min

class ReceiveTransactionFragment : ConnectedFragment() {

    companion object {
        private const val TAG = "ReceiveTransactionFrag"

        private const val RESPONSE_LIST_ASSETS = 1
        private const val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 100

        /** Number of assets to request from the NetworkService to show as suggestions in the AutoCompleteTextView */
        private const val AUTO_SUGGEST_ASSET_LIMIT = 5

        private const val OTHER_ASSET = "other_asset"
    }

    private var _binding: FragmentReceiveTransactionBinding? = null
    private val binding get() = _binding!!

    private lateinit var mViewModel: ReceiveTransactionViewModel

    /** Current user account */
    private var mUserAccount: UserAccount? = null

    private var mAsset: Asset? = null

    private var mAssetsAdapter: AssetsAdapter? = null

    private lateinit var mAutoSuggestAssetAdapter: AutoSuggestAssetAdapter

    private var mAssets = ArrayList<cy.agorise.bitsybitshareswallet.database.entities.Asset>()

    /** Keeps track of the current selected asset symbol */
    private var selectedAssetSymbol = "BTS"

    /** Used to avoid erasing the QR code when the user selects an item from the AutoComplete suggestions */
    private var selectedInAutoCompleteTextView = false

    // Map used to keep track of request and response id pairs
    private val responseMap = LongSparseArray<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        val nightMode = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)

        // Sets the toolbar background color to green
        val toolbar: Toolbar? = activity?.findViewById(R.id.toolbar)
        toolbar?.setBackgroundResource(if (!nightMode) R.color.colorReceive else R.color.colorToolbarDark)

        // Sets the status and navigation bars background color to a dark green or just dark
        val window = activity?.window
        context?.let { context ->
            val statusBarColor = ContextCompat.getColor(
                context,
                if (!nightMode) R.color.colorReceiveDark else R.color.colorStatusBarDark
            )
            window?.statusBarColor = statusBarColor
            window?.navigationBarColor = statusBarColor
        }

        _binding = FragmentReceiveTransactionBinding.inflate(inflater, container, false)
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

        // Configure ViewModel
        mViewModel = ViewModelProviders.of(this).get(ReceiveTransactionViewModel::class.java)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")

        mViewModel.getUserAccount(userId!!).observe(this,
            Observer<cy.agorise.bitsybitshareswallet.database.entities.UserAccount> { user ->
                mUserAccount = UserAccount(user.id, user.name)
            })

        mViewModel.getAllNonZero().observe(this,
            Observer<List<cy.agorise.bitsybitshareswallet.database.entities.Asset>> { assets ->
                mAssets.clear()
                mAssets.addAll(assets)

                // Add BTS to always show a QR
                if (mAssets.isEmpty())
                    mAssets.add(
                        cy.agorise.bitsybitshareswallet.database.entities.Asset(
                            "1.3.0", "BTS", 5, "", ""
                        )
                    )

                mAssets.sortWith(
                    Comparator { a, b -> a.toString().compareTo(b.toString(), true) }
                )

                // Add an option at the end so the user can search for an asset other than the ones saved in the db
                val asset = cy.agorise.bitsybitshareswallet.database.entities.Asset(
                    OTHER_ASSET, getString(R.string.text__other), 0, "", ""
                )
                mAssets.add(asset)

                mAssetsAdapter =
                    AssetsAdapter(context!!, android.R.layout.simple_spinner_item, mAssets)
                binding.spAsset.adapter = mAssetsAdapter

                // Try to select the selectedAssetSymbol
                for (i in 0 until mAssetsAdapter!!.count) {
                    if (mAssetsAdapter!!.getItem(i)!!.symbol == selectedAssetSymbol) {
                        binding.spAsset.setSelection(i)
                        break
                    }
                }
            })

        mViewModel.qrCodeBitmap.observe(this, Observer { bitmap ->
            binding.ivQR.setImageBitmap(bitmap)
        })

        binding.spAsset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mAssetsAdapter?.getItem(position)?.let { asset ->
                    if (asset.id == OTHER_ASSET) {
                        binding.tilAsset.visibility = View.VISIBLE
                        binding.actvAsset.showKeyboard()
                        mAsset = null
                    } else {
                        binding.tilAsset.visibility = View.GONE
                        selectedAssetSymbol = asset.symbol

                        mAsset = Asset(asset.id, asset.toString(), asset.precision)
                    }
                }

                updateQR()
            }
        }

        // Use RxJava Debounce to create QR code only after the user stopped typing an amount
        mDisposables.add(
            binding.tietAmount.textChanges()
                .debounce(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ updateQR() }, { mAsset = null })
        )

        // Add adapter to the Assets AutoCompleteTextView
        mAutoSuggestAssetAdapter =
            AutoSuggestAssetAdapter(context!!, android.R.layout.simple_dropdown_item_1line)
        binding.actvAsset.setAdapter(mAutoSuggestAssetAdapter)

        // Use RxJava Debounce to avoid making calls to the NetworkService on every text change event and also avoid
        // the first call when the View is created
        mDisposables.add(
            binding.actvAsset.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .map { it.toString().trim().toUpperCase() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (!selectedInAutoCompleteTextView) {
                        mAsset = null
                        updateQR()
                    }
                    selectedInAutoCompleteTextView = false

                    // Get a list of assets that match the already typed string by the user
                    if (it.length > 1 && mNetworkService != null) {
                        val id = mNetworkService?.sendMessage(
                            ListAssets(it, AUTO_SUGGEST_ASSET_LIMIT),
                            ListAssets.REQUIRED_API
                        )
                        if (id != null) responseMap.append(id, RESPONSE_LIST_ASSETS)
                    }
                }, { mAsset = null })
        )

        binding.actvAsset.setOnItemClickListener { parent, _, position, _ ->
            val asset =
                parent.adapter.getItem(position) as cy.agorise.bitsybitshareswallet.database.entities.Asset
            mAsset = Asset(asset.id, asset.toString(), asset.precision)
            selectedInAutoCompleteTextView = true
            updateQR()
        }
    }

    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {
        if (responseMap.containsKey(response.id)) {
            val responseType = responseMap[response.id]
            when (responseType) {
                RESPONSE_LIST_ASSETS -> handleListAssets(response.result)
            }
            responseMap.remove(response.id)
        }
    }

    override fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate) {
        if (connectionStatusUpdate.updateCode == ConnectionStatusUpdate.DISCONNECTED) {
            // If we got a disconnection notification, we should clear our response map, since
            // all its stored request ids will now be reset
            responseMap.clear()
        }
    }

    /**
     * Handles the list of assets returned from the node that correspond to what the user has typed in the Asset
     * AutoCompleteTextView and adds them to its adapter to show as suggestions.
     */
    private fun handleListAssets(result: Any?) {
        if (result is List<*> && result.isNotEmpty() && result[0] is Asset) {
            val assetList = result as List<Asset>
            Log.d(TAG, "handleListAssets")
            val assets = ArrayList<cy.agorise.bitsybitshareswallet.database.entities.Asset>()
            for (_asset in assetList) {
                val asset = cy.agorise.bitsybitshareswallet.database.entities.Asset(
                    _asset.objectId,
                    _asset.symbol,
                    _asset.precision,
                    _asset.description ?: "",
                    _asset.issuer ?: ""
                )

                assets.add(asset)
            }
            mAutoSuggestAssetAdapter.setData(assets)
            mAutoSuggestAssetAdapter.notifyDataSetChanged()
        }
    }

    private fun updateQR() {
        if (mAsset == null) {
            binding.ivQR.setImageDrawable(null)
            // TODO clean the please pay and to text at the bottom too
            return
        }

        val asset = mAsset!!

        // Try to obtain the amount from the Amount Text Field or make it zero otherwise
        val amount: Long = try {
            val tmpAmount = binding.tietAmount.text.toString().toDouble()
            (tmpAmount * Math.pow(10.0, asset.precision.toDouble())).toLong()
        } catch (e: Exception) {
            0
        }

        val total = AssetAmount(UnsignedLong.valueOf(amount), asset)
        val totalInDouble = Util.fromBase(total)
        val items = arrayOf(LineItem("transfer", 1, totalInDouble))
        val invoice = Invoice(
            mUserAccount?.name, "", "",
            asset.symbol.replaceFirst("bit", ""), items, "", ""
        )
        Log.d(TAG, "invoice: " + invoice.toJsonString())
        try {
            mViewModel.updateInvoice(invoice, min(binding.ivQR.width, binding.ivQR.height))
            updateAmountAddressUI(amount, asset.symbol, asset.precision, mUserAccount!!.name)
        } catch (e: NullPointerException) {
            Log.e(TAG, "NullPointerException. Msg: " + e.message)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    /**
     * Updates the UI to show the amount and account to send the payment
     */
    private fun updateAmountAddressUI(
        assetAmount: Long,
        assetSymbol: String,
        assetPrecision: Int,
        account: String
    ) {
        val txtAmount: String = if (assetAmount == 0L) {
            getString(R.string.template__please_send, getString(R.string.text__any_amount), " ")
        } else {
            val df = DecimalFormat("####." + ("#".repeat(assetPrecision)))
            df.roundingMode = RoundingMode.CEILING
            df.decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())

            val amount = assetAmount.toDouble() / Math.pow(10.toDouble(), assetPrecision.toDouble())
            val strAmount = df.format(amount)
            getString(R.string.template__please_send, strAmount, assetSymbol)
        }

        val txtAccount = getString(R.string.template__to, account)

        binding.tvPleasePay.text = txtAmount
        binding.tvTo.text = txtAccount
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_receive_transaction, menu)

        // Animate the share icon
        val shareIcon = menu.findItem(R.id.menu_share).icon
        if (shareIcon is Animatable) {
            shareIcon.start()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_share) {
            verifyStoragePermission()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun verifyStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                activity!!,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not already granted
            requestPermissions(
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
            )
        } else {
            // Permission is already granted
            shareQRScreenshot()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                shareQRScreenshot()
            } else {
                context?.toast(getString(R.string.msg__storage_permission_necessary_share))
            }
            return
        }
    }

    /**
     * This function takes a screenshot as a bitmap, saves it into a temporal cache image and then
     * sends an intent so the user can select the desired method to share the image.
     */
    private fun shareQRScreenshot() {
        // TODO improve, show errors where necessary so the user can fix it
        // Avoid sharing the QR code image if the fields are not filled correctly
        if (mAsset == null)
            return

        // Get Screenshot
        val screenshot = Helper.loadBitmapFromView(binding.container)
        val imageUri = Helper.saveTemporalBitmap(context!!, screenshot)

        // Prepare information for share intent
        val subject = getString(R.string.msg__invoice_subject, mUserAccount?.name)
        val content = binding.tvPleasePay.text.toString() + "\n" +
                binding.tvTo.text.toString()

        // Create share intent and call it
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        shareIntent.putExtra(Intent.EXTRA_TEXT, content)
        shareIntent.type = "*/*"
        startActivity(Intent.createChooser(shareIntent, getString(R.string.text__share_with)))
    }
}