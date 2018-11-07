package cy.agorise.bitsybitshareswallet.apigenerator

import cy.agorise.bitsybitshareswallet.apigenerator.insightapi.AddressesActivityWatcher
import cy.agorise.bitsybitshareswallet.apigenerator.insightapi.BroadcastTransaction
import cy.agorise.bitsybitshareswallet.apigenerator.insightapi.GetEstimateFee
import cy.agorise.bitsybitshareswallet.apigenerator.insightapi.GetTransactionByAddress
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.network.CryptoNetManager
import java.util.HashMap

object InsightApiGenerator {

    private val transactionGetters = HashMap<CryptoCoin,GetTransactionByAddress>()
    private val transactionFollowers = HashMap<CryptoCoin,AddressesActivityWatcher>()


    private val PATH = "api"

    /**
     * Fecth all the transaciton for a giving address
     * @param cryptoCoin the crypto net of the address
     * @param address The address String
     * @param subscribe If needs to follow the address (Real time)
     */
    fun getTransactionFromAddress(
        cryptoCoin: CryptoCoin,
        address: String,
        subscribe: Boolean,
        listener: HasTransactionListener?
    ) {
        /*if(!transactionGetters.containsKey(cryptoCoin)){
            transactionGetters.put(cryptoCoin,new GetTransactionByAddress(cryptoCoin,CryptoNetManager.getURL(cryptoCoin.getCryptoNet()),PATH));
        }
        transactionGetters.get(cryptoCoin).addAddress(address);
        transactionGetters.get(cryptoCoin).start();*/

        val transByAddr =
            GetTransactionByAddress(cryptoCoin, CryptoNetManager.getURL(cryptoCoin.cryptoNet)!!, PATH, listener)
        transByAddr.addAddress(address)
        transByAddr.start()

        if (subscribe) {
            if (!transactionFollowers.containsKey(cryptoCoin)) {
                transactionFollowers.put(
                    cryptoCoin,
                    AddressesActivityWatcher(CryptoNetManager.getURL(cryptoCoin.cryptoNet)!!, PATH, cryptoCoin)
                )
            }
            transactionFollowers.get(cryptoCoin)!!.addAddress(address)
            transactionFollowers.get(cryptoCoin)!!.connect()
        }
    }

    /**
     * Broadcast an insight api transaction
     * @param cryptoCoin The cryptoNet of the transaction
     * @param rawtx the transaction to be broadcasted
     */
    fun broadcastTransaction(cryptoCoin: CryptoCoin, rawtx: String, request: ApiRequest) {
        val bTransaction = BroadcastTransaction(rawtx,
            CryptoNetManager.getURL(cryptoCoin.cryptoNet)!!,
            PATH,
            object : BroadcastTransaction.BroadCastTransactionListener {
                override fun onSuccess() {
                    request.listener.success(true, request.id)
                }

                override fun onFailure(msg: String) {
                    request.listener.fail(request.id)
                }

                override fun onConnecitonFailure() {
                    request.listener.fail(request.id)
                }
            })
        bTransaction.start()
    }

    /**
     * Fetch the estimated fee for a transaction
     */
    fun getEstimateFee(cryptoCoin: CryptoCoin, request: ApiRequest) {
        GetEstimateFee.getEstimateFee(
            CryptoNetManager.getURL(cryptoCoin.cryptoNet)!!,
            object : GetEstimateFee.estimateFeeListener {
                override fun estimateFee(value: Long) {
                    request.listener.success(value, request.id)
                }

                override fun fail() {
                    request.listener.fail(request.id)
                }
            })
    }

    interface HasTransactionListener {
        fun hasTransaction(value: Boolean)
    }
}
