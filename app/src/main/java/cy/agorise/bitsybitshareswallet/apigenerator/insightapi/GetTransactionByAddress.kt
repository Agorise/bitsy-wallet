package cy.agorise.bitsybitshareswallet.apigenerator.insightapi

import android.util.Log
import cy.agorise.bitsybitshareswallet.apigenerator.InsightApiGenerator
import cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models.AddressTxi
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.manager.GeneralAccountManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList

class GetTransactionByAddress
/**
 * Basic consturcotr
 */
    (
    private val cryptoNet: CryptoCoin,
    private val mServerUrl: String,
    private val mPath: String,
    private val listener: InsightApiGenerator.HasTransactionListener?
) : Thread(),
    Callback<AddressTxi> {
    /**
     * The list of address to query
     */
    private val mAddresses = ArrayList<String>()
    /**
     * The serviceGenerator to call
     */
    private val mServiceGenerator: InsightApiServiceGenerator

    private var inProcess = false

    init {
        this.mServiceGenerator = InsightApiServiceGenerator(mServerUrl)
    }

    /**
     * add an address to be query
     * @param address the address to be query
     */
    fun addAddress(address: String) {
        this.mAddresses.add(address)
    }


    /**
     * Handle the response
     * @param call The call with the addresTxi object
     * @param response the response status object
     */
    override fun onResponse(call: Call<AddressTxi>, response: Response<AddressTxi>) {
        inProcess = false
        if (response.isSuccessful()) {
            val addressTxi = response.body()
            if (listener != null) {
                if (addressTxi.items!!.size > 0) {
                    listener!!.hasTransaction(true)
                } else {
                    listener!!.hasTransaction(false)
                }
            }

            for (txi in addressTxi.items!!) {
                GeneralAccountManager.getAccountManager(this.cryptoNet)!!.processTxi(txi)
            }

        } else {
            listener!!.hasTransaction(false)
        }
    }

    /**
     * Failure of the call
     * @param call The call object
     * @param t The reason for the failure
     */
    override fun onFailure(call: Call<AddressTxi>, t: Throwable) {
        inProcess = false
        Log.e("GetTransactionByAddress", "Error in json format")
    }

    /**
     * Function to start the insight api call
     */
    override fun run() {
        if (this.mAddresses.size > 0 && !inProcess) {
            inProcess = true
            val addressToQuery = StringBuilder()
            for (address in this.mAddresses) {
                addressToQuery.append(address).append(",")
            }
            addressToQuery.deleteCharAt(addressToQuery.length - 1)
            val service = this.mServiceGenerator.getService(InsightApiService::class.java)
            val addressTxiCall = service.getTransactionByAddress(this.mPath, addressToQuery.toString())
            addressTxiCall.enqueue(this)
        }
    }
}
