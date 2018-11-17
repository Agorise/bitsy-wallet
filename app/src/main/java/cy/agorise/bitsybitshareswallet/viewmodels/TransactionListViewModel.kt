package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.models.CryptoCoinTransactionExtended
import cy.agorise.bitsybitshareswallet.repository.RepositoryManager

class TransactionListViewModel(application: Application) : AndroidViewModel(application) {

    var transactionList: LiveData<PagedList<CryptoCoinTransactionExtended>>? = null
        private set
    private val db: BitsyDatabase?
    private val application_:Application = application

    init {
        this.db = BitsyDatabase.getAppDatabase(application_.applicationContext)
        /*transactionList = this.db.transactionDao().transactionsByDate().create(0,
                new PagedList.Config.Builder()
                        .setEnablePlaceholders(true)
                        .setPageSize(10)
                        .setPrefetchDistance(10)
                        .build()
        );*/
    }

    fun initTransactionList(orderField: String, search: String) {
        var dataSource: DataSource.Factory<Int, CryptoCoinTransactionExtended>? = null

        when (orderField) {
            "date" -> dataSource = this.db!!.transactionDao().transactionsByDate(search)
            "amount" -> dataSource = this.db!!.transactionDao().transactionsByAmount(search)
            "is_input" -> dataSource = this.db!!.transactionDao().transactionsByIsInput(search)
            "from" -> dataSource = this.db!!.transactionDao().transactionsByFrom(search)
            "to" -> dataSource = this.db!!.transactionDao().transactionsByTo(search)
            else -> dataSource = this.db!!.transactionDao().transactionsByDate(search)
        }
        if (dataSource != null) {
            this.transactionList = LivePagedListBuilder(
                dataSource,
                PagedList.Config.Builder()
                    .setEnablePlaceholders(true)
                    .setPageSize(10)
                    .setPrefetchDistance(10)
                    .build()
            ).build()
        } else {
            this.transactionList = null
        }
    }
}