package cy.agorise.bitsybitshareswallet.apigenerator.insightapi

import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import java.util.HashMap

internal object InsightApiConstants {
    /**
     * Protocol of the insight api calls
     */
    val sProtocol = "https"
    /**
     * Protocol of the insigiht api Socket.IO connection
     */
    val sProtocolSocketIO = "http"
    /**
     * Contains each url information for each coin
     */
    private val sServerAddressPort = HashMap<CryptoCoin, AddressPort>()
    /**
     * Insight api Socket.IO new transaction by address notification
     */
    val sChangeAddressRoom = "bitcoind/addresstxid"
    /**
     * Socket.io subscribe command
     */
    val sSubscribeEmmit = "subscribe"
    /**
     * Tag used in the response of the address transaction notification
     */
    val sTxTag = "txid"

    /**
     * Wait time to check for confirmations
     */
    var sWaitTime = (30 * 1000).toLong() //wait 1 minute

    //Filled the serverAddressPort maps with static data
    init {
        //serverAddressPort.put(Coin.BITCOIN,new AddressPort("fr.blockpay.ch",3002,"node/btc/testnet","insight-api"));
        sServerAddressPort[CryptoCoin.BITCOIN] = AddressPort("fr.blockpay.ch", 3003, "node/btc/testnet", "insight-api")
        //serverAddressPort.put(Coin.BITCOIN_TEST,new AddressPort("fr.blockpay.ch",3003,"node/btc/testnet","insight-api"));
        sServerAddressPort[CryptoCoin.LITECOIN] = AddressPort("fr.blockpay.ch", 3009, "node/ltc", "insight-lite-api")
        sServerAddressPort[CryptoCoin.DASH] = AddressPort("fr.blockpay.ch", 3005, "node/dash", "insight-api-dash")
        sServerAddressPort[CryptoCoin.DOGECOIN] = AddressPort("fr.blockpay.ch", 3006, "node/dogecoin", "insight-api")
    }

    /**
     * Get the insight api server address
     * @param coin The coin of the API to find
     * @return The String address of the server, can be a name or the IP
     */
    fun getAddress(coin: CryptoCoin): String {
        return sServerAddressPort[coin]!!.mServerAddress
    }

    /**
     * Get the port of the server Insight API
     * @param coin The coin of the API to find
     * @return The server number port
     */
    fun getPort(coin: CryptoCoin): Int {
        return sServerAddressPort[coin]!!.mPort
    }

    /**
     * Get the url path of the server Insight API
     * @param coin The coin of the API to find
     * @return The path of the Insight API
     */
    fun getPath(coin: CryptoCoin): String {
        return sServerAddressPort[coin]!!.mPath + "/" + sServerAddressPort[coin]!!.mInsightPath
    }

    /**
     * Contains all the url info neccessary to connects to the insight api
     */
    private class AddressPort
    /**
     * Constructor
     * @param serverAddress The server address of the Insight API
     * @param port the port number of the Insight API
     * @param path the path to the Insight API before the last /
     * @param insightPath the path after the last / of the Insight API
     */
    internal constructor(
        /**
         * The server address
         */
        internal val mServerAddress: String,
        /**
         * The port used in the Socket.io
         */
        internal val mPort: Int,
        /**
         * The path of the coin server
         */
        internal val mPath: String,
        /**
         * The path of the insight api
         */
        internal val mInsightPath: String
    )
}
