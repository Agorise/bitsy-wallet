package cy.agorise.bitsybitshareswallet.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.collection.LongSparseArray
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.google.android.material.snackbar.Snackbar
import com.google.common.primitives.UnsignedLong
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.jakewharton.rxbinding3.widget.textChanges
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.BalancesDetailsAdapter
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail
import cy.agorise.bitsybitshareswallet.utils.*
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceDetailViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.SendTransactionViewModel
import cy.agorise.graphenej.*
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.calls.BroadcastTransaction
import cy.agorise.graphenej.api.calls.GetAccountByName
import cy.agorise.graphenej.api.calls.GetDynamicGlobalProperties
import cy.agorise.graphenej.api.calls.GetRequiredFees
import cy.agorise.graphenej.crypto.SecureRandomGenerator
import cy.agorise.graphenej.models.AccountProperties
import cy.agorise.graphenej.models.DynamicGlobalProperties
import cy.agorise.graphenej.models.JsonRpcResponse
import cy.agorise.graphenej.operations.TransferOperation
import cy.agorise.graphenej.operations.TransferOperationBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
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
import kotlin.Comparator

class SendTransactionFragment : ConnectedFragment(), ZXingScannerView.ResultHandler,
    BaseSecurityLockDialog.OnPINPatternEnteredListener {

    companion object {
        private const val TAG = "SendTransactionFragment"

        // Camera Permission
        private const val REQUEST_CAMERA_PERMISSION = 1

        // Constants used to organize NetworkService requests
        private const val RESPONSE_GET_ACCOUNT_BY_NAME = 1
        private const val RESPONSE_GET_DYNAMIC_GLOBAL_PROPERTIES = 2
        private const val RESPONSE_GET_REQUIRED_FEES = 3
        private const val RESPONSE_BROADCAST_TRANSACTION = 4

        // Constant used to perform security locked requests
        private const val ACTION_SEND_TRANSFER = 1
    }

    // Navigation AAC Safe Args
    private val args: SendTransactionFragmentArgs by navArgs()

    private lateinit var mViewModel: SendTransactionViewModel

    /** Variables used in field's validation */
    private var isCameraPreviewVisible = false
    private var isToAccountCorrect = false
    private var isAmountCorrect = false
    private var isMemoCorrect = true

    private var mBalancesDetails = ArrayList<BalanceDetail>()

    private lateinit var mBalanceDetailViewModel: BalanceDetailViewModel

    private var mBalancesDetailsAdapter: BalancesDetailsAdapter? = null

    /** Keeps track of the asset's symbol selected in the Asset spinner */
    private var selectedAssetSymbol = "BTS"

    /** Current user account */
    private var mUserAccount: UserAccount? = null

    /** User account to which send the funds */
    private var mSelectedUserAccount: UserAccount? = null

    // Map used to keep track of request and response id pairs
    private val responseMap = LongSparseArray<Int>()

    /** Transaction being built */
    private var transaction: Transaction? = null

    /** Variable holding the current user's private key in the WIF format */
    private var wifKey: String? = null

    /** This is one of the recipient account's public key, it will be used for memo encoding */
    private var destinationPublicKey: PublicKey? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        val nightMode = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)

        // Sets the toolbar background color to red
        val toolbar: Toolbar? = activity?.findViewById(R.id.toolbar)
        toolbar?.setBackgroundResource(if (!nightMode) R.color.colorSend else R.color.colorToolbarDark)

        // Sets the status and navigation bars background color to a dark red or just dark
        val window = activity?.window
        context?.let { context ->
            val statusBarColor = ContextCompat.getColor(context,
                    if (!nightMode) R.color.colorSendDark else R.color.colorStatusBarDark)
            window?.statusBarColor = statusBarColor
            window?.navigationBarColor = statusBarColor
        }

        return inflater.inflate(R.layout.fragment_send_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "") ?: ""

        if (userId != "")
            mUserAccount = UserAccount(userId)

        // Configure ViewModel
        mViewModel= ViewModelProviders.of(this).get(SendTransactionViewModel::class.java)

        mViewModel.getWIF(userId, AuthorityType.ACTIVE.ordinal).observe(this,
            Observer<String> { encryptedWIF ->
                context?.let {
                    try {
                        wifKey = CryptoUtils.decrypt(it, encryptedWIF)
                    } catch (e: Exception) {
                        crashlytics.recordException(e)
                    }
                }
            })

        // Use Navigation SafeArgs to decide if we should activate or not the camera feed
        if (args.openCamera) {
            // Delay the camera action to avoid flicker in the fragment transition
            Handler().postDelayed({
                run { verifyCameraPermission() }
            }, 500)
        }

        fabOpenCamera.setOnClickListener { if (isCameraPreviewVisible) stopCameraPreview() else verifyCameraPermission() }

        // Configure BalanceDetailViewModel to show the current balances
        mBalanceDetailViewModel = ViewModelProviders.of(this).get(BalanceDetailViewModel::class.java)

        mBalanceDetailViewModel.getAll().observe(this, Observer<List<BalanceDetail>> { balancesDetails ->
            mBalancesDetails.clear()
            mBalancesDetails.addAll(balancesDetails)
            mBalancesDetails.sortWith(
                Comparator { a, b -> a.toString().compareTo(b.toString(), true) }
            )
            mBalancesDetailsAdapter = BalancesDetailsAdapter(context!!, android.R.layout.simple_spinner_item, mBalancesDetails)
            spAsset.adapter = mBalancesDetailsAdapter

            // Try to select the selectedAssetSymbol
            for (i in 0 until mBalancesDetailsAdapter!!.count) {
                if (mBalancesDetailsAdapter!!.getItem(i)!!.symbol == selectedAssetSymbol) {
                    spAsset.setSelection(i)
                    break
                }
            }
        })

        spAsset.onItemSelectedListener = assetItemSelectedListener

        fabSendTransaction.setOnClickListener { verifySecurityLockSendTransfer() }
        fabSendTransaction.disable(R.color.lightGray)

        // Use RxJava Debounce to avoid making calls to the NetworkService on every text change event
        mDisposables.add(
            tietTo.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .map { it.toString().trim() }
                .subscribe {
                    validateAccount(it)
                }
        )

        // Use RxJava Debounce to update the Amount error only after the user stops writing for > 500 ms
        mDisposables.add(
            tietAmount.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { validateAmount() }
        )

        // Use RxJava Debounce to update the Memo error, to make sure it has the correct length
        mDisposables.add(
            tietMemo.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.toString().trim() }
                .subscribe {
                    isMemoCorrect = it.length <= tilMemo.counterMaxLength
                    enableDisableSendFAB()
                }
        )

        // Populates the To field if a Deep Link was used
        if (args.to != " " && args.asset != " " && args.amount > 0 && args.memo != " ") {
            val items = arrayOf(LineItem("transfer", 1, args.amount.toDouble()))
            val invoice = Invoice(args.to, "", args.memo, args.asset, items, "", "")
            Handler().postDelayed({
                populatePropertiesFromQRCodeString(Invoice.toQrCode(invoice))
            }, 2000) // Wait to let the other elements of the fragment initialize
        }

        verifyGlobalSettingsTimeAutomatic()
    }

    /** Handles the selection of items in the Asset spinner, to keep track of the selectedAssetSymbol and show the
     * current user's balance of the selected asset. */
    private val assetItemSelectedListener = object : AdapterView.OnItemSelectedListener{
        override fun onNothingSelected(parent: AdapterView<*>?) { }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val balance = mBalancesDetailsAdapter!!.getItem(position)!!
            selectedAssetSymbol = balance.symbol

            val amount = balance.amount.toDouble() / Math.pow(10.0, balance.precision.toDouble())

            tvAvailableAssetAmount.text =
                    String.format("%." + Math.min(balance.precision, 8) + "f %s", amount, balance.toString())

            validateAmount()
        }
    }

    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {
        if (responseMap.containsKey(response.id)) {
            when (responseMap[response.id]) {
                RESPONSE_GET_ACCOUNT_BY_NAME            -> handleAccountProperties(response.result)
                RESPONSE_GET_DYNAMIC_GLOBAL_PROPERTIES  -> handleDynamicGlobalProperties(response.result)
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

    /** Handles the result of the [GetAccountByName] api call to find out if the account written in the To text
     * field corresponds to an actual BitShares account or not and acts accordingly */
    private fun handleAccountProperties(result: Any?) {
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

    /** Handles the result of the [GetDynamicGlobalProperties] api call to add the needed metadata to the [Transaction]
     * the app is building and ultimately send, if everything is correct adds the needed info to the [Transaction] and
     * calls the next step which is [GetRequiredFees] else it shows an error */
    private fun handleDynamicGlobalProperties(result: Any?) {
        if (result is DynamicGlobalProperties) {

            val now = System.currentTimeMillis() / 1000
            val time = result.time.time / 1000

            // Show an error if the current connected node is out of sync
            if (now - time > Constants.CHECK_NODE_OUT_OF_SYNC) {
                context?.toast(getString(R.string.msg__transaction_not_sent))
                return
            }

            val expirationTime = time + Transaction.DEFAULT_EXPIRATION_TIME
            val headBlockId = result.head_block_id
            val headBlockNumber = result.head_block_number

            transaction!!.blockData = BlockData(headBlockNumber, headBlockId, expirationTime)

            val feeAsset = Asset(Constants.CORE_ASSET)

            val id = mNetworkService?.sendMessage(GetRequiredFees(transaction!!, feeAsset), GetRequiredFees.REQUIRED_API)
            if (id != null) responseMap.append(id, RESPONSE_GET_REQUIRED_FEES)
        } else {
            context?.toast(getString(R.string.msg__transaction_not_sent))
        }
    }

    /** Handles the result of the [GetRequiredFees] api call to add the fees to the [Transaction] the app is building
     * and ultimately send, and if everything is correct broadcasts the [Transaction] else it shows an error */
    private fun handleRequiredFees(result: Any?) {
        if (result is List<*> && result[0] is AssetAmount) {
            Log.d(TAG, "GetRequiredFees: " + transaction.toString())
            transaction!!.setFees(result as List<AssetAmount>) // TODO find how to remove this warning

            val id = mNetworkService?.sendMessage(BroadcastTransaction(transaction), BroadcastTransaction.REQUIRED_API)
            if (id != null) responseMap.append(id, RESPONSE_BROADCAST_TRANSACTION)
        } else {
            context?.toast(getString(R.string.msg__transaction_not_sent))
        }
    }

    /** Handles the result of the [BroadcastTransaction] api call to find out if the Transaction was sent successfully
     * or not and acts accordingly */
    private fun handleBroadcastTransaction(message: JsonRpcResponse<*>) {
        if (message.result == null && message.error == null) {
            context?.toast(getString(R.string.text__transaction_sent))

            // Return to the main screen
            findNavController().navigateUp()
        } else if (message.error != null) {
            context?.toast(message.error.message, Toast.LENGTH_LONG)
        }
    }

    /** Verifies if the user has already granted the Camera permission, if not the asks for it */
    private fun verifyCameraPermission() {
        if (ContextCompat.checkSelfPermission(activity!!, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not already granted
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            // Permission is already granted
            startCameraPreview()
        }
    }

    /** Handles the result from the camera permission request */
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
        cameraPreview.setLaserColor(R.color.colorSecondary)
        cameraPreview.setMaskColor(R.color.colorSecondary)
        cameraPreview.setResultHandler(this)
        cameraPreview.startCamera()

        cameraPreview.scrollY = holderCamera.width / 6
    }

    private fun stopCameraPreview() {
        cameraPreview.visibility = View.INVISIBLE
        fabOpenCamera.setImageResource(R.drawable.ic_camera)
        isCameraPreviewVisible = false
        cameraPreview.stopCamera()
    }

    /** Handles the result of the QR code read from the camera **/
    override fun handleResult(result: Result?) {
        populatePropertiesFromQRCodeString(result!!.text)
    }

    /** Tries to populate the Account, Amount and Memo fields
    * and the Asset spinner with the obtained information */
    private fun populatePropertiesFromQRCodeString(qrString: String) {
        try {
            val invoice = Invoice.fromQrCode(qrString)

            Log.d(TAG, "QR Code read: " + invoice.toJsonString())

            tietTo.setText(invoice.to)

            if (invoice.memo != null) {
                tietMemo.setText(invoice.memo)
                if (invoice.memo.startsWith("PP"))
                    tietMemo.isEnabled = false
            }

            var balanceDetail: BalanceDetail? = null

            // Try to select the invoice's Asset in the Assets spinner
            for (i in 0 until (mBalancesDetailsAdapter?.count ?: 0)) {
                if (mBalancesDetailsAdapter?.getItem(i)?.symbol == invoice.currency.toUpperCase() ||
                    (invoice.currency.startsWith("bit", true) &&
                            invoice.currency.replaceFirst("bit", "").toUpperCase() ==
                            mBalancesDetailsAdapter?.getItem(i)?.symbol)) {
                    spAsset.setSelection(i)
                    balanceDetail = mBalancesDetailsAdapter?.getItem(i)
                    break
                }
            }

            // If the user does not own any of the requested asset then show a SnackBar to explain the issue and
            // return early to avoid filling the asset field
            if (balanceDetail == null) {
                Snackbar.make(rootView, getString(R.string.error__you_dont_own_asset, invoice.currency.toUpperCase()),
                    Snackbar.LENGTH_INDEFINITE).setAction(android.R.string.ok) {  }.show()
                return
            }

            var amount = 0.0
            for (nextItem in invoice.lineItems) {
                amount += nextItem.quantity * nextItem.price
            }

            // Populate the amount field only if amount is not zero.
            if (amount > 0.0) {
                val df = DecimalFormat("####." + "#".repeat(balanceDetail.precision))
                df.roundingMode = RoundingMode.CEILING
                df.decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())
                tietAmount.setText(df.format(amount))
            } else {
                tietAmount.setText("")
            }

        }catch (e: Exception) {
            Log.d(TAG, "Invoice error: " + e.message)
        }
    }

    /**
     * Sends a request to the node through the NetworkService to validate that accountName is a valid
     * BitShares account.
     */
    private fun validateAccount(accountName: String) {
        isToAccountCorrect = false
        val id = mNetworkService?.sendMessage(GetAccountByName(accountName), GetAccountByName.REQUIRED_API)
        if (id != null) responseMap.append(id, RESPONSE_GET_ACCOUNT_BY_NAME)
    }

    private fun validateAmount() {
        val txtAmount = tietAmount.text.toString().replace(",", ".")

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
        if (isToAccountCorrect && isAmountCorrect && isMemoCorrect) {
            fabSendTransaction.enable(R.color.colorSend)
            vSend.setBackgroundResource(R.drawable.send_fab_background)
        } else {
            fabSendTransaction.disable(R.color.lightGray)
            vSend.setBackgroundResource(R.drawable.send_fab_background_disabled)
        }
    }

    private fun verifySecurityLockSendTransfer() {
        val securityLockSelected = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Constants.KEY_SECURITY_LOCK_SELECTED, 0)
        // Security Lock Options
        // 0 -> None
        // 1 -> PIN
        // 2 -> Pattern

        // Args used for both PIN and Pattern options
        val args = Bundle()
        args.putInt(BaseSecurityLockDialog.KEY_STEP_SECURITY_LOCK,
            BaseSecurityLockDialog.STEP_SECURITY_LOCK_VERIFY)
        args.putInt(BaseSecurityLockDialog.KEY_ACTION_IDENTIFIER, ACTION_SEND_TRANSFER)

        when (securityLockSelected) {
            0 -> { /* None */
                startSendTransferOperation()

            }
            1 -> { /* PIN */
                val pinFrag = PINSecurityLockDialog()
                pinFrag.arguments = args
                pinFrag.show(childFragmentManager, "pin_security_lock_tag")
            }
            else -> { /* Pattern */
                val patternFrag = PatternSecurityLockDialog()
                patternFrag.arguments = args
                patternFrag.show(childFragmentManager, "pattern_security_lock_tag")
            }
        }
    }

    override fun onPINPatternEntered(actionIdentifier: Int) {
        if (actionIdentifier == ACTION_SEND_TRANSFER) {
            startSendTransferOperation()
        }
    }

    override fun onPINPatternChanged() { /* Do nothing */ }

    /** Starts the Send Transfer operation procedure, creating a [TransferOperation] and sending a call to the
     * NetworkService to obtain the [DynamicGlobalProperties] object needed to successfully send a Transfer */
    private fun startSendTransferOperation() {
        // Create TransferOperation
        if (mNetworkService?.isConnected == true) {
            val balance = mBalancesDetailsAdapter!!.getItem(spAsset.selectedItemPosition)!!
            val amount = (tietAmount.text.toString().replace(",", ".").toDouble()
                    * Math.pow(10.0, balance.precision.toDouble())).toLong()

            val transferAmount = AssetAmount(UnsignedLong.valueOf(amount), Asset(balance.id))

            val operationBuilder = TransferOperationBuilder()
                .setSource(mUserAccount)
                .setDestination(mSelectedUserAccount)
                .setTransferAmount(transferAmount)

            val privateKey = ECKey.fromPrivate(DumpedPrivateKey.fromBase58(null, wifKey).key.privKeyBytes)

            // Add memo if it is not empty
            val memoMsg = tietMemo.text.toString()
            if (memoMsg.isNotEmpty()) {
                val nonce = Math.abs(SecureRandomGenerator.getSecureRandom().nextLong()).toBigInteger()
                val encryptedMemo = Memo.encryptMessage(privateKey, destinationPublicKey!!, nonce, memoMsg)
                val from = Address(ECKey.fromPublicOnly(privateKey.pubKey))
                val to = Address(destinationPublicKey!!.key)
                val memo = Memo(from, to, nonce, encryptedMemo)
                operationBuilder.setMemo(memo)
            }

            // Object that will contain all operations to be sent at once
            val operations = ArrayList<BaseOperation>()
            // Transfer from the current user to the selected one
            val transferOperation = operationBuilder.build()
            operations.add(transferOperation)

            // Transfer operation to be sent as a fee to Agorise
            val feeOperation = getAgoriseFeeOperation(transferOperation)
            if (feeOperation != null && (feeOperation.assetAmount?.amount?.toLong() ?: 0L) > 0L)
                operations.add(feeOperation)

            transaction = Transaction(privateKey, null, operations)

            // Start the send transaction procedure which includes a series of calls
            val id = mNetworkService?.sendMessage(GetDynamicGlobalProperties(),
                GetDynamicGlobalProperties.REQUIRED_API)
            if (id != null ) responseMap.append(id,  RESPONSE_GET_DYNAMIC_GLOBAL_PROPERTIES)
        } else
            Log.d(TAG, "Network Service is not connected")
    }

    private fun verifyGlobalSettingsTimeAutomatic() {
        if (Settings.Global.getInt(context?.contentResolver, Settings.Global.AUTO_TIME, 0) != 1) {
            context?.let { context ->
                MaterialDialog(context).show {
                    title(R.string.title__time_sync_error)
                    message(R.string.msg__time_sync_error)
                    positiveButton(android.R.string.ok) {
                        findNavController().navigateUp()
                    }
                    cancelable(false)
                    cancelOnTouchOutside(false)
                }
            }
        }
    }

    /**
     * Obtains the correct [TransferOperation] object to send the fee to Agorise. A fee is only sent if the Asset is
     * BTS or a SmartCoin.
     */
    private fun getAgoriseFeeOperation(transferOperation: TransferOperation): TransferOperation? {
        // Disable Agorise fees on debug builds
        if (BuildConfig.DEBUG)
            return null

        // Verify that the current Asset is either BTS or a SmartCoin
        if (Constants.assetsWhichSendFeeToAgorise.contains(transferOperation.assetAmount?.asset?.objectId ?: "")) {
            val fee = transferOperation.assetAmount?.multiplyBy(Constants.FEE_PERCENTAGE) ?: return null

            return TransferOperationBuilder()
                .setSource(mUserAccount)
                .setDestination(Constants.AGORISE_ACCOUNT)
                .setTransferAmount(fee)
                .build()
        } else
            return null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_send_transaction, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_info) {
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
