package cy.agorise.bitsybitshareswallet.network

import android.util.Log
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketListener
import java.io.IOException
import java.util.HashMap

class WebSocketThread
/**
 * Basic constructor,
 *
 * TODO make it throw exception is problem creating the socket
 *
 * @param webSocketListener The socket listener for the wbesocket to response
 * @param url The url to connect
 */
    (webSocketListener: WebSocketListener, url: String) : Thread() {

    // The tag of this class for the log
    private val TAG = this.javaClass.name

    // The websocket to be used
    private var mWebSocket: WebSocket? = null
    // The socketListener for the websocket to reponse
    private var mWebSocketListener: WebSocketListener? = null
    // The url to connect
    private var mUrl: String? = null
    // If the parameters of this class can be change
    private var canChange = true

    /**
     * Gets the current url where the websocket will connect
     * @return the full url
     */
    /**
     * Sets the url of the websocket to connects, it would not change if this thread was already started
     * @param url The full url with the protocol and the ports
     */
    var url: String?
        get() = mUrl
        set(url) {
            if (canChange) {
                try {
                    val factory = WebSocketFactory().setConnectionTimeout(5000)
                    this.mUrl = url
                    mWebSocket = factory.createSocket(mUrl)
                    mWebSocket!!.addListener(this.mWebSocketListener)
                } catch (e: IOException) {
                    Log.e(TAG, "IOException. Msg: " + e.message)
                } catch (e: NullPointerException) {
                    Log.e(TAG, "NullPointerException at WebsocketWorkerThreas. Msg: " + e.message)
                }

            }
        }

    /**
     * Return the class listening for the websocket response
     */
    /**
     * Sets the class listenening the websocket response, it will not change if this thread was already started
     */
    var webSocketListener: WebSocketListener?
        get() = mWebSocketListener
        set(webSocketListener) {
            if (canChange) {
                try {
                    val factory = WebSocketFactory().setConnectionTimeout(5000)
                    this.mWebSocketListener = webSocketListener
                    mWebSocket = factory.createSocket(mUrl)
                    mWebSocket!!.addListener(this.mWebSocketListener)
                } catch (e: IOException) {
                    Log.e(TAG, "IOException. Msg: " + e.message)
                } catch (e: NullPointerException) {
                    Log.e(TAG, "NullPointerException at WebsocketWorkerThreas. Msg: " + e.message)
                }

            }
        }

    val isConnected: Boolean
        get() = mWebSocket!!.isOpen()


    init {
        try {
            val factory = WebSocketFactory().setConnectionTimeout(5000)
            this.mUrl = url
            this.mWebSocketListener = webSocketListener
            this.mWebSocket = factory.createSocket(this.mUrl)
            this.mWebSocket!!.addListener(this.mWebSocketListener)
        } catch (e: IOException) {
            Log.e(TAG, "IOException. Msg: " + e.message)
        } catch (e: NullPointerException) {
            Log.e(TAG, "NullPointerException at WebsocketWorkerThreas. Msg: " + e.message)
        }

    }

    override fun run() {
        canChange = false
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        try {
            WebSocketThread.currentThreads[this.id] = this
            mWebSocket!!.connect()
        } catch (e: WebSocketException) {
            Log.e(TAG, "WebSocketException. Msg: " + e.message)
        } catch (e: NullPointerException) {
            Log.e(TAG, "NullPointerException. Msg: " + e.message)
        }

        WebSocketThread.currentThreads.remove(this.id)
    }

    companion object {

        //This is to manage the differents threads of this app
        private val currentThreads = HashMap<Long, WebSocketThread>()
        // The connection tiemout
        private val connectionTimeout = 5000
    }
}