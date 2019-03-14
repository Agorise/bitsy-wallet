package cy.agorise.bitsybitshareswallet.fragments

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.crashlytics.android.Crashlytics
import com.google.common.primitives.UnsignedLong
import cy.agorise.bitsybitshareswallet.BuildConfig
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.FullNodesAdapter
import cy.agorise.bitsybitshareswallet.repositories.AuthorityRepository
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.CryptoUtils
import cy.agorise.bitsybitshareswallet.viewmodels.SettingsFragmentViewModel
import cy.agorise.graphenej.*
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.calls.BroadcastTransaction
import cy.agorise.graphenej.api.calls.GetAccounts
import cy.agorise.graphenej.api.calls.GetDynamicGlobalProperties
import cy.agorise.graphenej.models.DynamicGlobalProperties
import cy.agorise.graphenej.models.JsonRpcResponse
import cy.agorise.graphenej.network.FullNode
import cy.agorise.graphenej.operations.AccountUpgradeOperationBuilder
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_settings.*
import org.bitcoinj.core.DumpedPrivateKey
import org.bitcoinj.core.ECKey
import java.text.NumberFormat
import javax.crypto.AEADBadTagException

class SettingsFragment : ConnectedFragment(), BaseSecurityLockDialog.OnPINPatternEnteredListener {

    companion object {
        private const val TAG = "SettingsFragment"

        // Constants used to perform security locked requests
        private const val ACTION_CHANGE_SECURITY_LOCK = 1
        private const val ACTION_SHOW_BRAINKEY = 2

        // Constants used to organize NetworkService requests
        private const val RESPONSE_GET_DYNAMIC_GLOBAL_PROPERTIES_NODES = 1
        private const val RESPONSE_GET_DYNAMIC_GLOBAL_PROPERTIES_LTM = 2
        private const val RESPONSE_BROADCAST_TRANSACTION = 3
    }

    private lateinit var mViewModel: SettingsFragmentViewModel

    private var mUserAccount: UserAccount? = null

    private var privateKey: String? = null

    // Dialog displaying the list of nodes and their latencies
    private var mNodesDialog: MaterialDialog? = null

    /** Adapter that holds the FullNode list used in the Bitshares nodes modal */
    private var nodesAdapter: FullNodesAdapter? = null

    // Map used to keep track of request and response id pairs
    private val responseMap = HashMap<Long, Int>()

    /** Transaction to upgrade to LTM */
    private var ltmTransaction: Transaction? = null

    private val mHandler = Handler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        val nightMode = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)

        // Make sure the toolbar show the correct colors in both day and night modes
        val toolbar: Toolbar? = activity?.findViewById(R.id.toolbar)
        toolbar?.setBackgroundResource(if (!nightMode) R.color.colorPrimary else R.color.colorToolbarDark)

        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Crashlytics.setString(Constants.CRASHLYTICS_KEY_LAST_SCREEN, TAG)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "") ?: ""

        // Configure ViewModel
        mViewModel= ViewModelProviders.of(this).get(SettingsFragmentViewModel::class.java)

        mViewModel.getUserAccount(userId).observe(this,
            androidx.lifecycle.Observer<cy.agorise.bitsybitshareswallet.database.entities.UserAccount>{ userAccount ->
                if (userAccount != null) {
                    mUserAccount = UserAccount(userAccount.id, userAccount.name)
                    btnUpgradeToLTM.isEnabled = !userAccount.isLtm  // Disable button if already LTM
                }
        })

        mViewModel.getWIF(userId, AuthorityType.ACTIVE.ordinal).observe(this,
            androidx.lifecycle.Observer<String> { encryptedWIF ->
                context?.let {
                    try {
                        privateKey = CryptoUtils.decrypt(it, encryptedWIF)
                    } catch (e: AEADBadTagException) {
                        Log.e(TAG, "AEADBadTagException. Class: " + e.javaClass + ", Msg: " + e.message)
                    }
                }
            })

        initAutoCloseSwitch()

        initNightModeSwitch()

        tvNetworkStatus.setOnClickListener { v ->
            if (mNetworkService != null) {
                // PublishSubject used to announce full node latencies updates
                val fullNodePublishSubject = mNetworkService!!.nodeLatencyObservable
                fullNodePublishSubject?.observeOn(AndroidSchedulers.mainThread())?.subscribe(nodeLatencyObserver)

                val fullNodes = mNetworkService!!.nodes

                nodesAdapter = FullNodesAdapter(v.context)
                nodesAdapter?.add(fullNodes)

                mNodesDialog = MaterialDialog(v.context)
                    .title(text = String.format("%s v%s", getString(R.string.app_name), BuildConfig.VERSION_NAME))
                    .message(text = getString(R.string.title__bitshares_nodes_dialog, "-------"))
                    .customListAdapter(nodesAdapter as FullNodesAdapter)
                    .negativeButton(android.R.string.ok)
                    .onDismiss { mHandler.removeCallbacks(mRequestDynamicGlobalPropertiesTask) }

                mNodesDialog?.show()

                // Registering a recurrent task used to poll for dynamic global properties requests
                mHandler.post(mRequestDynamicGlobalPropertiesTask)
            }
        }

        // Obtain the current Security Lock Option selected and display it in the screen
        val securityLockSelected = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(Constants.KEY_SECURITY_LOCK_SELECTED, 0)
        // Security Lock Options
        // 0 -> None
        // 1 -> PIN
        // 2 -> Pattern

        tvSecurityLockSelected.text = resources.getStringArray(R.array.security_lock_options)[securityLockSelected]

        tvSecurityLock.setOnClickListener { onSecurityLockTextSelected() }
        tvSecurityLockSelected.setOnClickListener { onSecurityLockTextSelected() }

        btnViewBrainKey.setOnClickListener { onShowBrainKeyButtonSelected() }

        btnUpgradeToLTM.setOnClickListener { onUpgradeToLTMButtonSelected() }
    }

    /**
     * Observer used to be notified about node latency measurement updates.
     */
    private val nodeLatencyObserver = object : Observer<FullNode> {
        override fun onSubscribe(d: Disposable) {
            mDisposables.add(d)
        }

        override fun onNext(fullNode: FullNode) {
            if (!fullNode.isRemoved)
                nodesAdapter?.add(fullNode)
            else
                nodesAdapter?.remove(fullNode)
        }

        override fun onError(e: Throwable) {
            Log.e(TAG, "nodeLatencyObserver.onError.Msg: " + e.message)
        }

        override fun onComplete() {}
    }

    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {
        if (responseMap.containsKey(response.id)) {
            val responseType = responseMap[response.id]
            when (responseType) {
                RESPONSE_GET_DYNAMIC_GLOBAL_PROPERTIES_NODES    -> handleDynamicGlobalPropertiesNodes(response.result)
                RESPONSE_GET_DYNAMIC_GLOBAL_PROPERTIES_LTM      -> handleDynamicGlobalPropertiesLTM(response.result)
                RESPONSE_BROADCAST_TRANSACTION                  -> handleBroadcastTransaction(response)
            }
            responseMap.remove(response.id)
        }
    }

    override fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate) {  }

    /** Handles the result of the [GetDynamicGlobalProperties] api call to obtain the current block number and update
     * it in the Nodes Dialog */
    private fun handleDynamicGlobalPropertiesNodes(result: Any?) {
        if (result is DynamicGlobalProperties) {
            if (mNodesDialog != null && mNodesDialog?.isShowing == true) {
                val blockNumber = NumberFormat.getInstance().format(result.head_block_number)
                mNodesDialog?.message(text = getString(R.string.title__bitshares_nodes_dialog, blockNumber))
            }
        }
    }

    private fun handleDynamicGlobalPropertiesLTM(result: Any?) {
        if (result is DynamicGlobalProperties) {
            val expirationTime = (result.time.time / 1000) + Transaction.DEFAULT_EXPIRATION_TIME
            val headBlockId = result.head_block_id
            val headBlockNumber = result.head_block_number

            ltmTransaction?.blockData = BlockData(headBlockNumber, headBlockId, expirationTime)

            val id = mNetworkService?.sendMessage(BroadcastTransaction(ltmTransaction), BroadcastTransaction.REQUIRED_API)
            if (id != null) responseMap[id] = RESPONSE_BROADCAST_TRANSACTION

            // TODO use an indicator to show that a transaction is in progress
        }
    }

    /** Handles the result of the [BroadcastTransaction] api call to find out if the Transaction to upgrade the
     * current account to LTM was successful or not */
    private fun handleBroadcastTransaction(message: JsonRpcResponse<*>) {
        if (message.result == null && message.error == null) {
            // Looks like the upgrade to LTM was successful, we need to update the current account information from
            // the blockchain and show a success dialog
            mNetworkService?.sendMessage(GetAccounts(mUserAccount), GetAccounts.REQUIRED_API)

            context?.let { context ->
                MaterialDialog(context).show {
                    title(R.string.title__account_upgraded)
                    message(R.string.msg__account_upgraded)
                    positiveButton(android.R.string.ok)
                }
            }
        } else if (message.error != null) {
            // The upgrade to LTM wasn't successful, show a dialog to the user explaining the situation
            context?.let { context ->
                MaterialDialog(context).show {
                    title(R.string.title__upgrade_account_error)
                    message(R.string.msg__upgrade_account_error)
                    positiveButton(android.R.string.ok)
                }
            }
        }
    }

    /**
     * Task used to obtain frequent updates on the global dynamic properties object
     */
    private val mRequestDynamicGlobalPropertiesTask = object : Runnable {
        override fun run() {
            val id = mNetworkService?.sendMessage(GetDynamicGlobalProperties(), GetDynamicGlobalProperties.REQUIRED_API)
            if (id != null) responseMap[id] = RESPONSE_GET_DYNAMIC_GLOBAL_PROPERTIES_NODES

            mHandler.postDelayed(this, Constants.BLOCK_PERIOD)
        }
    }

    /**
     * Fetches the relevant preference from the SharedPreferences and configures the corresponding switch accordingly,
     * and adds a listener to the said switch to store the preference in case the user changes it.
     */
    private fun initAutoCloseSwitch() {
        val autoCloseOn = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_AUTO_CLOSE_ACTIVATED, true)

        switchAutoClose.isChecked = autoCloseOn

        switchAutoClose.setOnCheckedChangeListener { buttonView, isChecked ->
            PreferenceManager.getDefaultSharedPreferences(buttonView.context).edit()
                .putBoolean(Constants.KEY_AUTO_CLOSE_ACTIVATED, isChecked).apply()
        }
    }

    /**
     * Fetches the relevant preference from the SharedPreferences and configures the corresponding switch accordingly,
     * and adds a listener to the said switch to store the preference in case the user changes it. Also makes a call to
     * recreate the activity and apply the selected theme.
     */
    private fun initNightModeSwitch() {
        val nightModeOn = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)

        switchNightMode.isChecked = nightModeOn

        switchNightMode.setOnCheckedChangeListener { buttonView, isChecked ->

            PreferenceManager.getDefaultSharedPreferences(buttonView.context).edit()
                .putBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, isChecked).apply()

            // Recreates the activity to apply the selected theme
            activity?.recreate()
        }
    }

    private fun onSecurityLockTextSelected() {
        if (!verifySecurityLock(ACTION_CHANGE_SECURITY_LOCK))
            showChooseSecurityLockDialog()
    }

    /**
     * Encapsulates the logic required to do actions possibly locked by the Security Lock. If PIN/Pattern is selected
     * then it prompts for it.
     *
     * @param actionIdentifier      Identifier used to know why a verify security lock was launched
     * @return                      true if the action was handled, false otherwise
     */
    private fun verifySecurityLock(actionIdentifier: Int): Boolean {
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
        args.putInt(BaseSecurityLockDialog.KEY_ACTION_IDENTIFIER, actionIdentifier)

        return when (securityLockSelected) {
            0 -> { /* None */
                false
            }
            1 -> { /* PIN */
                val pinFrag = PINSecurityLockDialog()
                pinFrag.arguments = args
                pinFrag.show(childFragmentManager, "pin_security_lock_tag")
                true
            }
            else -> { /* Pattern */
                val patternFrag = PatternSecurityLockDialog()
                patternFrag.arguments = args
                patternFrag.show(childFragmentManager, "pattern_security_lock_tag")
                true
            }
        }
    }

    override fun onPINPatternEntered(actionIdentifier: Int) {
        if (actionIdentifier == ACTION_CHANGE_SECURITY_LOCK) {
            showChooseSecurityLockDialog()
        } else if (actionIdentifier == ACTION_SHOW_BRAINKEY) {
            getBrainkey()
        }
    }

    override fun onPINPatternChanged() {
        // Obtain the new Security Lock Option selected and display it in the screen
        val securityLockSelected = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Constants.KEY_SECURITY_LOCK_SELECTED, 0)
        // Security Lock Options
        // 0 -> None
        // 1 -> PIN
        // 2 -> Pattern

        tvSecurityLockSelected.text = resources.getStringArray(R.array.security_lock_options)[securityLockSelected]
    }

    /**
     * Shows a dialog so the user can select its desired Security Lock option.
     */
    private fun showChooseSecurityLockDialog() {
        // Obtain the current Security Lock Option selected and display it in the screen
        val securityLockSelected = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Constants.KEY_SECURITY_LOCK_SELECTED, 0)
        // Security Lock Options
        // 0 -> None
        // 1 -> PIN
        // 2 -> Pattern

        context?.let {
            MaterialDialog(it).show {
                title(R.string.title__security_dialog)
                listItemsSingleChoice(R.array.security_lock_options, initialSelection = securityLockSelected) {_, index, _ ->
                    // Args used for both PIN and Pattern options
                    val args = Bundle()
                    args.putInt(BaseSecurityLockDialog.KEY_STEP_SECURITY_LOCK,
                        BaseSecurityLockDialog.STEP_SECURITY_LOCK_CREATE)
                    args.putInt(BaseSecurityLockDialog.KEY_ACTION_IDENTIFIER, -1)

                    when (index) {
                        0 -> { /* None */
                            PreferenceManager.getDefaultSharedPreferences(context).edit()
                                .putInt(Constants.KEY_SECURITY_LOCK_SELECTED, 0).apply() // 0 -> None

                            // Call this function to update the UI
                            onPINPatternChanged()
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
            }
        }
    }

    private fun onShowBrainKeyButtonSelected() {
        if (!verifySecurityLock(ACTION_SHOW_BRAINKEY))
            getBrainkey()
    }

    /**
     * Obtains the brainKey from the authorities db table for the current user account and if it is not null it passes
     * the brainKey to a method to show it in a nice MaterialDialog
     */
    private fun getBrainkey() {
        context?.let {
            val userId = PreferenceManager.getDefaultSharedPreferences(it)
                .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "") ?: ""

            val authorityRepository = AuthorityRepository(it)

            mDisposables.add(authorityRepository.get(userId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { authority ->
                    if (authority != null) {
                        val plainBrainKey = CryptoUtils.decrypt(it, authority.encryptedBrainKey)
                        val plainSequenceNumber = CryptoUtils.decrypt(it, authority.encryptedSequenceNumber)
                        val sequenceNumber = Integer.parseInt(plainSequenceNumber)
                        val brainKey = BrainKey(plainBrainKey, sequenceNumber)
                        showBrainKeyDialog(brainKey)
                    }
                }
            )
        }
    }

    /**
     * Shows the plain brainkey in a dialog so that the user can view and Copy it.
     */
    private fun showBrainKeyDialog(brainKey: BrainKey) {
        context?.let { context ->
            MaterialDialog(context).show {
                title(text = "BrainKey")
                message(text = brainKey.brainKey)
                customView(R.layout.dialog_copy_brainkey)
                cancelable(false)
                positiveButton(R.string.button__copied) { it.dismiss() }
            }
        }
    }

    private fun onUpgradeToLTMButtonSelected() {
        context?.let { context ->
            val content = getString(R.string.msg__account_upgrade_dialog, mUserAccount?.name)
            MaterialDialog(context).show {
                message(text = content)
                negativeButton(android.R.string.cancel)
                positiveButton(android.R.string.ok) {
                    val operation = AccountUpgradeOperationBuilder()
                        .setIsUpgrade(true)
                        .setFee(AssetAmount(UnsignedLong.ZERO, Asset("1.3.0"))) // 0 BTS
                        .setAccountToUpgrade(mUserAccount).build()

                    val operations = ArrayList<BaseOperation>()
                    operations.add(operation)

                    val currentPrivateKey = ECKey.fromPrivate(
                        DumpedPrivateKey.fromBase58(null, privateKey).key.privKeyBytes)
                    ltmTransaction = Transaction(currentPrivateKey, null, operations)

                    val id = mNetworkService?.sendMessage(GetDynamicGlobalProperties(), GetDynamicGlobalProperties.REQUIRED_API)
                    if (id != null) responseMap[id] = RESPONSE_GET_DYNAMIC_GLOBAL_PROPERTIES_LTM
                }
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        super.onServiceDisconnected(name)

        tvNetworkStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null,
            resources.getDrawable(R.drawable.ic_disconnected, null), null)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        super.onServiceConnected(name, service)

        tvNetworkStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null,
            resources.getDrawable(R.drawable.ic_connected, null), null)
    }
}

