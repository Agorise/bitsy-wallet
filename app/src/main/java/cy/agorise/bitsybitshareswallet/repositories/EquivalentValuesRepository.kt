package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.daos.EquivalentValueDao
import cy.agorise.bitsybitshareswallet.database.daos.TransferDao

class EquivalentValuesRepository(context: Context) {

    private val mEquivalentValuesDao: EquivalentValueDao?
    private val mTransfersDao: TransferDao?

    init {
        val db = BitsyDatabase.getDatabase(context)
        mEquivalentValuesDao = db?.equivalentValueDao()
        mTransfersDao = db?.transferDao()
    }

    fun purge(): Int? {
        return mEquivalentValuesDao?.purge()
    }
}