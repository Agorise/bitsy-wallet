package cy.agorise.bitsybitshareswallet.utils

import android.app.Application
import com.crashlytics.android.Crashlytics
import cy.agorise.graphenej.api.ApiAccess
import cy.agorise.graphenej.api.android.NetworkServiceManager
import io.reactivex.plugins.RxJavaPlugins


class BitsyApplication : Application() {

    companion object {
        private val BITSHARES_NODE_URLS = arrayOf(
            // PP private nodes
            "wss://nl.palmpay.io/ws",

            // Other public nodes
            "wss://kc-us-dex.xeldal.com/ws",            // missouri, usa
//            "wss://bitshares.nu/ws",                   // Stockholm, Sweden
            "wss://bitshares.openledger.info/ws"      // Openledger node
//            "wss://dallas.bitshares.apasia.tech/ws",	// Dallas, USA
//            "wss://atlanta.bitshares.apasia.tech/ws",	// Atlanta, USA
//            "wss://dex.rnglab.org",				// Amsterdam, Netherlands
//            "wss://citadel.li/node"
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Add RxJava error handler to avoid crashes when an error occurs on a RxJava operation, but still log the
        // exception to Crashlytics so that we can fix the issues
        RxJavaPlugins.setErrorHandler { throwable -> Crashlytics.logException(throwable)}

        // Specifying some important information regarding the connection, such as the
        // credentials and the requested API accesses
        val requestedApis = ApiAccess.API_DATABASE or ApiAccess.API_HISTORY or ApiAccess.API_NETWORK_BROADCAST
        val networkManager = NetworkServiceManager.Builder()
            .setUserName("")
            .setPassword("")
            .setRequestedApis(requestedApis)
            .setCustomNodeUrls(setupNodes())
            .setAutoConnect(true)
            .setNodeLatencyVerification(true)
            .build(this)

        /*
        * Registering this class as a listener to all activity's callback cycle events, in order to
        * better estimate when the user has left the app and it is safe to disconnect the websocket connection
        */
        registerActivityLifecycleCallbacks(networkManager)
    }

    private fun setupNodes(): String {
        val stringBuilder = StringBuilder()
        for (url in BITSHARES_NODE_URLS) {
            stringBuilder.append(url).append(",")
        }
        stringBuilder.replace(stringBuilder.length - 1, stringBuilder.length, "")
        return stringBuilder.toString()
    }
}