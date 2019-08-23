package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.Log
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.daos.MerchantDao
import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import cy.agorise.bitsybitshareswallet.network.FeathersResponse
import cy.agorise.bitsybitshareswallet.network.BitsyWebservice
import cy.agorise.bitsybitshareswallet.utils.Constants
import io.reactivex.Single
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MerchantRepository internal constructor(val context: Context) : retrofit2.Callback<FeathersResponse<Merchant>> {

    companion object {
        private const val TAG = "MerchantRepository"
    }

    private val mMerchantDao: MerchantDao

    init {
        val db = BitsyDatabase.getDatabase(context)
        mMerchantDao = db!!.merchantDao()
    }

    /** Returns a LiveData object directly from the database while the response from the WebService is obtained. */
    fun getAll(): LiveData<List<Merchant>> {
        refreshMerchants()
        return mMerchantDao.getAll()
    }

    /** Refreshes the merchants information only if the MERCHANT_UPDATE_PERIOD has passed, otherwise it does nothing */
    private fun refreshMerchants() {
        val lastMerchantUpdate = PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(Constants.KEY_MERCHANTS_LAST_UPDATE, 0)

        val now = System.currentTimeMillis()

        if (lastMerchantUpdate + Constants.MERCHANTS_UPDATE_PERIOD < now) {
            Log.d(TAG, "Updating merchants from webservice")
            // TODO make sure it works when there are more merchants than those sent back in the first response
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.BITSY_WEBSERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val bitsyWebservice = retrofit.create<BitsyWebservice>(BitsyWebservice::class.java)
            val call = bitsyWebservice.getMerchants(0)
            call.enqueue(this)
        }
    }

    override fun onResponse(call: Call<FeathersResponse<Merchant>>, response: Response<FeathersResponse<Merchant>>) {
        if (response.isSuccessful) {
            val res: FeathersResponse<Merchant>? = response.body()
            val merchants = res?.data ?: return
            insertAllAsyncTask(mMerchantDao).execute(merchants)

            val now = System.currentTimeMillis()
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong(Constants.KEY_MERCHANTS_LAST_UPDATE, now).apply()
        }
    }

    override fun onFailure(call: Call<FeathersResponse<Merchant>>, t: Throwable) { /* Do nothing */ }

    private class insertAllAsyncTask internal constructor(private val mAsyncTaskDao: MerchantDao) :
        AsyncTask<List<Merchant>, Void, Void>() {

        override fun doInBackground(vararg merchants: List<Merchant>): Void? {
            mAsyncTaskDao.deleteAll()
            mAsyncTaskDao.insertAll(merchants[0])
            return null
        }
    }

    fun findMerchantsByWord(query: String): Single<List<Merchant>> {
        return mMerchantDao.findMerchantsByWord("%$query%")
    }
}