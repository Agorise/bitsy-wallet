package cy.agorise.bitsybitshareswallet.repository

import android.app.Activity
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.models.AccountSeed
import cy.agorise.bitsybitshareswallet.models.CryptoNetAccount
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
        db!!.cryptoNetAccountDao().nukeTable()
        db!!.accountSeedDao().nukeTable()
        db!!.transactionDao().nukeTable()
    }

    fun addAccount(id:Long, name:String, masterSeeed:String):Long{

        var accountSeed: AccountSeed = AccountSeed()
        accountSeed.id = id
        accountSeed.name = name
        accountSeed.masterSeed = AESUtils.encrypt(masterSeeed)
        return db!!.accountSeedDao().insertAccountSeed(accountSeed)
    }


    fun addAccount(accountSeed: AccountSeed) : Long{
        return db!!.accountSeedDao().insertAccountSeed(accountSeed)
    }

    fun getTotalCryptoNetAccounts() : Int {
        var accounts:List<CryptoNetAccount> = db!!.cryptoNetAccountDao().allCryptoNetAccount
        return accounts.size
    }

    fun getCryptoNetLocalAcount() : CryptoNetAccount? {
        var accounts:List<CryptoNetAccount> = db!!.cryptoNetAccountDao().allCryptoNetAccount
        var contains:Boolean = accounts.size > 0
        if(contains){
           return accounts.get(0)
        }
        else{
            return null
        }
    }

    fun addCryptoNetAcount(cryptoNetAccount: CryptoNetAccount): Long {
        return db!!.cryptoNetAccountDao().insertCryptoNetAccount(cryptoNetAccount)[0]
    }
}