package cy.agorise.bitsybitshareswallet.apigenerator

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.HashMap
import java.util.concurrent.TimeUnit

abstract class BitsharesFaucetApiGenerator {

    /**
     * Class used for the json serializer. this represents a peitition
     */
    private class CreateAccountPetition {
        // The account to be created
        internal var account: Account? = null
    }

    /**
     * Class used for the json serializer. This represents the account on the petition
     */
    class Account {
        /**
         * The name of the account
         */
        internal var name: String? = null
        /**
         * The owner key address
         */
        internal var owner_key: String? = null
        /**
         * The active key address
         */
        internal var active_key: String? = null
        /**
         * The memo key address
         */
        internal var memo_key: String? = null
    }

    class ServiceGenerator(apiBaseUrl: String) {

        init {
            API_BASE_URL = apiBaseUrl
            logging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            clientBuilder = OkHttpClient.Builder().addInterceptor(
                logging
            )
            builder = Retrofit.Builder().baseUrl(
                API_BASE_URL
            ).addConverterFactory(GsonConverterFactory.create())
            Services = HashMap()
        }

        fun <T> getService(serviceClass: Class<T>): T {

            var service: T? = serviceClass.cast(Services[serviceClass])
            if (service == null) {
                service =
                        createService(
                            serviceClass
                        )
                setService(
                    serviceClass,
                    service
                )
            }
            return service!!
        }

        companion object {
            var TAG = "ServiceGenerator"
            lateinit var API_BASE_URL: String
            private lateinit var logging: HttpLoggingInterceptor
            private lateinit var clientBuilder: OkHttpClient.Builder
            private lateinit var builder: Retrofit.Builder
            private val TIMEOUT: Long = 30

            private lateinit var Services: HashMap<Class<*>, Any>

            fun <T> setService(klass: Class<T>, thing: T?) {
                Services.put(klass, thing!!)
            }

            fun <S> createService(serviceClass: Class<S>): S {

                clientBuilder.interceptors().add(Interceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder().method(original.method(), original.body())

                    val request = requestBuilder.build()
                    chain.proceed(request)
                })
                clientBuilder.readTimeout(
                    TIMEOUT, TimeUnit.SECONDS)
                    .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                val client = clientBuilder.build()
                val retrofit = builder.client(client).build()
                return retrofit.create(serviceClass)

            }

            fun Create(): IWebService {
                val httpClient = OkHttpClient.Builder()
                httpClient.interceptors().add(Interceptor { chain ->
                    val original = chain.request()

                    // Customize the request
                    val request = original.newBuilder().method(original.method(), original.body()).build()

                    chain.proceed(request)
                })

                val client = httpClient.build()
                val retrofit = Retrofit.Builder().baseUrl(API_BASE_URL).client(client).build()

                return retrofit.create(IWebService::class.java)

            }
        }
    }

    interface IWebService {
        @Headers("Content-Type: application/json")
        @POST("/api/v1/accounts")
        fun getReg(@Body params: Map<String, HashMap<*, *>>): Call<RegisterAccountResponse>

    }

    inner class RegisterAccountResponse {
        var account: Account? = null
        var error: Error? = null

        inner class Error {
            var base: Array<String>? = null
        }
    }

    companion object {

        /**
         * Class to register a new Bitshares Account
         *
         * @param accountName The name of the Account to be register
         * @param ownerKey The owner key public address
         * @param activeKey The active key public address
         * @param memoKey the memo key public address
         * @param url The url of the faucet
         * @return The bitshares id of the registered account, or null
         */
        fun registerBitsharesAccount(
            accountName: String, ownerKey: String,
            activeKey: String, memoKey: String, url: String,
            request: ApiRequest
        ) {
            val petition =
                CreateAccountPetition()
            val account = Account()
            account.name = accountName
            account.owner_key = ownerKey
            account.active_key = activeKey
            account.memo_key = memoKey
            petition.account = account
            val gson = Gson()
            val jsonPetition = gson.toJson(petition)
            println("create account petition :$jsonPetition")

            //TODO faucet function

            val hm = HashMap<String, Any>()
            hm.put("name", account.name!!)
            hm.put("owner_key", account.owner_key!!)
            hm.put("active_key", account.active_key!!)
            hm.put("memo_key", account.memo_key!!)
            hm.put("refcode","agorise")
            hm.put("referrer","agorise")

            val hashMap = HashMap<String, HashMap<*, *>>()
            hashMap["account"] = hm

            try {
                val sg = ServiceGenerator(url)
                val service = sg.getService(IWebService::class.java)
                val postingService = service.getReg(hashMap)
                postingService.enqueue(object : Callback<RegisterAccountResponse> {

                    override fun onResponse(
                        call: Call<RegisterAccountResponse>,
                        response: Response<RegisterAccountResponse>
                    ) {
                        if (response.isSuccessful) {
                            println("faucet answer $response")
                            val resp = response.body()
                            if (resp.account != null) {
                                try {
                                    if (resp.account!!.name == account.name) {
                                        request.listener.success(true, request.id)
                                    } else {
                                        request.listener.fail(request.id)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    request.listener.fail(request.id)
                                }

                            } else {
                                println("ERROR response doesn't have account " + response.message())
                                request.listener.fail(request.id)

                            }
                        } else {
                            println("ERROR fetching info")
                            request.listener.fail(request.id)
                        }
                    }

                    override fun onFailure(call: Call<RegisterAccountResponse>, t: Throwable) {
                        t.printStackTrace()
                        request.listener.fail(request.id)
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                request.listener.fail(request.id)

            }

        }
    }
}
