package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.daos.EquivalentValueDao
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetailDao

class TransferDetailRepository internal constructor(context: Context) {

    private val mTransferDetailDao: TransferDetailDao
    private val mEquivalentValuesDao: EquivalentValueDao

    init {
        val db = BitsyDatabase.getDatabase(context)
        mTransferDetailDao = db!!.transferDetailDao()
        mEquivalentValuesDao = db.equivalentValueDao()
    }

    fun get(userId: String, transferId: String): LiveData<TransferDetail> {
        return mTransferDetailDao.get(userId, transferId)
    }

    fun getAll(userId: String, currency: String): LiveData<List<TransferDetail>> {
        return mTransferDetailDao.getAll(userId, currency)
    }

}