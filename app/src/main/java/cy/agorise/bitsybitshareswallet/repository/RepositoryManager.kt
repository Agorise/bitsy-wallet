package cy.agorise.bitsybitshareswallet.repository

import android.app.Activity
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase

class RepositoryManager{

    companion object {

        fun getAccountsRepository(acivity:Activity) : AccountRepository {
            return AccountRepository(acivity)
        }

        fun getTransacionRepository(acivity:Activity) : TransactionRepository {
            return TransactionRepository(acivity)
        }

        fun getDB(activity: Activity):BitsyDatabase{
            return BitsyDatabase.getAppDatabase(activity!!)!!
        }
    }
}