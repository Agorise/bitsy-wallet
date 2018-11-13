package cy.agorise.bitsybitshareswallet.repository

import android.app.Activity
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.models.AccountSeed
import cy.agorise.bitsybitshareswallet.utils.AESUtils

class AccountRepository(activity: Activity?) : Repository(activity) {

    fun getLocalAccount() : AccountSeed {

        val accounts = db!!.accountSeedDao().allNoLiveData
        val account: AccountSeed = accounts[0]
        account.masterSeed = AESUtils.decrypt(account.masterSeed!!)
        return account
    }

    fun getMasterSeed() : String{

        val accounts = db!!.accountSeedDao().allNoLiveData
        val account: AccountSeed = accounts[0]
        return AESUtils.decrypt(account.masterSeed!!)
    }

    fun getTotalAccounts() : Int {

        val accounts = db!!.accountSeedDao().countAccountSeeds()
        return accounts
    }

    fun removeAccount(){
        db!!.accountSeedDao().nukeTable()
    }

    fun addAccount(id:Long, name:String, masterSeeed:String){

        var accountSeed: AccountSeed = AccountSeed()
        accountSeed.id = id
        accountSeed.name = name
        accountSeed.masterSeed = AESUtils.encrypt(masterSeeed)
        db!!.accountSeedDao().insertAccountSeed(accountSeed)
    }


}