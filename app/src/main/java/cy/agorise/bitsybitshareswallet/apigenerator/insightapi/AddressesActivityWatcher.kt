package cy.agorise.bitsybitshareswallet.apigenerator.insightapi

import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger

class AddressesActivityWatcher
/**
 * Basic constructor
 *
 */
    (private val mServerUrl: String, private val mPath: String, private val cryptoCoin: CryptoCoin) {
    /**
     * The list of address to monitor
     */
    private val mWatchAddress = ArrayList<String>()
    /**
     * the Socket.IO
     */
    private var mSocket: Socket? = null

    /**
     * Handles the address/transaction notification.
     * Then calls the GetTransactionData to get the info of the new transaction
     */
    private val onAddressTransaction = object : Emitter.Listener {
        override fun call(vararg os: Any) {
            try {
                println("Receive accountActivtyWatcher " + os[0].toString())
                val txid = (os[0] as JSONObject).getString(InsightApiConstants.sTxTag)
                GetTransactionData(txid, mServerUrl, mPath, cryptoCoin).start()
            } catch (ex: JSONException) {
                Logger.getLogger(AddressesActivityWatcher::class.java.name).log(Level.SEVERE, null, ex)
            }

        }
    }

    /**
     * Handles the connect of the Socket.IO
     */
    private val onConnect = object : Emitter.Listener {
        override fun call(vararg os: Any) {
            println("Connected to accountActivityWatcher")
            val array = JSONArray()
            for (addr in mWatchAddress) {
                array.put(addr)
            }
            mSocket!!.emit(InsightApiConstants.sSubscribeEmmit, InsightApiConstants.sChangeAddressRoom, array)
        }
    }

    /**
     * Handles the disconnect of the Socket.Io
     * Reconcects the mSocket
     */
    private val onDisconnect = object : Emitter.Listener {
        override fun call(vararg os: Any) {
            try {
                Thread.sleep(60000)
            } catch (ignore: InterruptedException) {
            }

            mSocket!!.connect()
        }
    }

    /**
     * Error handler, doesn't need reconnect, the mSocket.io do that by default
     */
    private val onError = object : Emitter.Listener {
        override fun call(vararg os: Any) {
            println("Error to accountActivityWatcher ")
            for (ob in os) {
                println("accountActivityWatcher " + ob.toString())
            }
            try {
                Thread.sleep(60000)
            } catch (ignore: InterruptedException) {
            }

            mSocket!!.connect()
        }
    }

    init {
        try {
            this.mSocket = IO.socket(mServerUrl)
            this.mSocket!!.on(Socket.EVENT_CONNECT, onConnect)
            this.mSocket!!.on(Socket.EVENT_DISCONNECT, onDisconnect)
            this.mSocket!!.on(Socket.EVENT_ERROR, onError)
            this.mSocket!!.on(Socket.EVENT_CONNECT_ERROR, onError)
            this.mSocket!!.on(Socket.EVENT_CONNECT_TIMEOUT, onError)
            this.mSocket!!.on(InsightApiConstants.sChangeAddressRoom, onAddressTransaction)
        } catch (e: URISyntaxException) {
            //TODO change exception handler
            e.printStackTrace()
        }

    }

    /**
     * Add an address to be monitored, it can be used after the connect
     * @param address The String address to monitor
     */
    fun addAddress(address: String) {
        mWatchAddress.add(address)
        if (this.mSocket!!.connected()) {
            mSocket!!.emit(
                InsightApiConstants.sSubscribeEmmit,
                InsightApiConstants.sChangeAddressRoom,
                arrayOf(address)
            )
        }
    }

    /**
     * Connects the Socket
     */
    fun connect() {
        try {
            if (this.mSocket == null || !this.mSocket!!.connected()) {
                this.mSocket!!.connect()
            }
        } catch (ignore: Exception) {
        }

    }

    /**
     * Disconnects the Socket
     */
    fun disconnect() {
        this.mSocket!!.disconnect()
    }
}
