package cy.agorise.bitsybitshareswallet.network

import cy.agorise.bitsybitshareswallet.models.coingecko.HistoricalPrice
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface CoingeckoService {

    @Headers("Content-Type: application/json")
    @GET("/api/v3/coins/bitshares/history")
    fun getHistoricalValueSync(@Query("id") id: String,
                               @Query("date") date: String,
                               @Query("localization") localization: Boolean): Call<HistoricalPrice>
}