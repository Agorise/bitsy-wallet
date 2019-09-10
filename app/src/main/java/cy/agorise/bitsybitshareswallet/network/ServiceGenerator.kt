package cy.agorise.bitsybitshareswallet.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import cy.agorise.bitsybitshareswallet.models.coingecko.MarketData
import cy.agorise.bitsybitshareswallet.models.coingecko.MarketDataDeserializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.util.HashMap
import java.util.concurrent.TimeUnit

class ServiceGenerator private constructor(apiBaseUrl: String, gson: Gson) {

    private var httpClient: OkHttpClient.Builder
    private var builder: Retrofit.Builder

    private val services = HashMap<Class<*>, Any>()

    init {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        val logging =
            httpLoggingInterceptor.apply {
                httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            }
        httpClient = OkHttpClient.Builder().addInterceptor(logging)
        builder = Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    }

    constructor(apiBaseUrl: String) : this(apiBaseUrl, Gson())

    fun <T> getService(serviceClass: Class<T>): T? {

        var service = serviceClass.cast(services[serviceClass])
        if (service == null) {
            service = createService(serviceClass)
            setService(serviceClass, service)
        }
        return service
    }

    private fun <T> setService(klass: Class<T>, thing: T) {
        services[klass] = thing as Any
    }

    private fun <S> createService(serviceClass: Class<S>): S {
        httpClient.readTimeout(15, TimeUnit.SECONDS)
        httpClient.connectTimeout(15, TimeUnit.SECONDS)
        val client = httpClient.build()
        if (serviceClass == CoingeckoService::class.java) {
            // The MarketData class needs a custom de-serializer
            val gson = GsonBuilder().registerTypeAdapter(
                MarketData::class.java,
                MarketDataDeserializer()
            ).create()
            builder.addConverterFactory(GsonConverterFactory.create(gson))
        }
        val retrofit = builder.client(client).build()
        return retrofit.create(serviceClass)
    }

}
