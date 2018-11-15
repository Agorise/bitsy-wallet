package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.models.CryptoNetAccount

class CryptoNetAccountListViewModel(application: Application) : AndroidViewModel(application) {

    private val db: BitsyDatabase

    val cryptoNetAccountList: List<CryptoNetAccount>
        get() = this.db.cryptoNetAccountDao().allCryptoNetAccount

    val cryptoNetAccounts: LiveData<List<CryptoNetAccount>>
        get() = this.db.cryptoNetAccountDao().all

    init {
        this.db = BitsyDatabase.getAppDatabase(application.applicationContext)!!
    }
}
