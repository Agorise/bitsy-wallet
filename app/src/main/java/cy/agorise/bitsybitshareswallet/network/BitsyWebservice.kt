package cy.agorise.bitsybitshareswallet.network

import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import cy.agorise.bitsybitshareswallet.database.entities.Teller
import cy.agorise.bitsybitshareswallet.models.NodeWS
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BitsyWebservice {

    @GET("/api/v2/merchants")
    fun getMerchants(@Query(value = "\$skip") skip: Int,
                     @Query(value = "\$limit") limit: Int = 50):
            Call<FeathersResponse<Merchant>>

    @GET("api/v2/tellers")
    fun getTellers(@Query(value = "\$skip") skip: Int,
                   @Query(value = "\$limit") limit: Int = 50):
            Call<FeathersResponse<Teller>>

    @GET("/api/v2/nodes")
    suspend fun getNodes(): Response<List<NodeWS>>
}