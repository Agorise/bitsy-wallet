package cy.agorise.bitsybitshareswallet.network

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.os.Handler
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.stats.ExponentialMovingAverage

/**
 * Class used to manage the connection status of the NetworkService.
 *
 *
 * The basic idea here is to keep track of the sequence of activity life cycle callbacks so that we
 * can infer when the user has left the app and the node connection can be salfely shut down.
 */
class NetworkServiceManager(nodes: List<String>) :
    ActivityLifecycleCallbacks {
    /**
     * Handler instance used to schedule tasks back to the main thread
     */
    private val mHandler = Handler()
    private var mNetworkService: NetworkService? = null
    private val mNodeUrls: Array<String> = nodes.toTypedArray()
    /**
     * Runnable used to schedule a service disconnection once the app is not visible to the user for
     * more than DISCONNECT_DELAY milliseconds.
     */
    private val mDisconnectRunnable = Runnable {
        if (mNetworkService != null) {
            mNetworkService?.stop()
            mNetworkService = null
        }
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity?) {
        mHandler.removeCallbacks(mDisconnectRunnable)
        if (mNetworkService == null) {
            mNetworkService = NetworkService.getInstance()
            mNetworkService?.start(mNodeUrls, ExponentialMovingAverage.DEFAULT_ALPHA)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        mHandler.postDelayed(
            mDisconnectRunnable,
            DISCONNECT_DELAY.toLong()
        )
    }

    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        /**
         * Constant used to specify how long will the app wait for another activity to go through its starting life
         * cycle events before running the teardownConnectionTask task.
         *
         * This is used as a means to detect whether or not the user has left the app.
         */
        private const val DISCONNECT_DELAY = 1500
    }

}