package cy.agorise.bitsybitshareswallet.activities

import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import cy.agorise.bitsybitshareswallet.BuildConfig
import cy.agorise.bitsybitshareswallet.database.entities.Balance
import cy.agorise.bitsybitshareswallet.database.entities.Transfer
import cy.agorise.bitsybitshareswallet.processors.TransfersLoader
import cy.agorise.bitsybitshareswallet.repositories.AssetRepository
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.Helper
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.ConnectedActivityViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.TransferViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.UserAccountViewModel
import cy.agorise.graphenej.Asset
import cy.agorise.graphenej.AssetAmount
import cy.agorise.graphenej.UserAccount
import cy.agorise.graphenej.api.ApiAccess
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.api.android.RxBus
import cy.agorise.graphenej.api.calls.*
import cy.agorise.graphenej.models.*
import cy.agorise.graphenej.network.FullNode
import io.fabric.sdk.android.Fabric
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

/**
 * The app uses the single Activity methodology, but this activity was created so that MainActivity can extend from it.
 * This class manages everything related to keeping the information in the database updated using graphenej's
 * NetworkService, leaving to MainActivity only the Navigation work and some other UI features.
 */
abstract class ConnectedActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ConnectedActivity"

        // Delay between best node connection verifications
        private const val NODE_CHECK_DELAY = 60000L   // 60 seconds

        // Worst acceptable position of the node the app is currently connected to
        private const val BEST_NODE_THRESHOLD = 1

        private const val RESPONSE_GET_FULL_ACCOUNTS = 1
        private const val RESPONSE_GET_ACCOUNTS = 2
        private const val RESPONSE_GET_ACCOUNT_BALANCES = 3
        private const val RESPONSE_GET_ASSETS = 4
        private const val RESPONSE_GET_BLOCK_HEADER = 5
        private const val RESPONSE_GET_MARKET_HISTORY = 6
    }

    private lateinit var mUserAccountViewModel: UserAccountViewModel
    private lateinit var mBalanceViewModel: BalanceViewModel
    private lateinit var mTransferViewModel: TransferViewModel
    private lateinit var mConnectedActivityViewModel: ConnectedActivityViewModel

    private lateinit var mAssetRepository: AssetRepository

    /* Current user account */
    protected var mCurrentAccount: UserAccount? = null

    private val mHandler = Handler()

    // Composite disposable used to clear all disposables once the activity is destroyed
    private val mCompositeDisposable = CompositeDisposable()

    private var storedOpCount: Long = -1

    private var missingUserAccounts = ArrayList<UserAccount>()
    private var missingAssets = ArrayList<Asset>()

    /* Network service connection */
    protected var mNetworkService: NetworkService? = NetworkService.getInstance()

    // Map used to keep track of request and response id pairs
    private val responseMap = HashMap<Long, Int>()

    /** Map used to keep track of request id and block number pairs */
    private val requestIdToBlockNumberMap = HashMap<Long, Long>()

    private var blockNumberWithMissingTime = 0L

    // Variable used to hold a reference to the specific Transfer instance which we're currently trying
    // to resolve an equivalent BTS value
    var transfer: Transfer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashlytics = Crashlytics.Builder()
            .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
            .build()
        Fabric.with(this, crashlytics)

        getUserAccount()

        mAssetRepository = AssetRepository(this)

        // Configure ConnectedActivityViewModel to obtain missing equivalent values
        mConnectedActivityViewModel = ViewModelProviders.of(this).get(ConnectedActivityViewModel::class.java)

        val currencyCode = Helper.getCoingeckoSupportedCurrency(Locale.getDefault())
        Log.d(TAG, "Using currency: ${currencyCode.toUpperCase(Locale.ROOT)}")
        mConnectedActivityViewModel.observeMissingEquivalentValuesIn(currencyCode)

        // Configure UserAccountViewModel to obtain the missing account ids
        mUserAccountViewModel = ViewModelProviders.of(this).get(UserAccountViewModel::class.java)

        mUserAccountViewModel.getMissingUserAccountIds().observe(this, Observer<List<String>>{ userAccountIds ->
            if (userAccountIds.isNotEmpty()) {
                missingUserAccounts.clear()
                for (userAccountId in userAccountIds)
                    missingUserAccounts.add(UserAccount(userAccountId))

                mHandler.postDelayed(mRequestMissingUserAccountsTask, Constants.NETWORK_SERVICE_RETRY_PERIOD)
            }
        })

        // Configure UserAccountViewModel to obtain the missing account ids
        mBalanceViewModel = ViewModelProviders.of(this).get(BalanceViewModel::class.java)

        mBalanceViewModel.getMissingAssetIds().observe(this, Observer<List<String>>{ assetIds ->
            if (assetIds.isNotEmpty()) {
                missingAssets.clear()
                for (assetId in assetIds)
                    missingAssets.add(Asset(assetId))

                mHandler.postDelayed(mRequestMissingAssetsTask, Constants.NETWORK_SERVICE_RETRY_PERIOD)
            }
        })

        //Configure TransferViewModel to obtain the Transfer's block numbers with missing time information, one by one
        mTransferViewModel = ViewModelProviders.of(this).get(TransferViewModel::class.java)

        mTransferViewModel.getTransferBlockNumberWithMissingTime().observe(this, Observer<Long>{ blockNumber ->
            if (blockNumber != null && blockNumber != blockNumberWithMissingTime) {
                blockNumberWithMissingTime = blockNumber
                mHandler.post(mRequestBlockMissingTimeTask)
            }
        })

        val disposable = RxBus.getBusInstance()
            .asFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                this.handleIncomingMessage(it)
            }, {
                this.handleError(it)
            })
        mCompositeDisposable.add(disposable)


        val info = this.packageManager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
        val versionCode = PackageInfoCompat.getLongVersionCode(info)
        val hasPurgedEquivalentValues = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(Constants.KEY_HAS_PURGED_EQUIVALENT_VALUES, false)
        if(versionCode > 11 && !hasPurgedEquivalentValues) {
            thread {
                mConnectedActivityViewModel.purgeEquivalentValues()
                PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean(Constants.KEY_HAS_PURGED_EQUIVALENT_VALUES, true)
                    .apply()
            }
        }
    }

    /**
     * Obtains the userId from the shared preferences and creates a [UserAccount] instance.
     * Created as a public function, so that it can be called from its Fragments.
     */
    fun getUserAccount() {
        val userId = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "") ?: ""
        if (userId != "")
            mCurrentAccount = UserAccount(userId)

        // Make sure crashlytics reports contains the account ID
        Crashlytics.setString(Constants.CRASHLYTICS_KEY_ACCOUNT_ID, userId)
    }

    /**
     * Error consumer used to handle potential errors caused by the NetworkService while processing
     * incoming data.
     */
    private fun handleError(throwable: Throwable){
        Log.e(TAG, "Error while processing received message. Msg: " + throwable.message)
        val stack = throwable.stackTrace
        for (e in stack) {
            Log.e(TAG, String.format("%s#%s:%d", e.className, e.methodName, e.lineNumber))
        }
        Crashlytics.log(Log.ERROR, TAG, "ConnectedActivity reporting error. Msg: ${throwable.message}")
    }

    private fun handleIncomingMessage(message: Any?) {
        if (message is JsonRpcResponse<*>) {

            if (message.error == null) {
                if (responseMap.containsKey(message.id)) {
                    when (responseMap[message.id]) {
                        RESPONSE_GET_FULL_ACCOUNTS      ->
                            handleAccountDetails((message.result as List<*>)[0] as FullAccountDetails)

                        RESPONSE_GET_ACCOUNTS           ->
                            handleAccountProperties(message.result as List<AccountProperties>)

                        RESPONSE_GET_ACCOUNT_BALANCES   ->
                            handleBalanceUpdate(message.result as List<AssetAmount>)

                        RESPONSE_GET_ASSETS             ->
                            handleAssets(message.result as List<Asset>)

                        RESPONSE_GET_BLOCK_HEADER       -> {
                            val blockNumber = requestIdToBlockNumberMap[message.id] ?: 0L
                            handleBlockHeader(message.result as BlockHeader, blockNumber)
                            requestIdToBlockNumberMap.remove(message.id)
                        }
                        RESPONSE_GET_MARKET_HISTORY     -> handleMarketData(message.result as List<BucketObject>)
                    }
                    responseMap.remove(message.id)
                }
            } else {
                // In case of error
                Log.e(TAG, "Got error message from full node. Msg: " + message.error.message)
                Toast.makeText(
                    this@ConnectedActivity,
                    String.format("Error from full node. Msg: %s", message.error.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        } else if (message is ConnectionStatusUpdate) {
            if (message.updateCode == ConnectionStatusUpdate.CONNECTED) {
                // Make sure the Crashlytics report contains the currently selected node
                val selectedNode = mNetworkService?.selectedNode
                Crashlytics.log(selectedNode?.url)
            } else if (message.updateCode == ConnectionStatusUpdate.DISCONNECTED) {
                // If we got a disconnection notification, we should clear our response map, since
                // all its stored request ids will now be reset
                responseMap.clear()
            } else if (message.updateCode == ConnectionStatusUpdate.API_UPDATE) {
                // If we got an API update
                if(message.api == ApiAccess.API_HISTORY) {
                    // Starts the procedure that will obtain the missing equivalent values
                    mTransferViewModel
                        .getTransfersWithMissingBtsValue().observe(this, Observer<Transfer> {
                            if(it != null) handleTransfersWithMissingBtsValue(it)
                        })
                }
            }
        }
    }

    /**
     * Method called whenever we get a list of transfers with their bts value missing.
     */
    private fun handleTransfersWithMissingBtsValue(transfer: Transfer) {
        if(mNetworkService?.isConnected == true){
            val base = Asset(transfer.transferAssetId)
            val quote = Asset("1.3.0")
            val bucket: Long = TimeUnit.SECONDS.convert(1, TimeUnit.DAYS)
            val end: Long = transfer.timestamp * 1000L
            val start: Long = (transfer.timestamp - bucket) * 1000L
            val id = mNetworkService!!.sendMessage(GetMarketHistory(base, quote, bucket, start, end), GetMarketHistory.REQUIRED_API)
            responseMap[id] = RESPONSE_GET_MARKET_HISTORY
            this.transfer = transfer
        }
    }

    /**
     * Method called whenever a response to the 'get_full_accounts' API call has been detected.
     * @param accountDetails    De-serialized account details object
     */
    private fun handleAccountDetails(accountDetails: FullAccountDetails) {
        val latestOpCount = accountDetails.statistics.total_ops
        Log.d(TAG, "handleAccountDetails. prev count: $storedOpCount, current count: $latestOpCount")

        if (latestOpCount == 0L) {
            Log.d(TAG, "The node returned 0 total_ops for current account and may not have installed the history plugin. " +
                    "\nAsk the NetworkService to remove the node from the list and connect to another one.")
            mNetworkService?.reconnectNode()
        } else if (storedOpCount == -1L) {
            // Initial case when the app starts
            storedOpCount = latestOpCount
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putLong(Constants.KEY_ACCOUNT_OPERATION_COUNT, latestOpCount).apply()
            TransfersLoader(this)
            updateBalances()
        } else if (latestOpCount > storedOpCount) {
            storedOpCount = latestOpCount
            TransfersLoader(this)
            updateBalances()
        }
    }

    /**
     * Receives a list of missing [AccountProperties] from which it extracts the required information to
     * create a list of BiTSy's UserAccount objects and stores them into the database
     */
    private fun handleAccountProperties(accountPropertiesList: List<AccountProperties>) {
        val userAccounts = ArrayList<cy.agorise.bitsybitshareswallet.database.entities.UserAccount>()

        for (accountProperties in accountPropertiesList) {
            val userAccount = cy.agorise.bitsybitshareswallet.database.entities.UserAccount(
                accountProperties.id,
                accountProperties.name,
                accountProperties.membership_expiration_date == Constants.LIFETIME_EXPIRATION_DATE
            )

            userAccounts.add(userAccount)
        }

        mUserAccountViewModel.insertAll(userAccounts)
        missingUserAccounts.clear()
    }

    private fun handleBalanceUpdate(assetAmountList: List<AssetAmount>) {
        val now = System.currentTimeMillis() / 1000
        val balances = ArrayList<Balance>()
        for (assetAmount in assetAmountList) {
            val balance = Balance(
                assetAmount.asset.objectId,
                assetAmount.amount.toLong(),
                now
            )

            balances.add(balance)
        }

        mBalanceViewModel.insertAll(balances)
    }

    /**
     * Receives a list of missing [Asset] from which it extracts the required information to
     * create a list of BiTSy's Asset objects and stores them into the database
     */
    private fun handleAssets(_assets: List<Asset>) {
        val assets = ArrayList<cy.agorise.bitsybitshareswallet.database.entities.Asset>()

        for (_asset in _assets) {
            val asset = cy.agorise.bitsybitshareswallet.database.entities.Asset(
                _asset.objectId,
                _asset.symbol,
                _asset.precision,
                _asset.description ?: "",
                _asset.issuer ?: ""
            )

            assets.add(asset)
        }

        mAssetRepository.insertAll(assets)
        missingAssets.clear()
    }

    /**
     * Receives the [BlockHeader] related to a Transfer's missing time and saves it into the database.
     */
    private fun handleBlockHeader(blockHeader: BlockHeader, blockNumber: Long) {

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")

        try {
            val date = dateFormat.parse(blockHeader.timestamp)
            mTransferViewModel.setBlockTime(blockNumber, date.time / 1000)
        } catch (e: ParseException) {
            Log.e(TAG, "ParseException. Msg: " + e.message)
        }
    }

    private fun handleMarketData(buckets: List<BucketObject>) {
        if(buckets.isNotEmpty()){
            Log.d(TAG,"handleMarketData. Bucket is not empty")
            val bucket = buckets[0]
            val pair = Pair(transfer, bucket)
            val disposable = Observable.just(pair)
                .subscribeOn(Schedulers.computation())
                .map { mTransferViewModel.updateBtsValue(it.first!!, it.second) }
                .subscribe({},{
                    Log.e(TAG,"Error at updateBtsValue. Msg: ${it.message}")
                    for(line in it.stackTrace) Log.e(TAG, "${line.className}#${line.methodName}:${line.lineNumber}")
                })
            mCompositeDisposable.add(disposable)
        }else{
            Log.i(TAG,"handleMarketData. Bucket IS empty")
            AsyncTask.execute { mTransferViewModel.updateBtsValue(transfer!!, Transfer.ERROR) }
        }
    }

    private fun updateBalances() {
        if (mNetworkService?.isConnected == true) {
            val id = mNetworkService!!.sendMessage(GetAccountBalances(mCurrentAccount, ArrayList()),
                GetAccountBalances.REQUIRED_API)

            responseMap[id] = RESPONSE_GET_ACCOUNT_BALANCES
        }
    }

    /**
     * Task used to verify that the app is currently connected to one of the best nodes,
     * and ask for a reconnection if it is not the case.
     */
    private val verifyConnectionToSuitableNodeTask = object : Runnable {
        override fun run() {
            Log.d(TAG, "Verifying app is connected to one of the best nodes")
            mNetworkService?.nodes?.let { nodes ->
                for ((counter, node) in nodes.withIndex()) {
                    if (counter >= BEST_NODE_THRESHOLD) {
                        // Forcing reconnection to a better node
                        mNetworkService?.reconnectNode()
                        break
                    }
                    if (node.isConnected) {
                        // App is connected to one of the best nodes
                        break
                    }
                }
            }

            mHandler.postDelayed(this, NODE_CHECK_DELAY)
        }
    }

    /**
     * Task used to obtain the missing UserAccounts from Graphenej's NetworkService.
     */
    private val mRequestMissingUserAccountsTask = object : Runnable {
        override fun run() {
            if (mNetworkService?.isConnected == true) {
                val id = mNetworkService!!.sendMessage(GetAccounts(missingUserAccounts), GetAccounts.REQUIRED_API)

                responseMap[id] = RESPONSE_GET_ACCOUNTS
            } else if (missingUserAccounts.isNotEmpty()){
                mHandler.postDelayed(this, Constants.NETWORK_SERVICE_RETRY_PERIOD)
            }
        }
    }

    /**
     * Task used to obtain the missing Assets from Graphenej's NetworkService.
     */
    private val mRequestMissingAssetsTask = object : Runnable {
        override fun run() {
            if (mNetworkService?.isConnected == true) {
                val id = mNetworkService!!.sendMessage(GetAssets(missingAssets), GetAssets.REQUIRED_API)

                responseMap[id] = RESPONSE_GET_ASSETS
            } else if (missingAssets.isNotEmpty()){
                mHandler.postDelayed(this, Constants.NETWORK_SERVICE_RETRY_PERIOD)
            }
        }
    }

    /**
     * Task used to perform a redundant payment check.
     */
    private val mCheckMissingPaymentsTask = object : Runnable {
        override fun run() {
            if (mNetworkService?.isConnected == true) {
                if (mCurrentAccount != null) {
                    val userAccounts = ArrayList<String>()
                    userAccounts.add(mCurrentAccount!!.objectId)
                    val id = mNetworkService!!.sendMessage(GetFullAccounts(userAccounts, false),
                        GetFullAccounts.REQUIRED_API)

                    responseMap[id] = RESPONSE_GET_FULL_ACCOUNTS
                }
            } else {
                Log.w(TAG, "NetworkService is null or is not connected. mNetworkService: $mNetworkService")
            }
            mHandler.postDelayed(this, Constants.MISSING_PAYMENT_CHECK_PERIOD)

        }
    }

    /**
     * Task used to obtain the missing time from a block from Graphenej's NetworkService.
     */
    private val mRequestBlockMissingTimeTask = object : Runnable {
        override fun run() {

            if (mNetworkService?.isConnected == true) {
                val id = mNetworkService!!.sendMessage(GetBlockHeader(blockNumberWithMissingTime),
                    GetBlockHeader.REQUIRED_API)

                responseMap[id] = RESPONSE_GET_BLOCK_HEADER
                requestIdToBlockNumberMap[id] = blockNumberWithMissingTime
            } else {
                mHandler.postDelayed(this, Constants.MISSING_PAYMENT_CHECK_PERIOD)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mHandler.postDelayed(mCheckMissingPaymentsTask, Constants.MISSING_PAYMENT_CHECK_PERIOD)
        mHandler.postDelayed(verifyConnectionToSuitableNodeTask, NODE_CHECK_DELAY)
    }

    override fun onPause() {
        super.onPause()
        mNetworkService?.nodeLatencyVerifier?.nodeList?.let { nodes ->
            mConnectedActivityViewModel.updateNodeLatencies(nodes as List<FullNode>)
        }

        mHandler.removeCallbacks(mCheckMissingPaymentsTask)
        mHandler.removeCallbacks(mRequestMissingUserAccountsTask)
        mHandler.removeCallbacks(mRequestMissingAssetsTask)
        mHandler.removeCallbacks(mRequestBlockMissingTimeTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!mCompositeDisposable.isDisposed) mCompositeDisposable.dispose()
    }
}