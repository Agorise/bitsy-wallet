package cy.agorise.bitsybitshareswallet.manager

import android.content.Context
import cy.agorise.bitsybitshareswallet.apigenerator.ApiRequest
import cy.agorise.bitsybitshareswallet.apigenerator.ApiRequestListener
import cy.agorise.bitsybitshareswallet.apigenerator.InsightApiGenerator
import cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models.Txi
import cy.agorise.bitsybitshareswallet.dao.BitcoinAddressDao
import cy.agorise.bitsybitshareswallet.dao.CrystalDatabase
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.enums.CryptoNetAccount
import cy.agorise.bitsybitshareswallet.models.*
import cy.agorise.bitsybitshareswallet.requestmanagers.BitcoinSendRequest
import cy.agorise.bitsybitshareswallet.requestmanagers.CreateBitcoinAccountRequest
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequest
import cy.agorise.bitsybitshareswallet.requestmanagers.CryptoNetInfoRequestsListener
import cy.agorise.graphenej.Util
import org.bitcoinj.core.*
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.script.Script
import java.util.*

class GeneralAccountManager(internal val cryptoCoin: CryptoCoin, internal val context: Context) : CryptoAccountManager,
    CryptoNetInfoRequestsListener {

    init {
        generalAccountManagers[cryptoCoin] = this
    }

    override fun createAccountFromSeed(account: CryptoNetAccount, request: ManagerRequest, context: Context) {

    }

    override fun importAccountFromSeed(account: CryptoNetAccount, context: Context) {

    }

    override fun loadAccountFromDB(account: CryptoNetAccount, context: Context) {
        val db = CrystalDatabase.getAppDatabase(context)

        val seed = db!!.accountSeedDao().findById(account.seedId)
        val purposeKey = HDKeyDerivation.deriveChildKey(
            seed.privateKey as DeterministicKey,
            ChildNumber(44, true)
        )
        val coinKey = HDKeyDerivation.deriveChildKey(
            purposeKey,
            ChildNumber(cryptoCoin.coinNumber, true)
        )
        val accountKey = HDKeyDerivation.deriveChildKey(
            coinKey,
            ChildNumber(account.accountIndex, true)
        )
        val externalKey = HDKeyDerivation.deriveChildKey(
            accountKey,
            ChildNumber(0, false)
        )
        val changeKey = HDKeyDerivation.deriveChildKey(
            accountKey,
            ChildNumber(1, false)
        )

        val indexExternal = db!!.bitcoinAddressDao().getLastExternalAddress(account.id)
        if (indexExternal > 0) {
            for (i in 0 until indexExternal) {
                val address = db!!.bitcoinAddressDao().getExternalByIndex(i)
                InsightApiGenerator.getTransactionFromAddress(cryptoCoin, address.address, true, null)
            }
        } else {
            val externalAddrKey = HDKeyDerivation.deriveChildKey(externalKey, ChildNumber(0, true))
            val address = BitcoinAddress()
            address.isChange = false
            address.accountId = account.id
            address.index = 0
            val addressString = externalAddrKey.toAddress(this.cryptoCoin.parameters).toString()
            address.address = addressString
            db!!.bitcoinAddressDao().insertBitcoinAddresses(address)
            InsightApiGenerator.getTransactionFromAddress(
                cryptoCoin, addressString, true,
                CheckAddressForTransaction(db!!.bitcoinAddressDao(), account.id, externalKey, false, 0)
            )
        }

        val indexChange = db!!.bitcoinAddressDao().getLastChangeAddress(account.id)
        if (indexChange > 0) {
            for (i in 0 until indexChange) {
                val address = db!!.bitcoinAddressDao().getChangeByIndex(i)
                InsightApiGenerator.getTransactionFromAddress(cryptoCoin, address.address, true, null)
            }
        } else {
            val changeAddrKey = HDKeyDerivation.deriveChildKey(changeKey, ChildNumber(0, true))
            val address = BitcoinAddress()
            address.isChange = true
            address.accountId = account.id
            address.index = 0
            val addressString = changeAddrKey.toAddress(this.cryptoCoin.parameters).toString()
            address.address = addressString
            db!!.bitcoinAddressDao().insertBitcoinAddresses(address)
            InsightApiGenerator.getTransactionFromAddress(
                cryptoCoin, addressString, true,
                CheckAddressForTransaction(db!!.bitcoinAddressDao(), account.id, externalKey, true, 0)
            )
        }
    }


    override fun onNewRequest(request: CryptoNetInfoRequest) {
        //if(Arrays.asList(SUPPORTED_COINS).contains(request.coin)){
        if (request.coin.equals(this.cryptoCoin)) {
            if (request is BitcoinSendRequest) {
            } else if (request is CreateBitcoinAccountRequest) {
                this.createGeneralAccount(request as CreateBitcoinAccountRequest)
            } else if (request is NextBitcoinAccountAddressRequest) {
                this.getNextAddress(request as NextBitcoinAccountAddressRequest)
            } else {
                System.out.println("Invalid " + this.cryptoCoin.label + " request ")
            }

        }

    }

    /**
     * Class that process each transaction fetched by the insight api
     * @param txi
     */
    fun processTxi(txi: Txi) {
        val db = CrystalDatabase.getAppDatabase(this.context)
        val btTransactions = db!!.bitcoinTransactionDao().getTransactionsByTxid(txi.txid!!)
        if (!btTransactions.isEmpty()) {
            for (btTransaction in btTransactions) {
                btTransaction.confirmations = txi.confirmations
                val ccTransaction = db!!.transactionDao().getById(btTransaction.cryptoCoinTransactionId)
                if (!ccTransaction.isConfirmed && btTransaction.confirmations >= cryptoCoin.cryptoNet.confirmationsNeeded) {
                    ccTransaction.isConfirmed = true
                    db!!.transactionDao().insertTransaction(ccTransaction)
                    updateBalance(
                        ccTransaction,
                        (if (ccTransaction.input) 1 else -1) * ccTransaction.amount,
                        db
                    )
                }

                db!!.bitcoinTransactionDao().insertBitcoinTransaction(btTransaction)
            }
        } else {
            /*List<CryptoCoinTransaction> ccTransactions = new ArrayList();
            btTransactions = new ArrayList();*/ //TODO transactions involving multiples accounts
            val ccTransaction = CryptoCoinTransaction()
            val btTransaction = BitcoinTransaction()
            btTransaction.txId = txi.txid!!
            btTransaction.block = txi.blockheight.toLong()
            btTransaction.fee = (txi.fee * Math.pow(10.0, cryptoCoin.precision.toDouble())) as Long
            btTransaction.confirmations = txi.confirmations
            ccTransaction.date = Date(txi.time * 1000)
            if (txi.txlock || txi.confirmations >= cryptoCoin.cryptoNet.confirmationsNeeded) {
                ccTransaction.isConfirmed = true
            } else {
                ccTransaction.isConfirmed = false
            }

            ccTransaction.input = false

            var amount: Long = 0


            //transaction.setAccount(this.mAccount);
            //transaction.setType(cryptoCoin);
            val gtxios = ArrayList<BitcoinTransactionGTxIO>()
            for (vin in txi.vin!!) {
                val input = BitcoinTransactionGTxIO()
                val addr = vin.addr
                input.address = addr!!
                input.index = vin.n
                input.isOutput = true
                input.amount = (vin.value * Math.pow(10.0, cryptoCoin.precision.toDouble())) as Long
                input.originalTxId = vin.txid!!
                input.scriptHex = vin.scriptSig!!.hex!!

                val address = db!!.bitcoinAddressDao().getdadress(addr)
                if (address != null) {
                    if (ccTransaction.accountId < 0) {
                        ccTransaction.accountId = address.accountId
                        ccTransaction.from = addr
                        ccTransaction.input = false
                    }

                    if (ccTransaction.accountId === address!!.accountId) {
                        amount -= vin.value.toLong()
                    }
                }

                if (ccTransaction.from == null || ccTransaction.from!!.isEmpty()) {
                    ccTransaction.from = addr
                }

                gtxios.add(input)


            }

            for (vout in txi.vout!!) {
                if (vout.scriptPubKey!!.addresses == null || vout.scriptPubKey!!.addresses!!.size <= 0) {

                } else {
                    val output = BitcoinTransactionGTxIO()
                    val addr = vout.scriptPubKey!!.addresses!![0]
                    output.address = addr
                    output.index = vout.n
                    output.isOutput = false
                    output.amount = (vout.value * Math.pow(10.0, cryptoCoin.precision.toDouble())).toLong()
                    output.scriptHex = vout.scriptPubKey!!.hex!!
                    output.originalTxId = txi.txid!!

                    gtxios.add(output)
                    val address = db!!.bitcoinAddressDao().getdadress(addr)
                    if (address != null) {
                        if (ccTransaction.accountId < 0) {
                            ccTransaction.accountId = address.accountId
                            ccTransaction.input = true
                            ccTransaction.to = addr
                        }

                        if (ccTransaction.accountId === address!!.accountId) {
                            amount += vout.value.toLong()
                        }
                    } else {
                        //TOOD multiple send address
                        if (ccTransaction.to == null || ccTransaction.to!!.isEmpty()) {
                            ccTransaction.to = addr
                        }
                    }
                }
            }

            ccTransaction.amount = amount
            var currency = db!!.cryptoCurrencyDao()
                .getByNameAndCryptoNet(this.cryptoCoin.name, this.cryptoCoin.cryptoNet.name)
            if (currency == null) {
                currency = CryptoCurrency()
                currency.cryptoNet = this.cryptoCoin.cryptoNet
                currency.name = this.cryptoCoin.name
                currency.precision = this.cryptoCoin.precision
                val idCurrency = db!!.cryptoCurrencyDao().insertCryptoCurrency(currency)[0]
                currency.id = idCurrency
            }

            ccTransaction.idCurrency = currency.id as Int

            val ccId = db!!.transactionDao().insertTransaction(ccTransaction)[0]
            btTransaction.cryptoCoinTransactionId = ccId
            val btId = db!!.bitcoinTransactionDao().insertBitcoinTransaction(btTransaction)[0]
            for (gtxio in gtxios) {
                gtxio.bitcoinTransactionId = btId
                db!!.bitcoinTransactionDao().insertBitcoinTransactionGTxIO(gtxio)
            }

            if (ccTransaction.isConfirmed) {
                updateBalance(ccTransaction, amount, db)
            }
        }
    }

    private fun createGeneralAccount(request: CreateBitcoinAccountRequest) {
        val db = CrystalDatabase.getAppDatabase(this.context)
        val account = CryptoNetAccount()
        account.accountIndex = 0
        account.cryptoNet = this.cryptoCoin.cryptoNet
        account.name = request.accountSeed.name
        account.seedId = request.accountSeed.id
        val idAccount = db!!.cryptoNetAccountDao().insertCryptoNetAccount(account)[0]
        account.id = idAccount

        loadAccountFromDB(account, request.context)
        request.status = CreateBitcoinAccountRequest.StatusCode.SUCCEEDED

    }

    private fun updateBalance(ccTransaction: CryptoCoinTransaction, amount: Long, db: CrystalDatabase) {
        var currency =
            db!!.cryptoCurrencyDao().getByNameAndCryptoNet(this.cryptoCoin.name, this.cryptoCoin.cryptoNet.name)
        if (currency == null) {
            currency = CryptoCurrency()
            currency.cryptoNet = this.cryptoCoin.cryptoNet
            currency.name = this.cryptoCoin.name
            currency.precision = this.cryptoCoin.precision
            val idCurrency = db!!.cryptoCurrencyDao().insertCryptoCurrency(currency)[0]
            currency.id = idCurrency
        }

        var balance = db!!.cryptoCoinBalanceDao().getBalanceFromAccount(ccTransaction.accountId, currency.id)
        if (balance == null) {
            balance = CryptoCoinBalance()
            balance.accountId  = ccTransaction.accountId
            balance.cryptoCurrencyId = currency.id
            val idBalance = db!!.cryptoCoinBalanceDao().insertCryptoCoinBalance(balance)[0]
            balance.id = idBalance
        }
        balance.balance = balance.balance + amount
        db!!.cryptoCoinBalanceDao().insertCryptoCoinBalance(balance)
    }

    fun send(request: BitcoinSendRequest) {
        //TODO check server connection
        //TODO validate to address

        InsightApiGenerator.getEstimateFee(this.cryptoCoin, ApiRequest(1, object : ApiRequestListener {
            override fun success(answer: Any?, idPetition: Int) {

                val tx = Transaction(cryptoCoin.parameters)
                val currentAmount: Long = 0
                var fee: Long = -1
                val feeRate = answer as Long
                fee = 226 * feeRate

                val db = CrystalDatabase.getAppDatabase(request.context)
                db!!.bitcoinTransactionDao()

                val utxos = getUtxos(request.sourceAccount.id, db)

                if (currentAmount < request.amount + fee) {
                    request.status = BitcoinSendRequest.StatusCode.NO_BALANCE
                    return
                }
                val seed = db!!.accountSeedDao().findById(request.sourceAccount.seedId)
                val purposeKey = HDKeyDerivation.deriveChildKey(
                    seed.privateKey as DeterministicKey,
                    ChildNumber(44, true)
                )
                val coinKey = HDKeyDerivation.deriveChildKey(
                    purposeKey,
                    ChildNumber(cryptoCoin.coinNumber, true)
                )
                val accountKey = HDKeyDerivation.deriveChildKey(
                    coinKey,
                    ChildNumber(request.sourceAccount.accountIndex, true)
                )
                val externalKey = HDKeyDerivation.deriveChildKey(
                    accountKey,
                    ChildNumber(0, false)
                )
                val changeKey = HDKeyDerivation.deriveChildKey(
                    accountKey,
                    ChildNumber(1, false)
                )

                //String to an address
                val toAddr = Address.fromBase58(cryptoCoin.parameters, request.toAccount)
                tx.addOutput(Coin.valueOf(request.amount), toAddr)

                /*if(request.getMemo()!= null && !request.getMemo().isEmpty()){
                    String memo = request.getMemo();
                    if(request.getMemo().length()>40){
                        memo = memo.substring(0,40);
                    }
                    byte[]scriptByte = new byte[memo.length()+2];
                    scriptByte[0] = 0x6a;
                    scriptByte[1] = (byte) memo.length();
                    System.arraycopy(memo.getBytes(),0,scriptByte,2,memo.length());
                    Script memoScript = new Script(scriptByte);
                    tx.addOutput(Coin.valueOf(0),memoScript);
                }*/

                //Change address
                val remain = currentAmount - request.amount - fee
                if (remain > 0) {
                    var index = db!!.bitcoinAddressDao().getLastChangeAddress(request.sourceAccount.id)
                    var btAddress = db!!.bitcoinAddressDao().getChangeByIndex(index)
                    val changeAddr: Address
                    if (btAddress != null && db!!.bitcoinTransactionDao().getGtxIOByAddress(btAddress.address).size <= 0) {
                        changeAddr = Address.fromBase58(cryptoCoin.parameters, btAddress.address)

                    } else {
                        if (btAddress == null) {
                            index = 0
                        } else {
                            index++
                        }
                        btAddress = BitcoinAddress()
                        btAddress.index = index
                        btAddress.accountId = request.sourceAccount.id
                        btAddress.isChange = true
                        btAddress.address =
                            HDKeyDerivation.deriveChildKey(
                                changeKey,
                                ChildNumber(btAddress.index as Int, false)
                            ).toAddress(cryptoCoin.parameters).toString()

                        db!!.bitcoinAddressDao().insertBitcoinAddresses(btAddress)
                        changeAddr = Address.fromBase58(cryptoCoin.parameters, btAddress.address)
                    }
                    tx.addOutput(Coin.valueOf(remain), changeAddr)
                }

                for (utxo in utxos) {
                    val txHash = Sha256Hash.wrap(utxo.originalTxId)
                    val script = Script(Util.hexToBytes(utxo.scriptHex))
                    val outPoint = TransactionOutPoint(cryptoCoin.parameters, utxo.index.toLong(), txHash)
                    val btAddress = db!!.bitcoinAddressDao().getdadress(utxo.address)
                    val addrKey: ECKey

                    if (btAddress.isChange) {
                        addrKey = HDKeyDerivation.deriveChildKey(
                            changeKey,
                            ChildNumber(btAddress.index as Int, false)
                        )
                    } else {
                        addrKey = HDKeyDerivation.deriveChildKey(
                            externalKey,
                            ChildNumber(btAddress.index as Int, true)
                        )
                    }
                    tx.addSignedInput(outPoint, script, addrKey, Transaction.SigHash.ALL, true)
                }

                InsightApiGenerator.broadcastTransaction(
                    cryptoCoin,
                    Util.bytesToHex(tx.bitcoinSerialize()),
                    ApiRequest(1, object : ApiRequestListener {
                        
                        override fun success(answer: Any?, idPetition: Int) {
                            request.status = BitcoinSendRequest.StatusCode.SUCCEEDED
                        }

                        override fun fail(idPetition: Int) {
                            request.status = BitcoinSendRequest.StatusCode.PETITION_FAILED
                        }
                    })
                )
            }

            override fun fail(idPetition: Int) {
                request.status = BitcoinSendRequest.StatusCode.NO_FEE

            }
        }))
    }

    private fun getNextAddress(request: NextBitcoinAccountAddressRequest) {
        val db = CrystalDatabase.getAppDatabase(request.context)
        var index = db!!.bitcoinAddressDao().getLastExternalAddress(request.account.id)
        index++
        val seed = db!!.accountSeedDao().findById(request.account.seedId)
        val purposeKey = HDKeyDerivation.deriveChildKey(
            seed.privateKey as DeterministicKey,
            ChildNumber(44, true)
        )
        val coinKey = HDKeyDerivation.deriveChildKey(
            purposeKey,
            ChildNumber(cryptoCoin.coinNumber, true)
        )
        val accountKey = HDKeyDerivation.deriveChildKey(
            coinKey,
            ChildNumber(request.account.accountIndex, true)
        )
        val externalKey = HDKeyDerivation.deriveChildKey(
            accountKey,
            ChildNumber(0, false)
        )
        val addrKey = HDKeyDerivation.deriveChildKey(externalKey, ChildNumber(index.toInt(), true))
        val address = BitcoinAddress()
        address.isChange = false
        address.accountId = request.account.id
        address.index = index
        val addressString = addrKey.toAddress(this.cryptoCoin.parameters).toString()
        address.address = addressString
        db!!.bitcoinAddressDao().insertBitcoinAddresses(address)
        InsightApiGenerator.getTransactionFromAddress(this.cryptoCoin, addressString, true, null)

        request.address = addressString
        request.status = NextBitcoinAccountAddressRequest.StatusCode.SUCCEEDED
    }

    private fun getUtxos(accountId: Long, db: CrystalDatabase): List<BitcoinTransactionGTxIO> {
        val answer = ArrayList<BitcoinTransactionGTxIO>()
        val bTGTxI = ArrayList<BitcoinTransactionGTxIO>()
        val bTGTxO = ArrayList<BitcoinTransactionGTxIO>()
        val ccTransactions = db!!.transactionDao().getByIdAccount(accountId)
        for (ccTransaction in ccTransactions) {
            val gtxios = db!!.bitcoinTransactionDao().getGtxIOByTransaction(ccTransaction.id)
            for (gtxio in gtxios) {
                if (db!!.bitcoinAddressDao().addressExists(gtxio.address)!!) {
                    if (gtxio.isOutput) {
                        bTGTxO.add(gtxio)
                    } else {
                        bTGTxI.add(gtxio)
                    }
                }
            }
        }
        for (gtxi in bTGTxI) {
            var find = false
            for (gtxo in bTGTxO) {
                if (gtxo.originalTxId.equals(gtxi.originalTxId)) {
                    find = true
                    break
                }
            }
            if (!find) {
                answer.add(gtxi)
            }
        }

        return answer
    }

    internal inner class CheckAddressForTransaction(
        var bitcoinAddressDao: BitcoinAddressDao,
        var idAccount: Long,
        var addressKey: DeterministicKey,
        var isChange: Boolean,
        var lastIndex: Int
    ) : InsightApiGenerator.HasTransactionListener {

        override fun hasTransaction(value: Boolean) {
            if (value) {

                val externalAddrKey = HDKeyDerivation.deriveChildKey(addressKey, ChildNumber(lastIndex + 1, true))
                val address = BitcoinAddress()
                address.isChange = isChange
                address.accountId  = idAccount
                address.index = (lastIndex + 1).toLong()
                val addressString = externalAddrKey.toAddress(cryptoCoin.parameters).toString()
                address.address = addressString
                bitcoinAddressDao.insertBitcoinAddresses(address)
                InsightApiGenerator.getTransactionFromAddress(
                    cryptoCoin, addressString, true,
                    CheckAddressForTransaction(bitcoinAddressDao, idAccount, addressKey, isChange, lastIndex + 1)
                )
            }
        }
    }

    companion object {

        internal var generalAccountManagers: HashMap<CryptoCoin, GeneralAccountManager> = HashMap()

        private val SUPPORTED_COINS =
            arrayOf(CryptoCoin.BITCOIN, CryptoCoin.BITCOIN_TEST, CryptoCoin.DASH, CryptoCoin.LITECOIN)

        fun getAccountManager(coin: CryptoCoin): GeneralAccountManager? {
            return generalAccountManagers[coin]
        }
    }
}
