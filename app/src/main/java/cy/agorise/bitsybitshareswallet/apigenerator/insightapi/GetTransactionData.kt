package cy.agorise.bitsybitshareswallet.apigenerator.insightapi

import cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models.Txi
import cy.agorise.bitsybitshareswallet.enums.CryptoCoin
import cy.agorise.bitsybitshareswallet.manager.GeneralAccountManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GetTransactionData
/**
 * Consturctor to be used qhen the confirmations of the transaction are known
 * @param txid The txid of the transaciton to be query
 * @param mustWait If there is less confirmation that needed
 */
@JvmOverloads constructor(
    /**
     * The transaction txid to be query
     */
    private val mTxId: String,
    private val mServerUrl: String,
    private val mPath: String,
    private val cryptoCoin: CryptoCoin,
    mustWait: Boolean = false
) : Thread(),
    Callback<Txi> {
    /**
     * The serviceGenerator to call
     */
    private val mServiceGenerator: InsightApiServiceGenerator
    /**
     * If has to wait for another confirmation
     */
    private var mMustWait = false

    init {
        this.mServiceGenerator = InsightApiServiceGenerator(mServerUrl)
        this.mMustWait = mustWait
    }

    /**
     * Function to start the insight api call
     */
    override fun run() {
        if (this.mMustWait) {
            //We are waiting for confirmation
            try {
                Thread.sleep(InsightApiConstants.sWaitTime)
            } catch (ignored: InterruptedException) {
                //TODO this exception never rises
            }

        }

        val service = this.mServiceGenerator.getService(InsightApiService::class.java)
        val txiCall = service.getTransaction(this.mPath, this.mTxId)
        txiCall.enqueue(this)
    }

    override fun onResponse(call: Call<Txi>, response: Response<Txi>) {
        if (response.isSuccessful()) {

            val txi = response.body()
            GeneralAccountManager.getAccountManager(this.cryptoCoin)!!.processTxi(txi)
            if (txi.confirmations < this.cryptoCoin.cryptoNet.confirmationsNeeded) {
                //If transaction weren't confirmed, add the transaction to watch for change on the confirmations
                GetTransactionData(this.mTxId, this.mServerUrl, this.mPath, this.cryptoCoin, true).start()
            }
        }
    }

    /**
     * TODO handle the failure response
     * @param call the Call object
     * @param t the reason of the failure
     */
    override fun onFailure(call: Call<Txi>, t: Throwable) {

    }
}
/**
 * Constructor used to query for a transaction with unknown confirmations
 * @param txid The txid of the transaciton to be query
 */
