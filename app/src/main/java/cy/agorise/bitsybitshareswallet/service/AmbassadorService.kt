package cy.agorise.bitsybitshareswallet.service

import cy.agorise.bitsybitshareswallet.manager.FeathersResponse
import cy.agorise.bitsybitshareswallet.models.Ambassador
import cy.agorise.bitsybitshareswallet.models.AmbassadorLocation
import cy.agorise.bitsybitshareswallet.models.Merchant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface AmbassadorService {

    //https://ambpay.palmpay.io/api/v1/merchants?%24sort%5Baccount%5D=1&%24limit=50&%24skip=0
    @get:GET("/api/v1/merchants?%24sort%5Baccount%5D=1&%24limit=50&%24skip=0")
    val allMerchants: Call<FeathersResponse<Merchant>>

    @GET("/api/v1/ambassadors")
    fun getAmbassadors(@Query("cityId") cityId: String): Call<FeathersResponse<Ambassador>>

    @GET("/api/v1/cities")
    fun getAllCitiesSync(@Query("\$skip") skip: Long): Call<FeathersResponse<AmbassadorLocation>>
}
