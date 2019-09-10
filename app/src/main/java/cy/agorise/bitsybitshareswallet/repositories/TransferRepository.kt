package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.daos.EquivalentValueDao
import cy.agorise.bitsybitshareswallet.database.daos.TransferDao
import cy.agorise.bitsybitshareswallet.database.entities.EquivalentValue
import cy.agorise.bitsybitshareswallet.database.entities.Transfer
import cy.agorise.bitsybitshareswallet.network.CoingeckoService
import cy.agorise.bitsybitshareswallet.network.ServiceGenerator
import cy.agorise.bitsybitshareswallet.utils.Constants
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.*

class TransferRepository internal constructor(context: Context) {
    companion object {
        private const val TAG = "TransferRepository"
    }

    private val mTransferDao: TransferDao
    private val mEquivalentValuesDao: EquivalentValueDao
    private val compositeDisposable = CompositeDisposable()

    init {
        val db = BitsyDatabase.getDatabase(context)
        mTransferDao = db!!.transferDao()
        mEquivalentValuesDao = db.equivalentValueDao()
    }

    fun insertAll(transfers: List<Transfer>) {
        insertAllAsyncTask(mTransferDao).execute(transfers)
    }

    fun update(transfer: Transfer){
        mTransferDao.insert(transfer)
    }

    fun setBlockTime(blockNumber: Long, timestamp: Long) {
        setBlockTimeAsyncTask(mTransferDao).execute(Pair(blockNumber, timestamp))
    }

    fun getAll(): LiveData<List<Transfer>> {
        return mTransferDao.getAll()
    }

    fun getCount(): Single<Int> {
        return mTransferDao.getCount()
    }

    fun getTransferBlockNumberWithMissingTime(): LiveData<Long> {
        return mTransferDao.getTransferBlockNumberWithMissingTime()
    }

    fun getTransfersWithMissingBtsValue(): LiveData<Transfer> {
        return mTransferDao.getTransfersWithMissingBtsValue()
    }

    fun deleteAll() {
        deleteAllAsyncTask(mTransferDao).execute()
    }

    /**
     * Creates a subscription to the transfers table which will listen & process equivalent values.
     *
     * This function will create a subscription that will listen for missing equivalent values. This will
     * automatically trigger a procedure designed to calculate the fiat equivalent value of any entry
     * of the 'transactions' table that stil doesn't have a corresponding entry in the 'equivalent_values'
     * table for that specific fiat currency.
     *
     * @param   symbol  The 3 letters symbol of the desired fiat currency.
     */
    fun observeMissingEquivalentValuesIn(symbol: String) {
        compositeDisposable.add(mTransferDao.getTransfersWithMissingValueIn(symbol)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { transfer -> obtainFiatValue(transfer, symbol) }
            .subscribe({
                Log.d(TAG,"Got equivalent value: $it")
                mEquivalentValuesDao.insert(it)
            },{
                Log.e(TAG,"Error while trying to create a new equivalent value. Msg: ${it.message}")
                for(element in it.stackTrace){
                    Log.e(TAG,"${element.className}#${element.methodName}:${element.lineNumber}")
                }
            }))
    }

    /**
     * Creates an equivalent value for a given transaction.
     *
     * Function used to perform a request to the Coingecko's price API trying to obtain the
     * equivalent value of a specific [Transfer].
     *
     * @param   transfer    The transfer whose equivalent value we want to obtain
     * @param   symbol      The symbol of the fiat that the equivalent value should be calculated in
     * @return              An instance of the [EquivalentValue] class, ready to be inserted into the database.
     */
    private fun obtainFiatValue(transfer: Transfer, symbol: String): EquivalentValue {
        val sg = ServiceGenerator(Constants.COINGECKO_URL)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ROOT)
        val date = Date(transfer.timestamp * 1000)
        val response = sg.getService(CoingeckoService::class.java)
                        ?.getHistoricalValueSync("bitshares", dateFormat.format(date), false)
                        ?.execute()
        var equivalentFiatValue = -1L
        if(response?.isSuccessful == true){
            val price: Double = response.body()?.market_data?.current_price?.get(symbol) ?: -1.0
            // The equivalent value is obtained by:
            // 1- Dividing the base value by 100000 (BTS native precision)
            // 2- Multiplying that BTS value by the unit price in the chosen fiat
            // 3- Multiplying the resulting value by 100 in order to express it in cents
            equivalentFiatValue = Math.round(transfer.btsValue?.div(1e5)?.times(price)?.times(100) ?: -1.0)
        }else{
            Log.w(TAG,"Request was not successful. code: ${response?.code()}")
        }
        return EquivalentValue(transfer.id, equivalentFiatValue, symbol)
    }

    private class insertAllAsyncTask internal constructor(private val mAsyncTaskDao: TransferDao) :
        AsyncTask<List<Transfer>, Void, Void>() {

        override fun doInBackground(vararg transfers: List<Transfer>): Void? {
            mAsyncTaskDao.insertAll(transfers[0])
            return null
        }
    }

    private class setBlockTimeAsyncTask internal constructor(private val mAsyncTaskDao: TransferDao) :
        AsyncTask<Pair<Long, Long>, Void, Void>() {

        override fun doInBackground(vararg pair: Pair<Long, Long>): Void? {
            mAsyncTaskDao.setBlockTime(pair[0].first, pair[0].second)
            return null
        }
    }

    private class deleteAllAsyncTask internal constructor(private val mAsyncTaskDao: TransferDao) :
        AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg params: Void?): Void? {
            mAsyncTaskDao.deleteAll()
            return null
        }
    }

    /**
     * Called whenever the disposables have to be cleared.
     *
     * Since this repository manages a subscription it is necessary to clear the disposable after we're done with it.
     * The parent ViewModel will let us know when that subscription is no longer necessary and the resources can
     * be cleared.
     */
    fun onCleared() {
        if(!compositeDisposable.isDisposed)
            compositeDisposable.clear()
    }
}