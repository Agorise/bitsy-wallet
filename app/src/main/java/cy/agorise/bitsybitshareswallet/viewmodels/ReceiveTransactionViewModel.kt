package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.entities.Asset
import cy.agorise.bitsybitshareswallet.database.entities.UserAccount
import cy.agorise.bitsybitshareswallet.repositories.AssetRepository
import cy.agorise.bitsybitshareswallet.repositories.UserAccountRepository

class ReceiveTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private var mUserAccountRepository = UserAccountRepository(application)
    private var mAssetRepository = AssetRepository(application)

    internal fun getUserAccount(id: String): LiveData<UserAccount> {
        return mUserAccountRepository.getUserAccount(id)
    }

    internal fun getAllNonZero(): LiveData<List<Asset>> {
        return mAssetRepository.getAllNonZero()
    }
}