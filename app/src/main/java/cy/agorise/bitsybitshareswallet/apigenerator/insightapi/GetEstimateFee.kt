package cy.agorise.bitsybitshareswallet.apigenerator.insightapi

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object GetEstimateFee {

    private val PATH = "api"

    /**
     * The funciton to get the rate for the transaction be included in the next 2 blocks
     * @param serverUrl The url of the insight server
     * @param listener the listener to this answer
     */
    fun getEstimateFee(serverUrl: String, listener: estimateFeeListener) {
        try {
            val serviceGenerator = InsightApiServiceGenerator(serverUrl)
            val service = serviceGenerator.getService(InsightApiService::class.java)
            val call = service.estimateFee(PATH)
            call.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    listener.estimateFee(response.body().get("2").asDouble.toLong())

                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    listener.fail()
                    listener.estimateFee(-1)
                }
            })
        } catch (e: Exception) {
            listener.fail()
        }

    }

    interface estimateFeeListener {
        fun estimateFee(value: Long)
        fun fail()
    }

}