package cy.agorise.bitsybitshareswallet.repository

import android.app.Activity
import cy.agorise.bitsybitshareswallet.models.AccountSeed

class AccountRepository(activity: Activity?) : Repository(activity) {

    fun getLocalAccount() : AccountSeed {

        val accounts = db!!.accountSeedDao().allNoLiveData
        val account: AccountSeed = accounts[0]
        return account
    }

    fun getTotalAccounts() : Int {

        val accounts = db!!.accountSeedDao().countAccountSeeds()
        return accounts
    }
}