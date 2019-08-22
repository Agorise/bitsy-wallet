package cy.agorise.bitsybitshareswallet.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import cy.agorise.bitsybitshareswallet.models.coingecko.MarketData;
import cy.agorise.bitsybitshareswallet.models.coingecko.MarketDataDeserializer;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ServiceGenerator{
    private static OkHttpClient.Builder httpClient;
    private static Retrofit.Builder builder;

    private static HashMap<Class<?>, Object> Services = new HashMap<>();

    private ServiceGenerator(String apiBaseUrl, Gson gson) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClient = new OkHttpClient.Builder().addInterceptor(logging);
        builder = new Retrofit.Builder()
                .baseUrl(apiBaseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create());
    }

    public ServiceGenerator(String apiBaseUrl){
        this(apiBaseUrl, new Gson());
    }

    private static <T> void setService(Class<T> klass, T thing) {
        Services.put(klass, thing);
    }

    public <T> T getService(Class<T> serviceClass) {

        T service = serviceClass.cast(Services.get(serviceClass));
        if (service == null) {
            service = createService(serviceClass);
            setService(serviceClass, service);
        }
        return service;
    }

    private static <S> S createService(Class<S> serviceClass) {
        httpClient.readTimeout(15, TimeUnit.SECONDS);
        httpClient.connectTimeout(15, TimeUnit.SECONDS);
        OkHttpClient client = httpClient.build();
        if(serviceClass == CoingeckoService.class){
            // The MarketData class needs a custom de-serializer
            Gson gson = new GsonBuilder().registerTypeAdapter(MarketData.class, new MarketDataDeserializer()).create();
            builder.addConverterFactory(GsonConverterFactory.create(gson));
        }
        Retrofit retrofit = builder.client(client).build();
        return retrofit.create(serviceClass);
    }
}
