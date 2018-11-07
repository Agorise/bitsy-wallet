package cy.agorise.bitsybitshareswallet.apigenerator.insightapi

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.HashMap
import java.util.concurrent.TimeUnit

internal class InsightApiServiceGenerator
/**
 * Constructor, using the url of a insigth api coin
 * @param apiBaseUrl The complete url to the server of the insight api
 */
    (apiBaseUrl: String) {

    init {
        sApiBaseUrl = apiBaseUrl
        sLogging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        sClientBuilder = OkHttpClient.Builder().addInterceptor(sLogging)
        sBuilder = Retrofit.Builder().baseUrl(sApiBaseUrl).addConverterFactory(GsonConverterFactory.create())
        sServices = HashMap()
    }

    /**
     *
     * @param serviceClass
     * @param <T>
     * @return
    </T> */
    fun <T> getService(serviceClass: Class<T>): T {

        var service: T? = serviceClass.cast(sServices[serviceClass])
        if (service == null) {
            service = createService(serviceClass)
            setService(serviceClass, service)
        }
        return service!!
    }

    companion object {
        /**
         * Tag used for logging
         */
        var TAG = "InsightApiServiceGenerator"
        /**
         * The complete uri to connect to the insight api, this change from coin to coin
         */
        private lateinit var sApiBaseUrl: String
        /**
         * Loggin interceptor
         */
        private lateinit var sLogging: HttpLoggingInterceptor
        /**
         * Http builder
         */
        private lateinit var sClientBuilder: OkHttpClient.Builder
        /**
         * Builder for the retrofit class
         */
        private lateinit var sBuilder: Retrofit.Builder
        /**
         *
         */
        private lateinit var sServices: HashMap<Class<*>, Any>

        /**
         *
         * @param klass
         * @param thing
         * @param <T>
        </T> */
        private fun <T> setService(klass: Class<T>, thing: T?) {
            sServices.put(klass, thing!!)
        }

        /**
         *
         * @param serviceClass
         * @param <S>
         * @return
        </S> */
        private fun <S> createService(serviceClass: Class<S>): S {

            sClientBuilder.interceptors().add(Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder().method(original.method(), original.body())

                val request = requestBuilder.build()
                chain.proceed(request)
            })
            sClientBuilder.readTimeout(5, TimeUnit.MINUTES)
            sClientBuilder.connectTimeout(5, TimeUnit.MINUTES)
            val client = sClientBuilder.build()
            val retrofit = sBuilder.client(client).build()
            return retrofit.create(serviceClass)

        }

        /**
         *
         * @return
         */
        fun Create(): InsightApiService {
            val httpClient = OkHttpClient.Builder()
            httpClient.interceptors().add(Interceptor { chain ->
                val original = chain.request()

                // Customize the request
                val request = original.newBuilder().method(original.method(), original.body()).build()

                chain.proceed(request)
            })

            val client = httpClient.build()
            val retrofit = Retrofit.Builder().baseUrl(sApiBaseUrl).client(client).build()
            return retrofit.create(InsightApiService::class.java!!)
        }
    }
}
