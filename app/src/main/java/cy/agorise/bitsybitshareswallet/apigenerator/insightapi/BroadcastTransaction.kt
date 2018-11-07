package cy.agorise.bitsybitshareswallet.apigenerator.insightapi

import cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models.Txi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BroadcastTransaction
/**
 * Basic Consturctor
 * @param RawTx The RawTX in Hex String
 */
    (
    /**
     * The rawTX as Hex String
     */
    private val mRawTx: String,
    serverUrl: String,
    private val mPath: String,
    private val listener: BroadCastTransactionListener
) : Thread(),
    Callback<Txi> {
    /**
     * The serviceGenerator to call
     */
    private val mServiceGenerator: InsightApiServiceGenerator

    init {
        this.mServiceGenerator = InsightApiServiceGenerator(serverUrl)
    }

    /**
     * Handles the response of the call
     *
     */
    override fun onResponse(call: Call<Txi>, response: Response<Txi>) {
        if (response.isSuccessful()) {
            listener.onSuccess()
        } else {
            listener.onFailure(response.message())
        }
    }

    /**
     * Handles the failures of the call
     */
    override fun onFailure(call: Call<Txi>, t: Throwable) {
        listener.onConnecitonFailure()
    }

    /**
     * Starts the call of the service
     */
    override fun run() {
        val service = this.mServiceGenerator.getService(InsightApiService::class.java)
        val broadcastTransaction = service.broadcastTransaction(this.mPath, this.mRawTx)
        broadcastTransaction.enqueue(this)
    }

    interface BroadCastTransactionListener {
        fun onSuccess()
        fun onFailure(msg: String)
        fun onConnecitonFailure()
    }
}
