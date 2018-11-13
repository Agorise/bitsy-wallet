package cy.agorise.bitsybitshareswallet.repository

import android.app.Activity

class RepositoryManager{

    companion object {

        fun getAccountsRepository(acivity:Activity) : AccountRepository {
            return AccountRepository(acivity)
        }
    }
}