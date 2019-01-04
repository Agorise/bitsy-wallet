package cy.agorise.bitsybitshareswallet.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.google.common.primitives.UnsignedLong
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.jakewharton.rxbinding3.widget.textChanges
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.BalancesDetailsAdapter
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail
import cy.agorise.bitsybitshareswallet.repositories.AuthorityRepository
import cy.agorise.bitsybitshareswallet.utils.*
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceDetailViewModel
import cy.agorise.graphenej.*
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.calls.BroadcastTransaction
import cy.agorise.graphenej.api.calls.GetAccountByName
import cy.agorise.graphenej.api.calls.GetDynamicGlobalProperties
import cy.agorise.graphenej.api.calls.GetRequiredFees
import cy.agorise.graphenej.models.AccountProperties
import cy.agorise.graphenej.models.DynamicGlobalProperties
import cy.agorise.graphenej.models.JsonRpcResponse
import cy.agorise.graphenej.operations.TransferOperationBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_send_transaction.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import org.bitcoinj.core.DumpedPrivateKey
import org.bitcoinj.core.ECKey
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.AEADBadTagException

class SendTransactionFragment : ConnectedFragment(), ZXingScannerView.ResultHandler {
    private val TAG = this.javaClass.simpleName

    // Camera Permission
    private val REQUEST_CAMERA_PERMISSION = 1

    private val RESPONSE_GET_ACCOUNT_BY_NAME = 1
    private val RESPONSE_GET_DYNAMIC_GLOBAL_PARAMETERS = 2
    private val RESPONSE_GET_REQUIRED_FEES = 3
    private val RESPONSE_BROADCAST_TRANSACTION = 4

    private var isCameraPreviewVisible = false
    private var isToAccountCorrect = false
    private var isAmountCorrect = false

    private var mBalancesDetails: List<BalanceDetail>? = null

    private lateinit var mBalanceDetailViewModel: BalanceDetailViewModel

    private var mBalancesDetailsAdapter: BalancesDetailsAdapter? = null

    private var selectedAssetSymbol = ""

    /** Current user account */
    private var mUserAccount: UserAccount? = null

    /** User account to which send the funds */
    private var mSelectedUserAccount: UserAccount? = null

    // Map used to keep track of request and response id pairs
    private val responseMap = HashMap<Long, Int>()

    private var transaction: Transaction? = null

    /** Variable holding the current user's private key in the WIF format */
    private var wifKey: String? = null

    /** Repository to access and update Authorities */
    private var authorityRepository: AuthorityRepository? = null

    /* This is one of the of the recipient account's public key, it will be used for memo encoding */
    private var destinationPublicKey: PublicKey? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        val nightMode = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)

        // Sets the toolbar background color to red
        val toolbar: Toolbar? = activity?.findViewById(R.id.toolbar)
        toolbar?.setBackgroundResource(if (!nightMode) R.color.colorSend else R.color.colorToolbarDark)

        // Sets the status bar background color to a dark red
        val window = activity?.window
        window?.statusBarColor = ContextCompat.getColor(context!!,
            if (!nightMode) R.color.colorSendDark else R.color.colorStatusBarDark)

        return inflater.inflate(R.layout.fragment_send_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")

        if (userId != "")
            mUserAccount = UserAccount(userId)

        // Use Navigation SafeArgs to decide if we should activate or not the camera feed
        val safeArgs = SendTransactionFragmentArgs.fromBundle(arguments!!)
        if (safeArgs.openCamera)
            verifyCameraPermission()

        fabOpenCamera.setOnClickListener { if (isCameraPreviewVisible) stopCameraPreview() else verifyCameraPermission() }

        // Configure BalanceDetailViewModel to show the current balances
        mBalanceDetailViewModel = ViewModelProviders.of(this).get(BalanceDetailViewModel::class.java)

        mBalanceDetailViewModel.getAll().observe(this, Observer<List<BalanceDetail>> { balancesDetails ->
            mBalancesDetails = balancesDetails
            mBalancesDetailsAdapter = BalancesDetailsAdapter(context!!, android.R.layout.simple_spinner_item, mBalancesDetails!!)
            spAsset.adapter = mBalancesDetailsAdapter

            // Try to select the selectedAssetSymbol
            for (i in 0 until mBalancesDetailsAdapter!!.count) {
                if (mBalancesDetailsAdapter!!.getItem(i)!!.symbol == selectedAssetSymbol) {
                    spAsset.setSelection(i)
                    break
                }
            }
        })

        spAsset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) { }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val balance = mBalancesDetailsAdapter!!.getItem(position)!!
                selectedAssetSymbol = balance.symbol

                val amount = balance.amount.toDouble() / Math.pow(10.0, balance.precision.toDouble())

                tvAvailableAssetAmount.text =
                        String.format("%." + Math.min(balance.precision, 8) + "f %s", amount, balance.symbol)
            }
        }

        fabSendTransaction.setOnClickListener { startSendTransferOperation() }
        fabSendTransaction.disable(R.color.lightGray)

        authorityRepository = AuthorityRepository(context!!)

        mDisposables.add(
            authorityRepository!!.getWIF(userId!!, AuthorityType.ACTIVE.ordinal)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { encryptedWIF ->
                    try {
                        wifKey = CryptoUtils.decrypt(context!!, encryptedWIF)
                    } catch (e: AEADBadTagException) {
                        Log.e(TAG, "AEADBadTagException. Class: " + e.javaClass + ", Msg: " + e.message)
                    }

                }
        )

        // Use RxJava Debounce to avoid making calls to the NetworkService on every text change event
        mDisposables.add(
            tietTo.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .map { it.toString().trim() }
                .subscribe {
                    val id = mNetworkService!!.sendMessage(GetAccountByName(it!!), GetAccountByName.REQUIRED_API)
                    responseMap[id] = RESPONSE_GET_ACCOUNT_BY_NAME
                }
        )

        // Use RxJava Debounce to update the Amount error only after the user stops writing for > 500 ms
        mDisposables.add(
            tietAmount.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .map { it.toString().trim() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { validateAmount(it!!) }
        )
    }

    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {
        if (responseMap.containsKey(response.id)) {
            val responseType = responseMap[response.id]
            when (responseType) {
                RESPONSE_GET_ACCOUNT_BY_NAME            -> handleAccountName(response.result)
                RESPONSE_GET_DYNAMIC_GLOBAL_PARAMETERS  -> handleDynamicGlobalProperties(response.result)
                RESPONSE_GET_REQUIRED_FEES              -> handleRequiredFees(response.result)
                RESPONSE_BROADCAST_TRANSACTION          -> handleBroadcastTransaction(response)
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

    private fun handleAccountName(result: Any?) {
        if (result is AccountProperties) {
            mSelectedUserAccount = UserAccount(result.id, result.name)
            destinationPublicKey = result.active.keyAuths.keys.iterator().next()
            tilTo.isErrorEnabled = false
            isToAccountCorrect = true
        } else {
            mSelectedUserAccount = null
            destinationPublicKey = null
            tilTo.error = getString(R.string.error__invalid_account)
            isToAccountCorrect = false
        }

        enableDisableSendFAB()
    }

    private fun handleDynamicGlobalProperties(result: Any?) {
        if (result is DynamicGlobalProperties) {
            val expirationTime = (result.time.time / 1000) + Transaction.DEFAULT_EXPIRATION_TIME
            val headBlockId = result.head_block_id
            val headBlockNumber = result.head_block_number

            transaction!!.blockData = BlockData(headBlockNumber, headBlockId, expirationTime)

            val asset = Asset(mBalancesDetailsAdapter!!.getItem(spAsset.selectedItemPosition)!!.id)

            val id = mNetworkService!!.sendMessage(GetRequiredFees(transaction!!, asset), GetRequiredFees.REQUIRED_API)
            responseMap[id] = RESPONSE_GET_REQUIRED_FEES
        } else {
            context?.toast(getString(R.string.msg__transaction_not_sent))
        }
    }

    private fun handleRequiredFees(result: Any?) {
        if (result is List<*> && result[0] is AssetAmount) {
            Log.d(TAG, "GetRequiredFees: " + transaction.toString())
            transaction!!.setFees(result as List<AssetAmount>) // TODO find how to remove this warning

            val id = mNetworkService!!.sendMessage(BroadcastTransaction(transaction), BroadcastTransaction.REQUIRED_API)
            responseMap[id] = RESPONSE_BROADCAST_TRANSACTION
        } else {
            context?.toast(getString(R.string.msg__transaction_not_sent))
        }
    }

    private fun handleBroadcastTransaction(message: JsonRpcResponse<*>) {
        if (message.result == null && message.error == null) {
            context?.toast(getString(R.string.text__transaction_sent))

            // Remove information from the text fields and disable send button
            tietTo.setText("")
            tietAmount.setText("")
            tietMemo.setText("")
            isToAccountCorrect = false
            isAmountCorrect = false
            enableDisableSendFAB()

            // TODO return to Main fragment ??
        } else {
            context?.toast(message.error.message, Toast.LENGTH_LONG)
        }
    }

    private fun verifyCameraPermission() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not already granted
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            // Permission is already granted
            startCameraPreview()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startCameraPreview()
            } else {
                context?.toast(getString(R.string.msg__camera_permission_necessary))
            }
            return
        }
    }

    private fun startCameraPreview() {
        cameraPreview.visibility = View.VISIBLE
        fabOpenCamera.setImageResource(R.drawable.ic_close)
        isCameraPreviewVisible = true

        // Configure QR scanner
        cameraPreview.setFormats(listOf(BarcodeFormat.QR_CODE))
        cameraPreview.setAspectTolerance(0.5f)
        cameraPreview.setAutoFocus(true)
        cameraPreview.setLaserColor(R.color.colorAccent)
        cameraPreview.setMaskColor(R.color.colorAccent)
        cameraPreview.setResultHandler(this)
        cameraPreview.startCamera()
    }

    private fun stopCameraPreview() {
        cameraPreview.visibility = View.INVISIBLE
        fabOpenCamera.setImageResource(R.drawable.ic_camera)
        isCameraPreviewVisible = false
        cameraPreview.stopCamera()
    }

    override fun handleResult(result: Result?) {
        try {
            val invoice = Invoice.fromQrCode(result!!.text)

            Log.d(TAG, "QR Code read: " + invoice.toJsonString())

            tietTo.setText(invoice.to)

            for (i in 0 until mBalancesDetailsAdapter!!.count) {
                if (mBalancesDetailsAdapter!!.getItem(i)!!.symbol == invoice.currency.toUpperCase()) {
                    spAsset.setSelection(i)
                    break
                }
            }
            tietMemo.setText(invoice.memo)


            var amount = 0.0
            for (nextItem in invoice.lineItems) {
                amount += nextItem.quantity * nextItem.price
            }
            // TODO Improve pattern to account for different asset precisions
            val df = DecimalFormat("####.#####")
            df.roundingMode = RoundingMode.CEILING
            df.decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())
            tietAmount.setText(df.format(amount))

        }catch (e: Exception) {
            Log.d(TAG, "Invoice error: " + e.message)
        }
    }

    private fun validateAmount(txtAmount: String) {
        if (mBalancesDetailsAdapter?.isEmpty != false) return
        val balance = mBalancesDetailsAdapter?.getItem(spAsset.selectedItemPosition) ?: return
        val currentAmount = balance.amount.toDouble() / Math.pow(10.0, balance.precision.toDouble())

        val amount: Double = try {
            txtAmount.toDouble()
        } catch (e: Exception) {
            0.0
        }

        when {
            currentAmount < amount -> {
                tilAmount.error = getString(R.string.error__not_enough_funds)
                isAmountCorrect = false
            }
            amount == 0.0 -> {
                tilAmount.isErrorEnabled = false
                isAmountCorrect = false
            }
            else -> {
                tilAmount.isErrorEnabled = false
                isAmountCorrect = true
            }
        }

        enableDisableSendFAB()
    }

    private fun enableDisableSendFAB() {
        if (isToAccountCorrect && isAmountCorrect) {
            fabSendTransaction.enable(R.color.colorSend)
            vSend.setBackgroundResource(R.drawable.send_fab_background)
        } else {
            fabSendTransaction.disable(R.color.lightGray)
            vSend.setBackgroundResource(R.drawable.send_fab_background_disabled)
        }
    }

    private fun startSendTransferOperation() {
        // Create TransferOperation
        if (mNetworkService!!.isConnected) {
            val balance = mBalancesDetailsAdapter!!.getItem(spAsset.selectedItemPosition)!!
            val amount = (tietAmount.text.toString().toDouble() * Math.pow(10.0, balance.precision.toDouble())).toLong()

            val transferAmount = AssetAmount(UnsignedLong.valueOf(amount), Asset(balance.id))

            val operationBuilder = TransferOperationBuilder()
                .setSource(mUserAccount)
                .setDestination(mSelectedUserAccount)
                .setTransferAmount(transferAmount)

            val privateKey = ECKey.fromPrivate(DumpedPrivateKey.fromBase58(null, wifKey).key.privKeyBytes)

            // Add memo if exists TODO enable memo
//            val memoMsg = tietMemo.text.toString()
//            if (memoMsg.isNotEmpty()) {
//                val nonce = SecureRandomGenerator.getSecureRandom().nextLong().toBigInteger()
//                val encryptedMemo = Memo.encryptMessage(privateKey, destinationPublicKey!!, nonce, memoMsg)
//                val from = Address(ECKey.fromPublicOnly(privateKey.pubKey))
//                val to = Address(destinationPublicKey!!.key)
//                val memo = Memo(from, to, nonce, encryptedMemo)
//                operationBuilder.setMemo(memo)
//            }

            val operations = ArrayList<BaseOperation>()
            operations.add(operationBuilder.build())

            transaction = Transaction(privateKey, null, operations)

            val id = mNetworkService!!.sendMessage(GetDynamicGlobalProperties(),
                GetDynamicGlobalProperties.REQUIRED_API)
            responseMap[id] =  RESPONSE_GET_DYNAMIC_GLOBAL_PARAMETERS
        } else
            Log.d(TAG, "Network Service is not connected")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_send_transaction, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.menu_info) {
            MaterialDialog(context!!).show {
                customView(R.layout.dialog_send_transaction_info, scrollable = true)
                positiveButton(android.R.string.ok) { dismiss() }
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (isCameraPreviewVisible)
            startCameraPreview()
    }

    override fun onPause() {
        super.onPause()

        if (!isCameraPreviewVisible)
            stopCameraPreview()
    }
}
