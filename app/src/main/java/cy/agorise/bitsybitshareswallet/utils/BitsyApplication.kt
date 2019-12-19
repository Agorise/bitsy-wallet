package cy.agorise.bitsybitshareswallet.utils

import android.app.Application
import com.crashlytics.android.Crashlytics
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.network.NetworkServiceManager
import cy.agorise.bitsybitshareswallet.repositories.NodeRepository
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class BitsyApplication : Application() {

    /**
     * Coroutine Job used to create appScope and safely cancel all coroutines launched using it.
     */
    private val applicationJob = Job()

    /**
     * Application level scope used to launch coroutines not tied to ViewModels or Activities/Fragments.
     */
    lateinit var appScope: CoroutineScope

    private lateinit var mNodeRepository: NodeRepository

    override fun onCreate() {
        super.onCreate()

        // Add RxJava error handler to avoid crashes when an error occurs on a RxJava operation, but still log the
        // exception to Crashlytics so that we can fix the issues
        RxJavaPlugins.setErrorHandler { throwable -> Crashlytics.logException(throwable)}

        appScope = CoroutineScope(Dispatchers.Main + applicationJob)

        val nodeDao = BitsyDatabase.getDatabase(applicationContext)!!.nodeDao()

        mNodeRepository = NodeRepository(nodeDao)

        appScope.launch {
            startNetworkServiceConnection()
        }
    }

    private suspend fun startNetworkServiceConnection() {
        val (nodes, autoConnect) = mNodeRepository.getFormattedNodes()

        val networkManager = NetworkServiceManager(nodes.split(","))
        /*
        * Registering this class as a listener to all activity's callback cycle events, in order to
        * better estimate when the user has left the app and it is safe to disconnect the websocket connection
        */
        registerActivityLifecycleCallbacks(networkManager)

        // Fake call to onActivityResumed, because BiTSy is using a single activity and at the moment
        // onActivityResumed is called the first time the app starts, the NetworkServiceManager has not
        // been configured yet, thus causing the NetworkService to never connect.
        networkManager.onActivityResumed(null)
    }

    override fun onTerminate() {
        super.onTerminate()

        // Cancel the job which also cancels the scopes created using it, i.e. appScope
        applicationJob.cancel()
    }
}