package cy.agorise.bitsybitshareswallet.apigenerator

import android.content.Context
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.bitsybitshareswallet.models.*
import cy.agorise.bitsybitshareswallet.network.CryptoNetManager
import cy.agorise.bitsybitshareswallet.network.WebSocketThread
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetEvents
import cy.agorise.bitsybitshareswallet.requestmanagers.ReceivedFundsCryptoNetEvent
import cy.agorise.graphenej.*
import cy.agorise.graphenej.api.*
import cy.agorise.graphenej.interfaces.NodeErrorListener
import cy.agorise.graphenej.interfaces.SubscriptionListener
import cy.agorise.graphenej.interfaces.WitnessResponseListener
import cy.agorise.graphenej.models.*
import cy.agorise.graphenej.operations.TransferOperation
import java.io.Serializable
import java.util.*

object GrapheneApiGenerator {

    //TODO make to work with all Graphene type, not only bitshares

    // The message broker for bitshares
    private val bitsharesSubscriptionHub = SubscriptionMessagesHub("", "", true, NodeErrorListener {
        //TODO subcription hub error
        println("GrapheneAPI error")
    })

    /**
     * The subscription thread for the real time updates
     */
    private val subscriptionThread =
        WebSocketThread(bitsharesSubscriptionHub, CryptoNetManager.getURL(CryptoNet.BITSHARES)!!)
    /**
     * This is used for manager each listener in the subscription thread
     */
    private val currentBitsharesListener = HashMap<Long, SubscriptionListener>()

    /**
     * Retrieves the data of an account searching by it's id
     *
     * @param accountId The accountId to retrieve
     * @param request The Api request object, to answer this petition
     */
    fun getAccountById(accountId: String, request: ApiRequest) {
        val thread = WebSocketThread(GetAccounts(accountId,
            object : WitnessResponseListener {
                override fun onSuccess(response: WitnessResponse<*>) {
                    if (response.result.javaClass == ArrayList::class.java) {
                        val list = response.result as List<*>
                        if (list.size > 0) {
                            if (list[0] == AccountProperties::class.java) {
                                request.listener.success(list[0], request.id)
                                return
                            }
                        }
                    }
                    request.listener.fail(request.id)
                }

                override fun onError(error: BaseResponse.Error) {
                    request.listener.fail(request.id)
                }
            }), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!
        )
        thread.start()
    }

    /**
     * Gets the account ID from an owner or active key
     *
     * @param address The address to retrieve
     * @param request The Api request object, to answer this petition
     */
    fun getAccountByOwnerOrActiveAddress(address: Address, request: ApiRequest) {
        val thread = WebSocketThread(GetKeyReferences(address, true,
            object : WitnessResponseListener {
                override fun onSuccess(response: WitnessResponse<*>) {
                    try {
                        val resp = response.result as List<List<UserAccount>>
                        if (resp.size > 0) {
                            val accounts = resp[0]
                            if (accounts.size > 0) {
                                for (account in accounts) {
                                    request.listener.success(account, request.id)
                                    break
                                }
                            } else {
                                request.listener.fail(request.id)
                            }
                        } else {
                            request.listener.fail(request.id)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        request.listener.fail(request.id)
                    }

                }

                override fun onError(error: BaseResponse.Error) {
                    request.listener.fail(request.id)
                }
            }), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!
        )

        thread.start()
    }

    /**
     * Gets the Transaction for an Account
     *
     * @param accountGrapheneId The account id to search
     * @param start The start index of the transaction list
     * @param stop The stop index of the transaction list
     * @param limit the maximun transactions to retrieve
     * @param request The Api request object, to answer this petition
     */
    fun getAccountTransaction(
        accountGrapheneId: String, start: Int, stop: Int,
        limit: Int, request: ApiRequest
    ) {
        val thread = WebSocketThread(GetRelativeAccountHistory(UserAccount(accountGrapheneId),
            start, limit, stop, object : WitnessResponseListener {
                override fun onSuccess(response: WitnessResponse<*>) {
                    request.listener.success(response.result, request.id)
                }

                override fun onError(error: BaseResponse.Error) {
                    request.listener.fail(request.id)
                }
            }), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!
        )
        thread.start()
    }

    /**
     * Retrieves the account id by the name of the account
     *
     * @param accountName The account Name to find
     * @param request The Api request object, to answer this petition
     */
    fun getAccountByName(accountName: String, request: ApiRequest) {
        val thread = WebSocketThread(GetAccountByName(accountName,
            object : WitnessResponseListener {
                override fun onSuccess(response: WitnessResponse<*>) {
                    val accountProperties = response.result as AccountProperties
                    if (accountProperties == null) {
                        request.listener.fail(request.id)
                    } else {
                        request.listener.success(accountProperties, request.id)
                    }
                }

                override fun onError(error: BaseResponse.Error) {
                    request.listener.fail(request.id)
                }
            }), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!
        )
        thread.start()
    }

    /**
     * Retrieves the account id by the name of the account
     *
     * @param accountName The account Name to find
     * @param request The Api request object, to answer this petition
     */
    fun getAccountIdByName(accountName: String, request: ApiRequest) {
        val thread = WebSocketThread(GetAccountByName(accountName,
            object : WitnessResponseListener {
                override fun onSuccess(response: WitnessResponse<*>) {
                    val accountProperties = response.result as AccountProperties
                    if (accountProperties == null) {
                        request.listener.success(null, request.id)
                    } else {
                        request.listener.success(accountProperties.id, request.id)
                    }
                }

                override fun onError(error: BaseResponse.Error) {
                    request.listener.fail(request.id)
                }
            }), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!
        )
        thread.start()
    }

    /**
     * Broadcast a transaction, this is use for sending funds
     *
     * @param transaction The graphene transaction
     * @param feeAsset The feeAseet, this needs only the id of the asset
     * @param request the api request object, to answer this petition
     */
    fun broadcastTransaction(
        transaction: Transaction, feeAsset: Asset,
        request: ApiRequest
    ) {
        val thread = WebSocketThread(TransactionBroadcastSequence(transaction,
            feeAsset, object : WitnessResponseListener {
                override fun onSuccess(response: WitnessResponse<*>) {
                    request.listener.success(true, request.id)
                }

                override fun onError(error: BaseResponse.Error) {
                    request.listener.fail(request.id)
                }
            }), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!
        )
        thread.start()
    }

    /**
     * This gets the asset information using only the asset name
     *
     * @param assetNames The list of the names of the assets to be retrieve
     * @param request the api request object, to answer this petition
     */
    fun getAssetByName(assetNames: ArrayList<String>, request: ApiRequest) {

        val thread = WebSocketThread(LookupAssetSymbols(assetNames, true,
            object : WitnessResponseListener {
                override fun onSuccess(response: WitnessResponse<*>) {
                    val assets = response.result as List<Asset>
                    if (assets.size <= 0) {
                        request.listener.fail(request.id)
                    } else {
                        val responseAssets = ArrayList<BitsharesAsset>()
                        for (asset in assets) {
                            //TODO asset type
                            val assetType = BitsharesAsset.Type.UIA
                            /*if(asset.getAssetType().equals(Asset.AssetType.CORE_ASSET)){
                            assetType = BitsharesAsset.Type.CORE;
                        }else if(asset.getAssetType().equals(Asset.AssetType.SMART_COIN)){
                            assetType = BitsharesAsset.Type.SMART_COIN;
                        }else if(asset.getAssetType().equals(Asset.AssetType.UIA)){
                            assetType = BitsharesAsset.Type.UIA;
                        }else if(asset.getAssetType().equals(Asset.AssetType.PREDICTION_MARKET)){
                            assetType = BitsharesAsset.Type.PREDICTION_MARKET;
                        }*/
                            val responseAsset = BitsharesAsset(
                                asset.symbol,
                                asset.precision, asset.objectId, assetType
                            )
                            responseAssets.add(responseAsset)
                        }
                        request.listener.success(responseAssets, request.id)
                    }
                }

                override fun onError(error: BaseResponse.Error) {
                    request.listener.fail(request.id)
                }
            }), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!
        )
        thread.start()
    }

    /**
     * Gets the asset ifnormation using the id of the net
     * @param assetIds The list of the ids to retrieve
     * @param request the api request object, to answer this petition
     */
    fun getAssetById(assetIds: ArrayList<String>, request: ApiRequest) {
        val assets = ArrayList<Asset>()
        for (assetId in assetIds) {
            val asset = Asset(assetId)
            assets.add(asset)
        }

        val thread = WebSocketThread(LookupAssetSymbols(assets, true, object : WitnessResponseListener {
            override fun onSuccess(response: WitnessResponse<*>) {
                val assets = response.result as List<Asset>
                if (assets.size <= 0) {
                    request.listener.fail(request.id)
                } else {
                    val responseAssets = ArrayList<BitsharesAsset>()
                    for (asset in assets) {
                        //TODO asset type
                        val assetType = BitsharesAsset.Type.UIA
                        /*if(asset.getAssetType().equals(Asset.AssetType.CORE_ASSET)){
                            assetType = BitsharesAsset.Type.CORE;
                        }else if(asset.getAssetType().equals(Asset.AssetType.SMART_COIN)){
                            assetType = BitsharesAsset.Type.SMART_COIN;
                        }else if(asset.getAssetType().equals(Asset.AssetType.UIA)){
                            assetType = BitsharesAsset.Type.UIA;
                        }else if(asset.getAssetType().equals(Asset.AssetType.PREDICTION_MARKET)){
                            assetType = BitsharesAsset.Type.PREDICTION_MARKET;
                        }*/
                        val responseAsset = BitsharesAsset(
                            asset.symbol,
                            asset.precision, asset.objectId, assetType
                        )
                        responseAssets.add(responseAsset)
                    }
                    request.listener.success(responseAssets, request.id)
                }
            }

            override fun onError(error: BaseResponse.Error) {
                request.listener.fail(request.id)
            }
        }), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!)
        thread.start()
    }

    /**
     * Subscribe a bitshares account to receive real time updates
     *
     * @param accountId The id opf the database of the account
     * @param accountBitsharesId  The bitshares id of the account
     * @param context The android context of this application
     */
    fun subscribeBitsharesAccount(
        accountId: Long, accountBitsharesId: String,
        context: Context
    ) {
        if (!currentBitsharesListener.containsKey(accountId)) {
            val db = BitsyDatabase.getAppDatabase(context)
            val bitsharesAssetDao = db!!.bitsharesAssetDao()
            val cryptoCurrencyDao = db!!.cryptoCurrencyDao()
            val balanceListener = object : SubscriptionListener {
                override fun getInterestObjectType(): ObjectType {
                    return ObjectType.TRANSACTION_OBJECT
                }

                override fun onSubscriptionUpdate(response: SubscriptionResponse) {
                    val updatedObjects = response.params[1] as List<Serializable>
                    if (updatedObjects.size > 0) {
                        for (update in updatedObjects) {
                            if (update is BroadcastedTransaction) {
                                for (operation in update.transaction.operations) {
                                    if (operation is TransferOperation) {
                                        if (operation.from.objectId == accountBitsharesId || operation.to.objectId == accountBitsharesId) {
                                            getAccountBalance(
                                                accountId,
                                                accountBitsharesId,
                                                context
                                            )
                                            val transaction = CryptoCoinTransaction()
                                            transaction.accountId = accountId
                                            transaction.amount = operation.assetAmount.amount.toLong()
                                            val info =
                                                bitsharesAssetDao.getBitsharesAssetInfoById(operation.assetAmount.asset.objectId)
                                            if (info == null) {
                                                //The cryptoCurrency is not in the database, queringfor its data
                                                val assetRequest = ApiRequest(0, object : ApiRequestListener {
                                                    override fun success(answer: Any?, idPetition: Int) {
                                                        val assets = answer as ArrayList<BitsharesAsset>
                                                        for (asset in assets) {

                                                            var currencyId: Long = -1
                                                            val cryptoCurrencyDb =
                                                                cryptoCurrencyDao.getByNameAndCryptoNet(
                                                                    (answer as BitsharesAsset).name!!,
                                                                    (answer as BitsharesAsset).cryptoNet!!.name
                                                                )

                                                            if (cryptoCurrencyDb != null) {
                                                                currencyId = cryptoCurrencyDb!!.id
                                                            } else {
                                                                val idCryptoCurrency =
                                                                    cryptoCurrencyDao.insertCryptoCurrency(asset)[0]
                                                                currencyId = idCryptoCurrency
                                                            }

                                                            val info = BitsharesAssetInfo(asset)
                                                            info.cryptoCurrencyId = currencyId
                                                            asset.id = currencyId.toInt().toLong()
                                                            bitsharesAssetDao.insertBitsharesAssetInfo(info)
                                                            saveTransaction(
                                                                transaction,
                                                                cryptoCurrencyDao.getById(info.cryptoCurrencyId),
                                                                accountBitsharesId,
                                                                operation,
                                                                context
                                                            )
                                                        }
                                                    }

                                                    override fun fail(idPetition: Int) {
                                                        //TODO error retrieving asset
                                                    }
                                                })
                                                val assets = ArrayList<String>()
                                                assets.add(operation.assetAmount.asset.objectId)
                                                getAssetById(
                                                    assets,
                                                    assetRequest
                                                )
                                            } else {
                                                saveTransaction(
                                                    transaction,
                                                    cryptoCurrencyDao.getById(info!!.cryptoCurrencyId),
                                                    accountBitsharesId,
                                                    operation,
                                                    context
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }

            currentBitsharesListener[accountId] = balanceListener
            bitsharesSubscriptionHub.addSubscriptionListener(balanceListener)

            if (!subscriptionThread.isConnected) {
                subscriptionThread.start()
            } else if (!bitsharesSubscriptionHub.isSubscribed) {
                bitsharesSubscriptionHub.resubscribe()
            }
        }
    }

    /**
     * Function to save a transaction retrieved from the update
     * @param transaction The transaction db object
     * @param currency The currency of the transaccion
     * @param accountBitsharesId The id of the account in the bitshares network
     * @param tOperation The transfer operation fetched from the update
     * @param context The context of this app
     */
    private fun saveTransaction(
        transaction: CryptoCoinTransaction, currency: CryptoCurrency,
        accountBitsharesId: String, tOperation: TransferOperation,
        context: Context
    ) {
        transaction.idCurrency = currency.id as Int
        transaction.isConfirmed = true //graphene transaction are always confirmed
        transaction.from = tOperation.from.objectId
        transaction.input = tOperation.from.objectId != accountBitsharesId
        transaction.to = tOperation.to.objectId
        transaction.date = Date()
        BitsyDatabase.getAppDatabase(context)!!.transactionDao().insertTransaction(transaction)
        if (transaction.input) {
            CryptoNetEvents.getInstance()!!.fireEvent(ReceivedFundsCryptoNetEvent(transaction.account, currency, transaction.amount))
        }
    }

    /**
     * Cancels all bitshares account subscriptions
     */
    fun cancelBitsharesAccountSubscriptions() {
        bitsharesSubscriptionHub.cancelSubscriptions()
    }

    /**
     * Retrieve the account balance of an account
     *
     * @param accountId The dataabase id of the account
     * @param accountGrapheneId The bitshares id of the account
     * @param context The android context of this application
     */
    fun getAccountBalance(
        accountId: Long, accountGrapheneId: String,
        context: Context
    ) {

        val db = BitsyDatabase.getAppDatabase(context)
        val balanceDao = db!!.cryptoCoinBalanceDao()
        val bitsharesAssetDao = db!!.bitsharesAssetDao()
        val cryptoCurrencyDao = db!!.cryptoCurrencyDao()
        val thread = WebSocketThread(GetAccountBalances(UserAccount(accountGrapheneId),
            ArrayList(), object : WitnessResponseListener {
                override fun onSuccess(response: WitnessResponse<*>) {
                    val balances = response.result as List<AssetAmount>
                    for (balance in balances) {
                        val ccBalance = CryptoCoinBalance()
                        ccBalance.accountId = accountId
                        ccBalance.balance  = balance.amount.toLong()
                        val assetInfo = bitsharesAssetDao.getBitsharesAssetInfoById(balance.asset.objectId)
                        if (assetInfo == null) {
                            val idAssets = ArrayList<String>()
                            idAssets.add(balance.asset.objectId)
                            val getAssetRequest = ApiRequest(1, object : ApiRequestListener {
                                override fun success(answer: Any?, idPetition: Int) {
                                    val assets = answer as List<BitsharesAsset>
                                    for (asset in assets) {
                                        val info = BitsharesAssetInfo(asset)

                                        var currencyId: Long = -1
                                        val cryptoCurrencyDb = cryptoCurrencyDao.getByNameAndCryptoNet(
                                            asset.name!!,
                                            asset.cryptoNet!!.name
                                        )

                                        if (cryptoCurrencyDb != null) {
                                            currencyId = cryptoCurrencyDb!!.id
                                        } else {
                                            val cryptoCurrencyId =
                                                cryptoCurrencyDao.insertCryptoCurrency(asset as CryptoCurrency)
                                            currencyId = cryptoCurrencyId[0]
                                        }
                                        info.cryptoCurrencyId = currencyId
                                        bitsharesAssetDao.insertBitsharesAssetInfo(info)
                                        ccBalance.cryptoCurrencyId = currencyId
                                        balanceDao.insertCryptoCoinBalance(ccBalance)
                                    }
                                }

                                override fun fail(idPetition: Int) {}
                            })
                            getAssetById(
                                idAssets,
                                getAssetRequest
                            )

                        } else {

                            ccBalance.cryptoCurrencyId = assetInfo!!.cryptoCurrencyId
                            balanceDao.insertCryptoCoinBalance(ccBalance)
                        }
                    }
                }

                override fun onError(error: BaseResponse.Error) {

                }
            }), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!
        )

        thread.start()

    }

    /**
     * Gets the date time of a block header
     *
     * @param blockHeader The block header to retrieve the date time
     * @param request the api request object, to answer this petition
     */
    fun getBlockHeaderTime(blockHeader: Long, request: ApiRequest) {
        val thread = WebSocketThread(GetBlockHeader(blockHeader, object : WitnessResponseListener {
            override fun onSuccess(response: WitnessResponse<*>?) {
                if (response == null) {
                    request.listener.fail(request.id)
                } else {
                    request.listener.success(response.result, request.id)
                }
            }

            override fun onError(error: BaseResponse.Error) {
                request.listener.fail(request.id)
            }
        }), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!)
        thread.start()

    }

    /**
     * Gets a single equivalent value
     *
     * @param baseId The base asset bistshares id
     * @param quoteId the quote asset bitshares id
     * @param request the api request object, to answer this petition
     */
    fun getEquivalentValue(baseId: String, quoteId: String, request: ApiRequest) {
        val thread = WebSocketThread(GetLimitOrders(baseId, quoteId, 10,
            object : WitnessResponseListener {
                override fun onSuccess(response: WitnessResponse<*>) {
                    val orders = response.result as List<LimitOrder>
                    if (orders.size <= 0) {
                        //TODO indirect equivalent value
                    }
                    for (order in orders) {
                        if (order.sellPrice.base.asset.bitassetId == baseId) {
                            val converter = Converter()
                            val equiValue = converter.getConversionRate(
                                order.sellPrice,
                                Converter.BASE_TO_QUOTE
                            )
                            request.listener.success(equiValue, request.id)
                            break
                        }
                    }
                }

                override fun onError(error: BaseResponse.Error) {
                    request.listener.fail(request.id)
                }
            }), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!
        ) //todo change equivalent url for current server url
        thread.start()
    }

    /**
     * Gets equivalent value and store it on the database
     *
     * @param baseAsset The baset asset as a bitshares asset, it needs the CryptoCurrency and thge BitsharesInfo
     * @param quoteAssets The list of the qutoe assets as a full bitshares asset object
     * @param context The android context of this application
     */
    fun getEquivalentValue(
        baseAsset: BitsharesAsset,
        quoteAssets: List<BitsharesAsset>, context: Context
    ) {
        for (quoteAsset in quoteAssets) {
            val thread = WebSocketThread(
                GetLimitOrders(
                    baseAsset.bitsharesId,
                    quoteAsset.bitsharesId, 10,
                    EquivalentValueListener(
                        baseAsset,
                        quoteAsset, context
                    )
                ), CryptoNetManager.getURL(CryptoNet.BITSHARES)!!
            ) //todo change equivalent url for current server url
            thread.start()
        }
    }

    /**
     * Retrieves the equivalent value from a list of assets to a base asset
     *
     * @param baseAssetName The base asset to use
     * @param quoteAssets The list of quotes assets to query
     * @param context The Context of this Application
     */
    fun getEquivalentValue(baseAssetName: String, quoteAssets: List<BitsharesAsset>, context: Context) {
        val db = BitsyDatabase.getAppDatabase(context)
        val cryptoCurrencyDao = db!!.cryptoCurrencyDao()
        val bitsharesAssetDao = db!!.bitsharesAssetDao()
        val baseCurrency = cryptoCurrencyDao.getByName(baseAssetName, CryptoNet.BITSHARES.name)
        var info: BitsharesAssetInfo? = null
        if (baseCurrency != null) {
            info = db!!.bitsharesAssetDao().getBitsharesAssetInfo(baseCurrency!!.id)
        }
        if (baseCurrency == null || info == null) {
            val getAssetRequest = ApiRequest(1, object : ApiRequestListener {
                override fun success(answer: Any?, idPetition: Int) {
                    if (answer is BitsharesAsset) {
                        val info = BitsharesAssetInfo(answer as BitsharesAsset)

                        var currencyId: Long = -1
                        val cryptoCurrencyDb = cryptoCurrencyDao.getByNameAndCryptoNet(
                            (answer as BitsharesAsset).name!!,
                            (answer as BitsharesAsset).cryptoNet!!.name
                        )

                        if (cryptoCurrencyDb != null) {
                            currencyId = cryptoCurrencyDb!!.id
                        } else {
                            val cryptoCurrencyId = cryptoCurrencyDao.insertCryptoCurrency(answer as CryptoCurrency)[0]
                            currencyId = cryptoCurrencyId
                        }

                        info.cryptoCurrencyId = currencyId
                        bitsharesAssetDao.insertBitsharesAssetInfo(info)
                        getEquivalentValue(
                            answer as BitsharesAsset,
                            quoteAssets,
                            context
                        )
                    }
                }

                override fun fail(idPetition: Int) {
                    //TODO fail asset petition, the base asset is not an asset in bitshares, or there is no connection to the server
                }
            })
            val names = ArrayList<String>()
            names.add(baseAssetName)
            getAssetByName(names, getAssetRequest)

        } else {
            val baseAsset = BitsharesAsset(baseCurrency)
            baseAsset.loadInfo(info)
            getEquivalentValue(
                baseAsset,
                quoteAssets,
                context
            )
        }


    }

    /**
     * Listener of the equivalent value the answer is stored in the database, for use in conjuntion with LiveData
     */
    private class EquivalentValueListener(
        /**
         * The base asset
         */
        private val baseAsset: BitsharesAsset,
        /**
         * The quote asset
         */
        private val quoteAsset: BitsharesAsset,
        /**
         * The android context of this application
         */
        private val context: Context
    ) : WitnessResponseListener {

        override fun onSuccess(response: WitnessResponse<*>) {
            val orders = response.result as List<LimitOrder>
            if (orders.size <= 0) {
                //TODO indirect equivalent value
            }
            for (order in orders) {
                try {
                    //if (order.getSellPrice().base.getAsset().getBitassetId().equals(baseAsset.getBitsharesId())) {
                    val converter = Converter()
                    order.sellPrice.base.asset.precision = baseAsset.precision
                    order.sellPrice.quote.asset.precision = quoteAsset.precision
                    val equiValue = converter.getConversionRate(order.sellPrice, Converter.BASE_TO_QUOTE)
                    val equivalence = CryptoCurrencyEquivalence(
                        baseAsset.id,
                        quoteAsset.id,
                        (Math.pow(10.0, baseAsset.precision.toDouble()) * equiValue).toInt(),
                        Date()
                    )
                    BitsyDatabase.getAppDatabase(context)!!.cryptoCurrencyEquivalenceDao()
                        .insertCryptoCurrencyEquivalence(equivalence)
                    break
                    //}
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

        override fun onError(error: BaseResponse.Error) {

        }
    }

}