package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.repositories.EquivalentValuesRepository
import cy.agorise.bitsybitshareswallet.repositories.NodeRepository
import cy.agorise.bitsybitshareswallet.repositories.TransferRepository
import cy.agorise.graphenej.network.FullNode
import io.reactivex.schedulers.Schedulers

class ConnectedActivityViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val TAG = "ConnectedActivityVM"
    }
    private var mTransfersRepository = TransferRepository(application)
    private var mEquivalentValuesRep = EquivalentValuesRepository(application)
    private val mNodeRepository: NodeRepository

    init {
        val nodeDao = BitsyDatabase.getDatabase(application)!!.nodeDao()
        mNodeRepository = NodeRepository(nodeDao)
    }

    fun observeMissingEquivalentValuesIn(symbol: String) {
        mTransfersRepository.getSupportedCurrency(symbol)
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe({
                currency -> mTransfersRepository.observeMissingEquivalentValuesIn(currency)
            },{
                Log.e(TAG,"Error while trying to subscribe to missing equivalent values observer. Msg: ${it.message}")
                for(element in it.stackTrace){
                    Log.e(TAG,"${element.className}#${element.methodName}:${element.lineNumber}")
                }
            })
    }

    fun updateNodeLatencies(nodes: List<FullNode>) {
        mNodeRepository.updateNodeLatencies(nodes)
    }

    override fun onCleared() {
        super.onCleared()
        mTransfersRepository.onCleared()
    }

    fun purgeEquivalentValues(): Int? {
        return mEquivalentValuesRep.purge()
    }
}