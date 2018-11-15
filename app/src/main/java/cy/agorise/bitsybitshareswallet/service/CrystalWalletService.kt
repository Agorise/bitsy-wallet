package cy.agorise.bitsybitshareswallet.service

import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.annotation.Nullable
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.bitsybitshareswallet.manager.BitsharesAccountManager
import cy.agorise.bitsybitshareswallet.manager.FileBackupManager
import cy.agorise.bitsybitshareswallet.models.*
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequests
import cy.agorise.bitsybitshareswallet.requestmanagers.FileServiceRequests
import cy.agorise.bitsybitshareswallet.requestmanagers.GetBitsharesAccountNameCacheRequest
import java.util.ArrayList

class CrystalWalletService : LifecycleService() {

    private val mServiceLooper: Looper? = null
    private val mServiceHandler: ServiceHandler? = null
    private var bitsharesAccountManager: BitsharesAccountManager? = null
    private var LoadAccountTransactionsThread: Thread? = null
    private var LoadBitsharesAccountNamesThread: Thread? = null
    private var LoadEquivalencesThread: EquivalencesThread? = null
    private var keepLoadingAccountTransactions: Boolean = false
    private var keepLoadingEquivalences: Boolean = false
    private var cryptoNetInfoRequests: CryptoNetInfoRequests? = null
    private var fileBackupManager: FileBackupManager? = null
    private var fileServiceRequests: FileServiceRequests? = null

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }

            stopSelf(msg.arg1)
        }
    }

    fun loadBitsharesAccountNames() {
        val service = this
        val uncachedBitsharesAccountNames =
            BitsyDatabase.getAppDatabase(service)!!.bitsharesAccountNameCacheDao().uncachedBitsharesAccountName

        /*uncachedBitsharesAccountNames.observe(service, object : Observer<List<BitsharesAccountNameCache>> {
            override fun onChanged(@Nullable bitsharesAccountNameCacheList: List<BitsharesAccountNameCache>) {
                for (nextAccountId in bitsharesAccountNameCacheList) {
                    val request = GetBitsharesAccountNameCacheRequest(service, nextAccountId.accountId!!)

                    CryptoNetInfoRequests.getInstance()!!.addRequest(request)
                }
            }
        })*/
    }

    fun loadEquivalentsValues() {
        this.keepLoadingEquivalences = true
        val service = this

        //getting the preferred currency of the user
        val preferredCurrencySetting = BitsyDatabase.getAppDatabase(service)!!.generalSettingDao()
            .getByName(GeneralSetting.SETTING_NAME_PREFERRED_CURRENCY)

        preferredCurrencySetting.observe(service, object : Observer<GeneralSetting> {
            override fun onChanged(@Nullable generalSetting: GeneralSetting?) {
                if (generalSetting != null) {
                    val preferredCurrency = BitsyDatabase.getAppDatabase(service)!!.cryptoCurrencyDao()
                        .getByNameAndCryptoNet("EUR", CryptoNet.BITSHARES.name)

                    if (preferredCurrency != null) {
                        val preferredCurrencyBitsharesInfo = BitsyDatabase.getAppDatabase(service)!!.bitsharesAssetDao()
                            .getBitsharesAssetInfoFromCurrencyId(preferredCurrency!!.id)

                        if (preferredCurrencyBitsharesInfo != null) {
                            val preferredCurrencyBitshareAsset = BitsharesAsset(preferredCurrency)
                            preferredCurrencyBitshareAsset.loadInfo(preferredCurrencyBitsharesInfo)

                            //Loading "from" currencies
                            val bitsharesAssetInfo =
                                BitsyDatabase.getAppDatabase(service)!!.bitsharesAssetDao().all

                            bitsharesAssetInfo.observe(service, object : Observer<List<BitsharesAssetInfo>> {
                                override fun onChanged(@Nullable bitsharesAssetInfos: List<BitsharesAssetInfo>) {
                                    val bitsharesAssets = ArrayList<BitsharesAsset>()
                                    val currenciesIds = ArrayList<Long>()
                                    for (bitsharesAssetInfo in bitsharesAssetInfos) {
                                        currenciesIds.add(bitsharesAssetInfo.cryptoCurrencyId)
                                    }
                                    val bitsharesCurrencies =
                                        BitsyDatabase.getAppDatabase(service)!!.cryptoCurrencyDao()
                                            .getByIds(currenciesIds)

                                    var nextAsset: BitsharesAsset
                                    for (i in bitsharesCurrencies.indices) {
                                        val nextCurrency = bitsharesCurrencies.get(i)
                                        val nextBitsharesInfo = bitsharesAssetInfos[i]
                                        nextAsset = BitsharesAsset(nextCurrency)
                                        nextAsset.loadInfo(nextBitsharesInfo)
                                        bitsharesAssets.add(nextAsset)
                                    }

                                    if (LoadEquivalencesThread != null) {
                                        LoadEquivalencesThread!!.stopLoadingEquivalences()
                                    }
                                    LoadEquivalencesThread =
                                            EquivalencesThread(service, generalSetting!!.value!!, bitsharesAssets)
                                    LoadEquivalencesThread!!.start()
                                }
                            })
                        }
                    }
                }
            }
        })
    }

    fun loadAccountTransactions() {
        this.keepLoadingAccountTransactions = true
        val thisService = this

        val db = BitsyDatabase.getAppDatabase(this)
        //final LiveData<List<CryptoNetAccount>> cryptoNetAccountList = db.cryptoNetAccountDao().all;
        val grapheneAccountInfoList = db!!.grapheneAccountInfoDao().all
        /*grapheneAccountInfoList.observe(this, object : Observer<List<GrapheneAccountInfo>> {
            override fun onChanged(@Nullable grapheneAccountInfos: List<GrapheneAccountInfo>) {
                var nextGrapheneAccount: GrapheneAccount
                for (nextGrapheneAccountInfo in grapheneAccountInfos) {
                    val nextAccount = db.cryptoNetAccountDao().getById(nextGrapheneAccountInfo.cryptoNetAccountId)
                    //GrapheneAccountInfo grapheneAccountInfo = db.grapheneAccountInfoDao().getByAccountId(nextAccount.id);
                    nextGrapheneAccount = GrapheneAccount(nextAccount)
                    nextGrapheneAccount.loadInfo(nextGrapheneAccountInfo)


                    bitsharesAccountManager!!.loadAccountFromDB(nextGrapheneAccount, thisService)
                }
            }
        })*/


        /*while(this.keepLoadingAccountTransactions){
            try{
                Log.i("Crystal Service","Searching for transactions...");
                this.bitsharesAccountManager.loadAccountFromDB();
                Thread.sleep(60000);//Sleep for 1 minutes
                // TODO search for accounts and make managers find new transactions
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }*/
    }

    override fun onCreate() {
        super.onCreate()
        //Creates a instance_ for the cryptoNetInfoRequest and the managers
        this.cryptoNetInfoRequests = CryptoNetInfoRequests.getInstance()
        this.fileServiceRequests = FileServiceRequests.getInstance()
        this.bitsharesAccountManager = BitsharesAccountManager()
        this.fileBackupManager = FileBackupManager()

        //Add the managers as listeners of the CryptoNetInfoRequest so
        //they can carry out the info requests from the ui
        this.cryptoNetInfoRequests!!.addListener(this.bitsharesAccountManager!!)

        this.fileServiceRequests!!.addListener(this.fileBackupManager!!)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (LoadAccountTransactionsThread == null) {
            LoadAccountTransactionsThread = object : Thread() {
                override fun run() {
                    loadAccountTransactions()
                }
            }
            LoadAccountTransactionsThread!!.start()
        }
        if (LoadBitsharesAccountNamesThread == null) {
            LoadBitsharesAccountNamesThread = object : Thread() {
                override fun run() {
                    loadBitsharesAccountNames()
                }
            }
            LoadBitsharesAccountNamesThread!!.start()
        }
        loadEquivalentsValues()

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("Crystal Service", "Destroying service")
    }
}
