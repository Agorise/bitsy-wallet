package cy.agorise.bitsybitshareswallet.repository

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PagedList
import cy.agorise.bitsybitshareswallet.models.CryptoCoinTransaction
import cy.agorise.bitsybitshareswallet.models.CryptoCoinTransactionExtended
import cy.agorise.bitsybitshareswallet.utils.AESUtils
import java.util.*

class TransactionRepository(activity: Activity) : Repository(activity) {

    fun transactionsByDate(search: String): DataSource.Factory<Int, CryptoCoinTransactionExtended>{
        val transaction : DataSource.Factory<Int, CryptoCoinTransactionExtended> = db!!.transactionDao().transactionsByDate(search)
        return transaction
    }

    fun transactionsByAmount(search: String): DataSource.Factory<Int, CryptoCoinTransactionExtended>{
        val transaction : DataSource.Factory<Int, CryptoCoinTransactionExtended> = db!!.transactionDao().transactionsByAmount(search)
        return transaction
    }

    fun transactionsByIsInput(search: String): DataSource.Factory<Int, CryptoCoinTransactionExtended>{
        val transaction : DataSource.Factory<Int, CryptoCoinTransactionExtended> = db!!.transactionDao().transactionsByIsInput(search)
        return transaction
    }

    fun transactionsByFrom(search: String): DataSource.Factory<Int, CryptoCoinTransactionExtended>{
        val transaction : DataSource.Factory<Int, CryptoCoinTransactionExtended> = db!!.transactionDao().transactionsByFrom(search)
        return transaction
    }

    fun transactionsByTo(search: String): DataSource.Factory<Int, CryptoCoinTransactionExtended>{
        val transaction : DataSource.Factory<Int, CryptoCoinTransactionExtended> = db!!.transactionDao().transactionsByTo(search)
        return transaction
    }

    fun insertTransaction(id:Long,date:Date, input:Boolean, accountId:Long, amount: Long, idCurrency:Int, isConfirmed:Boolean, from:String, to:String){

            var cryptoCoinTransaction: CryptoCoinTransaction = CryptoCoinTransaction()
            cryptoCoinTransaction.id = id
            cryptoCoinTransaction.date = date
            cryptoCoinTransaction.input = input
            cryptoCoinTransaction.accountId = accountId
            cryptoCoinTransaction.amount = amount
            cryptoCoinTransaction.idCurrency = idCurrency
            cryptoCoinTransaction.isConfirmed = isConfirmed
            cryptoCoinTransaction.from = from
            cryptoCoinTransaction.to = to
            db!!.transactionDao().insertTransaction(cryptoCoinTransaction)
    }

    fun deleteAllTransactions() {
        db!!.transactionDao().deleteAllTransactions()
    }


}