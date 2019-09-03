package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.entities.UserAccount
import cy.agorise.bitsybitshareswallet.repositories.AuthorityRepository
import cy.agorise.bitsybitshareswallet.repositories.UserAccountRepository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragmentViewModel(application: Application) : AndroidViewModel(application) {
    private var mUserAccountRepository = UserAccountRepository(application)
    private var mAuthorityRepository = AuthorityRepository(application)

    internal fun getUserAccount(id: String): LiveData<UserAccount> {
        return mUserAccountRepository.getUserAccount(id)
    }

    internal fun getWIF(userId: String, authorityType: Int): LiveData<String> {
        return mAuthorityRepository.getWIF(userId, authorityType)
    }

    internal fun clearDatabase(context: Context) {
        val db = BitsyDatabase.getDatabase(context)
        viewModelScope.launch {
            withContext(IO) {
                db?.clearAllTables()
            }
        }
    }
}