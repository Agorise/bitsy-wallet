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

        private const val MERCHANTS_QUERY_LIMIT = 50
    }

    private val mMerchantDao: MerchantDao

    private val merchantsList = mutableListOf<Merchant>()
    private var merchantsSkip = 0

    private val bitsyWebservice: BitsyWebservice

    init {
        val db = BitsyDatabase.getDatabase(context)
        mMerchantDao = db!!.merchantDao()

        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BITSY_WEBSERVICE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        bitsyWebservice = retrofit.create<BitsyWebservice>(BitsyWebservice::class.java)
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
            val request = bitsyWebservice.getMerchants(merchantsSkip, MERCHANTS_QUERY_LIMIT)
            request.enqueue(this)
        }
    }

    override fun onResponse(call: Call<FeathersResponse<Merchant>>, response: Response<FeathersResponse<Merchant>>) {
        if (response.isSuccessful) {
            val res: FeathersResponse<Merchant>? = response.body()
            val merchants = res?.data ?: return

            if (merchants.isNotEmpty()) {
                merchantsList.addAll(merchants)
                merchantsSkip += MERCHANTS_QUERY_LIMIT

                val request = bitsyWebservice.getMerchants(merchantsSkip, MERCHANTS_QUERY_LIMIT)
                request.enqueue(this)
            } else {
                updateMerchants(merchantsList)

                val now = System.currentTimeMillis()
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putLong(Constants.KEY_MERCHANTS_LAST_UPDATE, now).apply()
            }
        }
    }

    override fun onFailure(call: Call<FeathersResponse<Merchant>>, t: Throwable) { /* Do nothing */ }

    private fun updateMerchants(merchants: List<Merchant>) {
        AsyncTask.execute {
            mMerchantDao.deleteAll()
            mMerchantDao.insertAll(merchants)
        }
    }

    fun findMerchantsByWord(query: String): Single<List<Merchant>> {
        return mMerchantDao.findMerchantsByWord("%$query%")
    }
}