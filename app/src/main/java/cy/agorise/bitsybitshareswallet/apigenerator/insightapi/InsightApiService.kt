package cy.agorise.bitsybitshareswallet.apigenerator.insightapi

import com.google.gson.JsonObject
import cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models.AddressTxi
import cy.agorise.bitsybitshareswallet.apigenerator.insightapi.models.Txi
import retrofit2.Call
import retrofit2.http.*

internal interface InsightApiService {

    /**
     * The query for the info of a single transaction
     * @param path The path of the insight api without the server address
     * @param txid the transasction to be query
     */
    @GET("{path}/tx/{txid}")
    fun getTransaction(
        @Path(value = "path", encoded = true) path: String, @Path(
            value = "txid",
            encoded = true
        ) txid: String
    ): Call<Txi>

    /**
     * The query for the transasctions of multiples addresses
     * @param path     The path of the insight api without the server address
     * @param addrs the addresses to be query each separated with a ","
     */
    @GET("{path}/addrs/{addrs}/txs")
    fun getTransactionByAddress(
        @Path(value = "path", encoded = true) path: String, @Path(
            value = "addrs",
            encoded = true
        ) addrs: String
    ): Call<AddressTxi>

    /**
     * Broadcast Transaction
     * @param path The path of the insight api without the server address
     * @param rawtx the rawtx to send in Hex String
     */
    @FormUrlEncoded
    @POST("{path}/tx/send")
    fun broadcastTransaction(
        @Path(
            value = "path",
            encoded = true
        ) path: String, @Field("rawtx") rawtx: String
    ): Call<Txi>

    /**
     * Get the estimate rate fee for a coin in the Insight API
     * @param path The path of the insight api without the server address
     */
    @GET("{path}/utils/estimatefee?nbBlocks=2")
    fun estimateFee(@Path(value = "path", encoded = true) path: String): Call<JsonObject>

    @GET("{path}/block-index/0")
    fun genesisBlock(@Path(value = "path", encoded = true) path: String): Call<JsonObject>

}
