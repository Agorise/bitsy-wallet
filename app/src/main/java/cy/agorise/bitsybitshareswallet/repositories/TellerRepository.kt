package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.daos.TellerDao
import cy.agorise.bitsybitshareswallet.database.entities.Teller
import cy.agorise.bitsybitshareswallet.network.FeathersResponse
import cy.agorise.bitsybitshareswallet.network.BitsyWebservice
import cy.agorise.bitsybitshareswallet.utils.Constants
import io.reactivex.Single
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TellerRepository internal constructor(val context: Context) : retrofit2.Callback<FeathersResponse<Teller>> {

    companion object {
        private const val TAG = "TellerRepository"

        private const val TELLERS_QUERY_LIMIT = 50
    }

    private val mTellerDao: TellerDao

    private val tellersList = mutableListOf<Teller>()
    private var tellersSkip = 0

    private val bitsyWebservice: BitsyWebservice

    init {
        val db = BitsyDatabase.getDatabase(context)
        mTellerDao = db!!.tellerDao()

        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BITSY_WEBSERVICE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        bitsyWebservice = retrofit.create<BitsyWebservice>(BitsyWebservice::class.java)
    }

    /** Returns a LiveData object directly from the database while the response from the WebService is obtained. */
    fun getAll(): LiveData<List<Teller>> {
        refreshTellers()
        return mTellerDao.getAll()
    }

    /** Refreshes the tellers information only if the MERCHANT_UPDATE_PERIOD has passed, otherwise it does nothing */
    private fun refreshTellers() {
        val lastTellerUpdate = PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(Constants.KEY_TELLERS_LAST_UPDATE, 0)

        val now = System.currentTimeMillis()

        if (lastTellerUpdate + Constants.MERCHANTS_UPDATE_PERIOD < now) {
            Log.d(TAG, "Updating tellers from webservice")
            val request = bitsyWebservice.getTellers(tellersSkip, TELLERS_QUERY_LIMIT)
            request.enqueue(this)
        }
    }

    override fun onResponse(call: Call<FeathersResponse<Teller>>, response: Response<FeathersResponse<Teller>>) {
        if (response.isSuccessful) {
            val res: FeathersResponse<Teller>? = response.body()
            val tellers = res?.data ?: return

            if (tellers.isNotEmpty()) {
                tellersList.addAll(tellers)
                tellersSkip += TELLERS_QUERY_LIMIT

                val request = bitsyWebservice.getTellers(tellersSkip, TELLERS_QUERY_LIMIT)
                request.enqueue(this)
            } else {
                updateTellers(tellersList)

                val now = System.currentTimeMillis()
                PreferenceManager.getDefaultSharedPreferences(context).edit {
                    putLong(Constants.KEY_TELLERS_LAST_UPDATE, now).apply()
                }
            }
        }
    }

    override fun onFailure(call: Call<FeathersResponse<Teller>>, t: Throwable) { /* Do nothing */ }

    private fun updateTellers(tellers: List<Teller>) {
        AsyncTask.execute {
            mTellerDao.deleteAll()
            mTellerDao.insertAll(tellers)
        }
    }

    fun findTellerByWord(word: String): Single<List<Teller>> {
        return mTellerDao.findTellersByWord("%$word%")
    }
}