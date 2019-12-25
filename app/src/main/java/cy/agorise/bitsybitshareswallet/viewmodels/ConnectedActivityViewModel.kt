package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.repositories.EquivalentValuesRepository
import cy.agorise.bitsybitshareswallet.repositories.NodeRepository
import cy.agorise.bitsybitshareswallet.repositories.TransferRepository
import cy.agorise.graphenej.network.FullNode

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
        mTransfersRepository.observeMissingEquivalentValuesIn(symbol)
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