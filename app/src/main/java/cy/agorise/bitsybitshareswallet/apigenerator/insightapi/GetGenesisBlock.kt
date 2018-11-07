package cy.agorise.bitsybitshareswallet.apigenerator.insightapi

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GetGenesisBlock(serverUrl: String, listener: genesisBlockListener) {

    init {
        try {
            val serviceGenerator = InsightApiServiceGenerator(serverUrl)
            val service = serviceGenerator.getService(InsightApiService::class.java)
            val call = service.genesisBlock(PATH)
            call.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    try {
                        listener.genesisBlock(response.body().get("blockHash").asString)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        listener.fail()
                    }

                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    listener.fail()
                }
            })
        } catch (e: Exception) {
            listener.fail()
        }

    }

    interface genesisBlockListener {
        fun genesisBlock(value: String)
        fun fail()
    }

    companion object {

        private val PATH = "api"
    }
}
