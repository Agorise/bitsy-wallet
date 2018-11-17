package cy.agorise.bitsybitshareswallet.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.google.common.primitives.UnsignedLong
import cy.agorise.bitsybitshareswallet.apigenerator.ApiRequest
import cy.agorise.bitsybitshareswallet.apigenerator.ApiRequestListener
import cy.agorise.bitsybitshareswallet.apigenerator.BitsharesFaucetApiGenerator
import cy.agorise.bitsybitshareswallet.apigenerator.GrapheneApiGenerator
import cy.agorise.bitsybitshareswallet.apigenerator.grapheneoperation.AccountUpgradeOperationBuilder
import cy.agorise.bitsybitshareswallet.application.constant.BitsharesConstant
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.dao.TransactionDao
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.bitsybitshareswallet.enums.SeedType
import cy.agorise.bitsybitshareswallet.models.*
import cy.agorise.bitsybitshareswallet.models.seed.BIP39
import cy.agorise.bitsybitshareswallet.network.CryptoNetManager
import cy.agorise.bitsybitshareswallet.repository.RepositoryManager
import cy.agorise.bitsybitshareswallet.requestmanagers.*
import cy.agorise.graphenej.*
import cy.agorise.graphenej.models.AccountProperties
import cy.agorise.graphenej.models.BlockHeader
import cy.agorise.graphenej.models.HistoricalTransfer
import cy.agorise.graphenej.operations.TransferOperationBuilder
import org.bitcoinj.core.ECKey
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class BitsharesAccountManager : CryptoAccountManager, CryptoNetInfoRequestsListener {

    override fun createAccountFromSeed(account: CryptoNetAccount, request: ManagerRequest, context: Context) {
        if (account is GrapheneAccount) {

            val grapheneAccount = account as GrapheneAccount
            val creationRequest = ApiRequest(1, object : ApiRequestListener {
                override fun success(answer: Any?, idPetition: Int) {
                    getAccountInfoByName(grapheneAccount.name!!, object : ManagerRequest {
                        override fun success(answer: Any) {
                            val fetch = answer as GrapheneAccount
                            fetch.seedId = grapheneAccount.seedId
                            fetch.cryptoNet = grapheneAccount.cryptoNet
                            fetch.accountIndex = grapheneAccount.accountIndex

                            val db = BitsyDatabase.getAppDatabase(context)
                            val idAccount = db!!.cryptoNetAccountDao().insertCryptoNetAccount(fetch)[0]
                            fetch.id = idAccount
                            db!!.grapheneAccountInfoDao().insertGrapheneAccountInfo(GrapheneAccountInfo(fetch))
                            subscribeBitsharesAccount(fetch.id, fetch.accountId, context)
                            request.success(fetch)
                        }

                        override fun fail() {
                            request.fail()
                        }
                    })

                }

                override fun fail(idPetition: Int) {
                    request.fail()
                }
            })
            BitsharesFaucetApiGenerator.registerBitsharesAccount(
                grapheneAccount.name!!,
                Address(ECKey.fromPublicOnly(grapheneAccount.getOwnerKey(context)!!.getPubKey())).toString(),
                Address(ECKey.fromPublicOnly(grapheneAccount.getActiveKey(context)!!.getPubKey())).toString(),
                Address(ECKey.fromPublicOnly(grapheneAccount.getMemoKey(context)!!.getPubKey())).toString(),
                BitsharesConstant.FAUCET_URL, creationRequest
            )
        }
    }

    override fun importAccountFromSeed(account: CryptoNetAccount, context: Context) {
        if (account is GrapheneAccount) {
            val grapheneAccount = account as GrapheneAccount

            if (grapheneAccount.accountId == null) {
                this.getAccountInfoByName(grapheneAccount.name!!, object : ManagerRequest {
                    override fun success(answer: Any) {
                        val fetch = answer as GrapheneAccount
                        grapheneAccount.accountId = fetch.accountId
                        val db = BitsyDatabase.getAppDatabase(context)
                        val idAccount = db!!.cryptoNetAccountDao().insertCryptoNetAccount(grapheneAccount)
                        grapheneAccount.id = idAccount[0]
                        db.grapheneAccountInfoDao().insertGrapheneAccountInfo(GrapheneAccountInfo(grapheneAccount))
                        subscribeBitsharesAccount(grapheneAccount.id, grapheneAccount.accountId, context)
                    }

                    override fun fail() {
                        //TODO get account data fail
                    }
                })

            } else if (grapheneAccount.name  == null) {
                this.getAccountInfoById(grapheneAccount.accountId, object : ManagerRequest {
                    override fun success(answer: Any) {
                        val fetch = answer as GrapheneAccount
                        grapheneAccount.name = fetch.name
                        val db = BitsyDatabase.getAppDatabase(context)
                        db!!.cryptoNetAccountDao().insertCryptoNetAccount(grapheneAccount)
                        db.grapheneAccountInfoDao().insertGrapheneAccountInfo(GrapheneAccountInfo(grapheneAccount))
                        subscribeBitsharesAccount(grapheneAccount.id, grapheneAccount.accountId, context)
                    }

                    override fun fail() {
                        //TODO get account data fail
                    }
                })
            } else {
                val db = BitsyDatabase.getAppDatabase(context)
                db!!.cryptoNetAccountDao().insertCryptoNetAccount(grapheneAccount)
                db.grapheneAccountInfoDao().insertGrapheneAccountInfo(GrapheneAccountInfo(grapheneAccount))
                subscribeBitsharesAccount(grapheneAccount.id, grapheneAccount.accountId, context)
            }
        }
    }

    override fun loadAccountFromDB(account: CryptoNetAccount, context: Context) {
        if (account is GrapheneAccount) {
            val grapheneAccount = account as GrapheneAccount
            val db = BitsyDatabase.getAppDatabase(context)
            val info = db!!.grapheneAccountInfoDao().getByAccountId(account.id)
            grapheneAccount.loadInfo(info)
            if (grapheneAccount.accountId == null) {
                this.getAccountInfoByName(grapheneAccount.name!!, object : ManagerRequest {
                    override fun success(answer: Any) {
                        val fetch = answer as GrapheneAccount
                        info.accountId = fetch.accountId
                        grapheneAccount.accountId = fetch.accountId
                        db!!.grapheneAccountInfoDao().insertGrapheneAccountInfo(info)
                        subscribeBitsharesAccount(grapheneAccount.id, grapheneAccount.accountId, context)
                    }

                    override fun fail() {
                        //TODO account data retrieve failed
                    }
                })
            } else if (grapheneAccount.name  == null) {
                this.getAccountInfoById(grapheneAccount.accountId, object : ManagerRequest {
                    override fun success(answer: Any) {
                        val fetch = answer as GrapheneAccount
                        info.name = fetch.name!!
                        grapheneAccount.name = fetch.name
                        db.grapheneAccountInfoDao().insertGrapheneAccountInfo(info)
                        subscribeBitsharesAccount(grapheneAccount.id, grapheneAccount.accountId, context)
                    }

                    override fun fail() {
                        //TODO account data retrieve failed
                    }
                })
            } else {
                subscribeBitsharesAccount(grapheneAccount.id, grapheneAccount.accountId, context)
            }
        }
    }

    private fun subscribeBitsharesAccount(accountId: Long, accountBitsharesID: String, context: Context) {
        GrapheneApiGenerator.subscribeBitsharesAccount(accountId, accountBitsharesID, context)
        BitsharesAccountManager.refreshAccountTransactions(accountId, context)
        GrapheneApiGenerator.getAccountBalance(accountId, accountBitsharesID, context)
    }

    /**
     * Process the bitshares manager request
     * @param request The request Object
     */
    override fun onNewRequest(request: CryptoNetInfoRequest) {
        if (request.coin.equals(CryptoCoin.BITSHARES)) {
            if (request is ImportBitsharesAccountRequest) {
                this.importAccount(request as ImportBitsharesAccountRequest)
            } else if (request is ValidateImportBitsharesAccountRequest) {
                this.validateImportAccount(request as ValidateImportBitsharesAccountRequest)
            } else if (request is ValidateExistBitsharesAccountRequest) {
                this.validateExistAcccount(request as ValidateExistBitsharesAccountRequest)
            } else if (request is ValidateBitsharesSendRequest) {
                this.validateSendRequest(request as ValidateBitsharesSendRequest)
            } else if (request is CryptoNetEquivalentRequest) {
                this.getEquivalentValue(request as CryptoNetEquivalentRequest)
            } else if (request is ValidateCreateBitsharesAccountRequest) {
                this.validateCreateAccount(request as ValidateCreateBitsharesAccountRequest)
            } else if (request is ValidateBitsharesLTMUpgradeRequest) {
                this.validateLTMAccountUpgrade(request as ValidateBitsharesLTMUpgradeRequest)
            } else if (request is GetBitsharesAccountNameCacheRequest) {
                this.getBitsharesAccountNameCacheRequest(request as GetBitsharesAccountNameCacheRequest)
            } else {

                //TODO not implemented
                System.out.println("Error request not implemented " + request.javaClass.name )
            }
        }
    }

    private fun importAccount(importRequest: ImportBitsharesAccountRequest) {
        val db = BitsyDatabase.getAppDatabase(importRequest.context!!)
        val accountSeedDao = db!!.accountSeedDao()
        val getAccountNamesBK = ApiRequest(0, object : ApiRequestListener {
            override fun success(answer: Any?, idPetition: Int) {
                if (answer != null && importRequest.status.equals(ImportBitsharesAccountRequest.StatusCode.NOT_STARTED)) {
                    val userAccount = answer as UserAccount?
                    importRequest.seedType = SeedType.BRAINKEY
                    importRequest.status = ImportBitsharesAccountRequest.StatusCode.SUCCEEDED

                    val seed = AccountSeed()
                    seed.name = userAccount!!.name
                    seed.type = importRequest.seedType
                    seed.masterSeed = importRequest.mnemonic
                    val idSeed = accountSeedDao.insertAccountSeed(seed)
                    if (idSeed >= 0) {
                        val account = GrapheneAccount()
                        account.cryptoNet = CryptoNet.BITSHARES
                        account.accountIndex = 0
                        account.seedId = idSeed 
                        account.accountId = userAccount.objectId
                        importAccountFromSeed(account, importRequest.context!!)
                    }
                }
            }

            override fun fail(idPetition: Int) {
                val bip39 = BIP39(-1, importRequest.mnemonic)
                val getAccountNamesBP39 = ApiRequest(0, object : ApiRequestListener {
                    override fun success(answer: Any?, idPetition: Int) {
                        if (answer != null && importRequest.status.equals(ImportBitsharesAccountRequest.StatusCode.NOT_STARTED)) {
                            val userAccount = answer as UserAccount?
                            importRequest.seedType = SeedType.BRAINKEY
                            importRequest.status = ImportBitsharesAccountRequest.StatusCode.SUCCEEDED

                            val seed = AccountSeed()
                            seed.name = userAccount!!.name
                            seed.type = importRequest.seedType
                            seed.masterSeed = importRequest.mnemonic
                            val idSeed = accountSeedDao.insertAccountSeed(seed)
                            if (idSeed >= 0) {
                                val account = GrapheneAccount()
                                account.cryptoNet = CryptoNet.BITSHARES
                                account.accountIndex = 0
                                account.seedId = idSeed
                                account.accountId = userAccount.objectId
                                importAccountFromSeed(account, importRequest.context!!)
                            }
                        }
                    }

                    override fun fail(idPetition: Int) {
                        importRequest.status = ImportBitsharesAccountRequest.StatusCode.BAD_SEED
                    }
                })
                GrapheneApiGenerator.getAccountByOwnerOrActiveAddress(
                    Address(ECKey.fromPublicOnly(bip39.getBitsharesActiveKey(0).getPubKey())), getAccountNamesBP39
                )
            }
        })

        val bk = BrainKey(importRequest.mnemonic, 0)

        GrapheneApiGenerator.getAccountByOwnerOrActiveAddress(bk.getPublicAddress("BTS"), getAccountNamesBK)


    }

    /**
     * Process the import account request
     */
    private fun validateImportAccount(importRequest: ValidateImportBitsharesAccountRequest) {
        //TODO check internet and server status
        val db = BitsyDatabase.getAppDatabase(importRequest.context!!)
        val accountSeedDao = db!!.accountSeedDao()

        val checkAccountName = ApiRequest(0, object : ApiRequestListener {
            override fun success(answer: Any?, idPetition: Int) {
                val getAccountInfo = ApiRequest(1, object : ApiRequestListener {
                    override fun success(answer: Any?, idPetition: Int) {
                        if (answer != null && answer is AccountProperties) {
                            val prop = answer as AccountProperties?
                            val bk = BrainKey(importRequest.mnemonic, 0)
                            for (activeKey in prop!!.owner.keyAuthList) {
                                if (Address(activeKey.key, "BTS").toString() == bk.getPublicAddress("BTS").toString()) {
                                    importRequest.seedType = SeedType.BRAINKEY
                                    importRequest.status = ValidateImportBitsharesAccountRequest.StatusCode.SUCCEEDED

                                    break
                                }
                            }
                            val bip39 = BIP39(-1, importRequest.mnemonic)
                            for (activeKey in prop.active.keyAuthList) {
                                if (Address(
                                        activeKey.key,
                                        "BTS" 
                                    ).toString() == Address(ECKey.fromPublicOnly(bip39.getBitsharesActiveKey(0).getPubKey())).toString()
                                ) {
                                    importRequest.seedType = SeedType.BIP39
                                    importRequest.status = ValidateImportBitsharesAccountRequest.StatusCode.SUCCEEDED
                                    break
                                }
                            }

                            if (importRequest.status === ValidateImportBitsharesAccountRequest.StatusCode.SUCCEEDED) {
                                if (importRequest.addAccountIfValid()) {
                                    val seed = AccountSeed()
                                    seed.name = importRequest.accountName
                                    seed.type = importRequest.seedType
                                    seed.masterSeed = importRequest.mnemonic
                                    val idSeed = accountSeedDao.insertAccountSeed(seed)
                                    if (idSeed >= 0) {
                                        val account = GrapheneAccount()
                                        account.cryptoNet = CryptoNet.BITSHARES
                                        account.accountIndex = 0
                                        account.seedId = idSeed
                                        account.name = importRequest.accountName
                                        importAccountFromSeed(account, importRequest.context!!)
                                    }
                                }
                                return
                            }

                            importRequest.status = ValidateImportBitsharesAccountRequest.StatusCode.BAD_SEED
                        }
                        importRequest.status = ValidateImportBitsharesAccountRequest.StatusCode.PETITION_FAILED

                    }

                    override fun fail(idPetition: Int) {
                        //
                        importRequest.status = ValidateImportBitsharesAccountRequest.StatusCode.NO_ACCOUNT_DATA
                    }
                })
                GrapheneApiGenerator.getAccountById(answer as String, getAccountInfo)
            }

            override fun fail(idPetition: Int) {
                //
                importRequest.status = ValidateImportBitsharesAccountRequest.StatusCode.ACCOUNT_DOESNT_EXIST
            }
        })

        GrapheneApiGenerator.getAccountIdByName(importRequest.accountName, checkAccountName)
    }

    private fun validateCreateAccount(createRequest: ValidateCreateBitsharesAccountRequest) {
        val context = createRequest.context
        val seed = AccountSeed.getAccountSeed(SeedType.BRAINKEY, context)
        val db = BitsyDatabase.getAppDatabase(context)
        val idSeed =RepositoryManager.getAccountsRepository(context as Activity).addAccount(seed!!)
        //val idSeed = db!!.accountSeedDao().insertAccountSeed(seed!!)
        assert(seed != null)
        seed!!.id = idSeed
        seed!!.name = createRequest.accountName
        val account = GrapheneAccount()
        account.name = createRequest.accountName
        account.seedId = idSeed
        account.accountIndex = 0
        account.cryptoNet = CryptoNet.BITSHARES
        this.createAccountFromSeed(account, object : ManagerRequest {

            override fun success(answer: Any) {
                createRequest.account = answer as GrapheneAccount
                createRequest.status = ValidateCreateBitsharesAccountRequest.StatusCode.SUCCEEDED
            }

            override fun fail() {
                createRequest.status = ValidateCreateBitsharesAccountRequest.StatusCode.ACCOUNT_EXIST
            }
        }, context)
    }

    /**
     * Process the account exist request, it consults the bitshares api for the account name.
     *
     * This can be used to know if the name is avaible, or the account to be send fund exists
     */
    private fun validateExistAcccount(validateRequest: ValidateExistBitsharesAccountRequest) {
        val checkAccountName = ApiRequest(0, object : ApiRequestListener {
            override fun success(answer: Any?, idPetition: Int) {
                if (answer != null) {
                    validateRequest.setAccountExists(true)
                } else {
                    validateRequest.setAccountExists(false)
                }
            }

            override fun fail(idPetition: Int) {
                //TODO verified
                validateRequest.setAccountExists(false)
            }
        })
        GrapheneApiGenerator.getAccountIdByName(validateRequest.accountName, checkAccountName)
    }

    /**
     * Broadcast a transaction request
     */
    private fun validateSendRequest(sendRequest: ValidateBitsharesSendRequest) {
        //TODO check internet, server connection
        //TODO feeAsset
        val db = BitsyDatabase.getAppDatabase(sendRequest.context)
        val currency = db!!.cryptoCurrencyDao().getByNameAndCryptoNet(sendRequest.asset, CryptoNet.BITSHARES.name)
        if (currency == null) {
            getAssetInfoByName(sendRequest.asset, object : ManagerRequest {
                override fun success(answer: Any) {
                    validateSendRequest(sendRequest, (answer as BitsharesAsset).bitsharesId!!)
                }

                override fun fail() {
                    sendRequest.status = ValidateBitsharesSendRequest.StatusCode.NO_ASSET_INFO_DB
                }
            })
        } else {
            val info = db!!.bitsharesAssetDao().getBitsharesAssetInfo(currency!!.id)
            if (info == null || info!!.bitsharesId == null || info!!.bitsharesId!!.isEmpty()) {
                getAssetInfoByName(sendRequest.asset, object : ManagerRequest {
                    override fun success(answer: Any) {
                        validateSendRequest(sendRequest, (answer as BitsharesAsset).bitsharesId!!)
                    }

                    override fun fail() {
                        sendRequest.status = ValidateBitsharesSendRequest.StatusCode.NO_ASSET_INFO
                    }
                })
            } else {
                this.validateSendRequest(sendRequest, info!!.bitsharesId!!)
            }
        }
    }

    /**
     * Broadcast a send asset request, the idAsset is already fetched
     * @param sendRequest The petition for transfer
     * @param idAsset The Bitshares Asset's id
     */
    private fun validateSendRequest(sendRequest: ValidateBitsharesSendRequest, idAsset: String) {
        val feeAsset = Asset(idAsset)
        val fromUserAccount = UserAccount(sendRequest.sourceAccount.accountId)

        val db = BitsyDatabase.getAppDatabase(sendRequest.context)
        val cacheAccount = db!!.bitsharesAccountNameCacheDao().getByAccountName(sendRequest.toAccount)
        if (cacheAccount == null) {
            this.getAccountInfoByName(sendRequest.toAccount, object : ManagerRequest {

                override fun success(answer: Any) {
                    val toUserGrapheneAccount = answer as GrapheneAccount
                    val toUserAccount = UserAccount(toUserGrapheneAccount.accountId)
                    try {
                        val cacheAccount = BitsharesAccountNameCache()
                        cacheAccount.name = sendRequest.toAccount
                        cacheAccount.accountId = toUserAccount.objectId
                        db!!.bitsharesAccountNameCacheDao().insertBitsharesAccountNameCache(cacheAccount)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    validateSendRequest(sendRequest, fromUserAccount, toUserAccount, feeAsset, idAsset)
                }

                override fun fail() {
                    sendRequest.status = ValidateBitsharesSendRequest.StatusCode.NO_TO_USER_INFO
                }
            })
        } else {
            val toUserAccount = UserAccount(cacheAccount!!.accountId)
            this.validateSendRequest(sendRequest, fromUserAccount, toUserAccount, feeAsset, idAsset)
        }
    }

    /**
     * Broadcast a transaction request
     * @param sendRequest The petition for transfer operation
     * @param fromUserAccount The source account
     * @param toUserAccount The receiver account
     * @param feeAsset The Fee Asset
     * @param idAsset The id of the asset to be used on the operation
     */
    private fun validateSendRequest(
        sendRequest: ValidateBitsharesSendRequest, fromUserAccount: UserAccount,
        toUserAccount: UserAccount, feeAsset: Asset, idAsset: String
    ) {
        val builder = TransferOperationBuilder()
            .setSource(fromUserAccount)
            .setDestination(toUserAccount)
            .setTransferAmount(AssetAmount(UnsignedLong.valueOf(sendRequest.amount), Asset(idAsset)))
            .setFee(AssetAmount(UnsignedLong.valueOf(0), feeAsset))
        if (sendRequest.memo != null) {
            //builder.setMemo(new Memo(fromUserAccount,toUserAccount,0,sendRequest.getMemo().getBytes()));
            //TODO memo
            println("transaction has memo")
        }
        val operationList = ArrayList<BaseOperation>()
        operationList.add(builder.build())

        val privateKey = sendRequest.sourceAccount.getActiveKey(sendRequest.context)

        val transaction = Transaction(privateKey, null, operationList)
        transaction.setChainId(CryptoNetManager.getChaindId(CryptoNet.BITSHARES))

        val transactionRequest = ApiRequest(0, object : ApiRequestListener {
            override fun success(answer: Any?, idPetition: Int) {
                sendRequest.status = ValidateBitsharesSendRequest.StatusCode.SUCCEEDED
            }

            override fun fail(idPetition: Int) {
                sendRequest.status = ValidateBitsharesSendRequest.StatusCode.PETITION_FAILED
            }
        })

        GrapheneApiGenerator.broadcastTransaction(transaction, feeAsset, transactionRequest)
    }

    /**
     * Broadcast a transaction request
     */
    private fun validateLTMAccountUpgrade(sendRequest: ValidateBitsharesLTMUpgradeRequest) {
        //TODO check internet, server connection
        val feeAsset = Asset(sendRequest.idAsset)
        val fromUserAccount = UserAccount(sendRequest.sourceAccount.accountId)

        val db = BitsyDatabase.getAppDatabase(sendRequest.context)

        val builder = AccountUpgradeOperationBuilder()
            .setAccountToUpgrade(fromUserAccount)
            .setFee(AssetAmount(UnsignedLong.valueOf(0), feeAsset))
        val operationList = ArrayList<BaseOperation>()
        operationList.add(builder.build())

        val privateKey = sendRequest.sourceAccount.getActiveKey(sendRequest.context)

        val transaction = Transaction(privateKey, null, operationList)
        transaction.setChainId(CryptoNetManager.getChaindId(CryptoNet.BITSHARES))


        val transactionRequest = ApiRequest(0, object : ApiRequestListener {
            override fun success(answer: Any?, idPetition: Int) {
                sendRequest.status = ValidateBitsharesLTMUpgradeRequest.StatusCode.SUCCEEDED
            }

            override fun fail(idPetition: Int) {
                sendRequest.status = ValidateBitsharesLTMUpgradeRequest.StatusCode.PETITION_FAILED
            }
        })

        GrapheneApiGenerator.broadcastTransaction(transaction, feeAsset, transactionRequest)
    }

    private fun getBitsharesAccountNameCacheRequest(request: GetBitsharesAccountNameCacheRequest) {
        val db = BitsyDatabase.getAppDatabase(request.context)
        val cacheAccount = db!!.bitsharesAccountNameCacheDao().getByAccountId(request.accountId)
        if (cacheAccount == null) {
            this.getAccountInfoById(request.accountId, object : ManagerRequest {

                override fun success(answer: Any) {
                    val userGrapheneAccount = answer as GrapheneAccount
                    val cacheAccount = BitsharesAccountNameCache()
                    cacheAccount.name = userGrapheneAccount.name
                    cacheAccount.accountId = request.accountId
                    if (db != null) {
                        db.bitsharesAccountNameCacheDao().insertBitsharesAccountNameCache(cacheAccount)
                    }
                    request.setAccountName(userGrapheneAccount.name!!)
                }

                override fun fail() {
                    //TODO error
                }
            })
        } else {
            request.setAccountName(cacheAccount!!.name!!)
        }
    }

    /**
     * Returns the account info from a graphene id
     * @param grapheneId The graphene id of the account
     */
    private fun getAccountInfoById(grapheneId: String, request: ManagerRequest) {

        val listener = AccountIdOrNameListener(request)

        val accountRequest = ApiRequest(0, listener)
        GrapheneApiGenerator.getAccountById(grapheneId, accountRequest)
    }

    /**
     * Gets account info by its name
     * @param grapheneName The name of the account to retrieve
     */
    private fun getAccountInfoByName(grapheneName: String, request: ManagerRequest) {

        val listener = AccountIdOrNameListener(request)

        val accountRequest = ApiRequest(0, listener)
        GrapheneApiGenerator.getAccountByName(grapheneName, accountRequest)

    }

    //TODO expand function to be more generic
    private fun getAssetInfoByName(assetName: String, request: ManagerRequest) {

        val nameListener = AssetIdOrNameListener(request)
        val assetRequest = ApiRequest(0, nameListener)
        val assetNames = ArrayList<String>()
        assetNames.add(assetName)
        GrapheneApiGenerator.getAssetByName(assetNames, assetRequest)

    }

    /**
     * Class that handles the transactions request
     */
    private class TransactionRequestListener
    /**
     * Basic consturctor
     */
    internal constructor(
        /**
         * Start index
         */
        internal var start: Int,
        /**
         * End index
         */
        internal var stop: Int,
        /**
         * Limit of transasction to fetch
         */
        internal var limit: Int,
        /**
         * The grapheneaccount with all data CryptoCurrnecy + info
         */
        internal var account: GrapheneAccount,
        /**
         * The database
         */
        internal var db: BitsyDatabase
    ) : ApiRequestListener {

        /**
         * Handles the success request of the transaction, if the amount of transaction is equal to the limit, ask for more transaction
         * @param answer The answer, this object depends on the kind of request is made to the api
         * @param idPetition the id of the ApiRequest petition
         */
        override fun success(answer: Any?, idPetition: Int) {
            val transfers = answer as List<HistoricalTransfer>
            for (transfer in transfers) {
                if (transfer.operation != null) {
                    val transaction = CryptoCoinTransaction()
                    transaction.accountId = account.id
                    transaction.amount = transfer.operation!!.getAssetAmount().getAmount().toLong()
                    val info = db.bitsharesAssetDao()
                        .getBitsharesAssetInfoById(transfer.operation!!.getAssetAmount().getAsset().getObjectId())

                    if (info == null) {
                        //The cryptoCurrency is not in the database, queringfor its data
                        val assetRequest = ApiRequest(0, object : ApiRequestListener {
                            override fun success(answer: Any?, idPetition: Int) {
                                val assets = answer as ArrayList<BitsharesAsset>
                                for (asset in assets) {
                                    var currencyId: Long = -1
                                    val cryptoCurrencyDb = db.cryptoCurrencyDao()
                                        .getByNameAndCryptoNet(asset.name!!, asset.cryptoNet!!.name)

                                    if (cryptoCurrencyDb != null) {
                                        currencyId = cryptoCurrencyDb!!.id
                                    } else {
                                        val idCryptoCurrency = db.cryptoCurrencyDao().insertCryptoCurrency(asset)[0]
                                        currencyId = idCryptoCurrency
                                    }


                                    val info = BitsharesAssetInfo(asset)
                                    info.cryptoCurrencyId = currencyId
                                    asset.id = currencyId.toInt().toLong()
                                    db.bitsharesAssetDao().insertBitsharesAssetInfo(info)
                                    saveTransaction(transaction, info, transfer)
                                }

                            }

                            override fun fail(idPetition: Int) {
                                //TODO Error
                            }
                        })
                        val assets = ArrayList<String>()
                        assets.add(transfer.operation!!.getAssetAmount().getAsset().getObjectId())
                        GrapheneApiGenerator.getAssetById(assets, assetRequest)

                    } else {
                        saveTransaction(transaction, info!!, transfer)
                    }

                }
            }
            if (transfers.size >= limit) {
                // The amount of transaction in the answer is equal to the limit, we need to query to see if there is more transactions
                val newStart = start + limit
                val newStop = stop + limit
                val transactionRequest =
                    ApiRequest(newStart / limit, TransactionRequestListener(newStart, newStop, limit, account, db))
                GrapheneApiGenerator.getAccountTransaction(
                    account.accountId,
                    newStart,
                    newStop,
                    limit,
                    transactionRequest
                )
            }
        }

        override fun fail(idPetition: Int) {

        }

        private fun saveTransaction(
            transaction: CryptoCoinTransaction,
            info: BitsharesAssetInfo,
            transfer: HistoricalTransfer
        ) {
            transaction.idCurrency = info.cryptoCurrencyId as Int
            transaction.isConfirmed = true //graphene transaction are always confirmed
            transaction.from = transfer.operation!!.getFrom().getObjectId()
            transaction.input = !transfer.operation!!.getFrom().getObjectId().equals(account.accountId)
            transaction.to = transfer.operation!!.getTo().getObjectId()

            GrapheneApiGenerator.getBlockHeaderTime(
                transfer.blockNum,
                ApiRequest(0, GetTransactionDate(transaction, db.transactionDao()))
            )
        }
    }

    /**
     * Gets the current change from two assets
     */
    private fun getEquivalentValue(request: CryptoNetEquivalentRequest) {
        if (request.fromCurrency is BitsharesAsset && request.toCurrency is BitsharesAsset) {
            val fromAsset = request.fromCurrency as BitsharesAsset
            val toAsset = request.toCurrency as BitsharesAsset
            val getEquivalentRequest = ApiRequest(0, object : ApiRequestListener {
                override fun success(answer: Any?, idPetition: Int) {
                    request.setPrice(answer as Long)
                }

                override fun fail(idPetition: Int) {
                    //TODO error
                }
            })
            GrapheneApiGenerator.getEquivalentValue(
                fromAsset.bitsharesId!!,
                toAsset.bitsharesId!!,
                getEquivalentRequest
            )
        } else {
            //TODO error
            println("Equivalent Value error ")
        }
    }

    /**
     * Class to retrieve the account id or the account name, if one of those is missing
     */
    private inner class AccountIdOrNameListener internal constructor(internal val request: ManagerRequest) :
        ApiRequestListener {

        internal lateinit var account: GrapheneAccount

        override fun success(answer: Any?, idPetition: Int) {
            if (answer is AccountProperties) {
                account = GrapheneAccount()
                account.accountId = answer.id
                account.name = answer.name
            }

            request.success(account)
        }

        override fun fail(idPetition: Int) {
            request.fail()
        }
    }

    /**
     * Class to retrieve the asset id or the asset name, if one of those is missing
     */
    private inner class AssetIdOrNameListener internal constructor(internal val request: ManagerRequest) :
        ApiRequestListener {

        internal lateinit var asset: BitsharesAsset

        override fun success(answer: Any?, idPetition: Int) {
            if (answer is ArrayList<*>) {
                if (answer[0] is BitsharesAsset) {
                    asset = answer[0] as BitsharesAsset
                    request.success(asset)
                }
            }
        }

        override fun fail(idPetition: Int) {
            //TODO fail asset retrieve
        }
    }

    /**
     * Class to retrieve the transaction date
     */
    class GetTransactionDate internal constructor(
        /**
         * The transaction to retrieve
         */
        private val transaction: CryptoCoinTransaction,
        /**
         * The DAO to insert or update the transaction
         */
        internal var transactionDao: TransactionDao
    ) : ApiRequestListener {

        override fun success(answer: Any?, idPetition: Int) {
            if (answer is BlockHeader) {
                @SuppressLint("SimpleDateFormat") val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT)
                dateFormat.timeZone = TimeZone.getTimeZone(DEFAULT_TIME_ZONE)
                try {
                    transaction.date = dateFormat.parse(answer.timestamp)
                    if (transactionDao.getByTransaction(
                            transaction.date!!,
                            transaction.from!!,
                            transaction.to!!,
                            transaction.amount
                        ) == null
                    ) {
                        transactionDao.insertTransaction(transaction)
                    }
                } catch (e: ParseException) {
                    e.printStackTrace()
                }

            }
        }

        override fun fail(idPetition: Int) {

        }
    }

    companion object {

        private val SIMPLE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
        private val DEFAULT_TIME_ZONE = "GMT"

        /**
         * Refresh the transactions of an account, important to notice, it return nothing, to get the changes tuse the LiveData
         * @param idAccount database id of the account
         * @param context The android context of this application
         */
        private fun refreshAccountTransactions(idAccount: Long, context: Context) {
            val db = BitsyDatabase.getAppDatabase(context)
            val transactions = db!!.transactionDao().getByIdAccount(idAccount)
            val account = db.cryptoNetAccountDao().getById(idAccount)
            if (account.cryptoNet === CryptoNet.BITSHARES) {

                val grapheneAccount = GrapheneAccount(account)
                grapheneAccount.loadInfo(db.grapheneAccountInfoDao().getByAccountId(idAccount))

                val start = transactions.size
                val limit = 50
                val stop = start + limit

                val transactionRequest =
                    ApiRequest(0, TransactionRequestListener(start, stop, limit, grapheneAccount, db))
                GrapheneApiGenerator.getAccountTransaction(
                    grapheneAccount.accountId,
                    start,
                    stop,
                    limit,
                    transactionRequest
                )
            }
        }
    }

}
