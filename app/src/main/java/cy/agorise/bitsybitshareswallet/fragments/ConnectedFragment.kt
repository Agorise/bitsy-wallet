package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.Fragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.api.android.RxBus
import cy.agorise.graphenej.models.JsonRpcResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

/**
 * Base fragment that defines the methods and variables commonly used in all fragments that directly connect and
 * talk to the BitShares nodes through graphenej's NetworkService
 */
abstract class ConnectedFragment : Fragment() {

    companion object {
        private const val TAG = "ConnectedFragment"
    }

    /** Network service connection */
    protected var mNetworkService: NetworkService? = NetworkService.getInstance()

    /** Keeps track of all RxJava disposables, to make sure they are all disposed when the fragment is destroyed */
    protected var mDisposables = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val locale = ConfigurationCompat.getLocales(resources.configuration)[0]
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey(Constants.CRASHLYTICS_KEY_LANGUAGE, locale.displayName)

        // Connect to the RxBus, which receives events from the NetworkService
        mDisposables.add(
            RxBus.getBusInstance()
                .asFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { handleIncomingMessage(it) } ,
                        { crashlytics.log("D/$TAG: ${it.message}") }
                )
        )
    }

    private fun handleIncomingMessage(message: Any?) {
        if (message is JsonRpcResponse<*>) {
            // Generic processing taken care by subclasses
            handleJsonRpcResponse(message)
        } else if (message is ConnectionStatusUpdate) {
            // Generic processing taken care by subclasses
            handleConnectionStatusUpdate(message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!mDisposables.isDisposed) mDisposables.dispose()
    }

    /**
     * Method to be implemented by all subclasses in order to be notified of JSON-RPC responses.
     * @param response
     */
    abstract fun handleJsonRpcResponse(response: JsonRpcResponse<*>)

    /**
     * Method to be implemented by all subclasses in order to be notified of connection status updates
     * @param connectionStatusUpdate
     */
    abstract fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate)
}