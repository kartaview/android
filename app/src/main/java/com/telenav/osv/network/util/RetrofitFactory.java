package com.telenav.osv.network.util;

import java.util.concurrent.TimeUnit;
import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.telenav.osv.BuildConfig;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.common.listener.ListenerDefault;
import com.telenav.osv.network.request.interceptor.JarvisRequestAuthorizationInterceptor;
import com.telenav.osv.network.request.interceptor.UploadInterceptor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Class which handles setup for the retrofit instance with all its methods.
 */
public class RetrofitFactory {

    private static final int TIMEOUT_UPLOAD_IN_MINUTES = 6;
    private static final long CONNECTION_TIMEOUT = 60_000L;
    private static final long READ_TIMEOUT = 60_000L;
    private static final long WRITE_TIMEOUT = 60_000L;

    /**
     * @param baseUrl the {@code String} url representing the base api call.
     * @param converters the {@code Convert} used for serialization and deserialization of objects.
     * @param adapter the {@code CallAdapter} used for supporting service method return types other than the default type, {@link retrofit2.Call}.
     * @return {@code Retrofit} builder with specific parameters.
     */
    public static Retrofit provideRetrofitBuilder(String baseUrl, @NonNull CallAdapter.Factory adapter, @Nullable OkHttpClient okHttpClient,
                                                  @NonNull Converter.Factory... converters) {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(adapter);
        for (Converter.Factory converter : converters) {
            builder.addConverterFactory(converter);
        }
        if (okHttpClient != null) {
            builder.client(okHttpClient);
        }
        return builder.build();
    }

    /**
     * @param baseUrl the {@code String} url representing the base api call.
     * @param simpleEventBus the event buss used in order to obtain internet availability changes.
     * @param applicationPreferences the application preferences required by the interceptor in order to be created.
     * @param context the context required by the interceptor in order to be created.
     * @return a default retrofit builder with {@code GsonConverterFactory} for serialization/deserialization and for {@code RxJava2CallAdapterFactory} for callbacks.
     * <p>The {@code RxJava2CallAdapterFactory} with {@link Schedulers#io()}.
     */
    public static Retrofit provideUploadRetrofitBuilder(@NonNull String baseUrl,
                                                        @NonNull SimpleEventBus simpleEventBus,
                                                        @NonNull ApplicationPreferences applicationPreferences,
                                                        @NonNull Context context,
                                                        @NonNull ListenerDefault noInternetListener) {
        Gson gson = new GsonBuilder().setLenient().create();
        return provideRetrofitBuilder(baseUrl,
                RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()),
                provideUploadOkHttpClient(simpleEventBus, applicationPreferences, context, noInternetListener),
                GsonConverterFactory.create(gson));
    }

    /**
     * @return a new {@code OkHttpClient} instance with {@link UploadInterceptor} attached.
     */
    private static OkHttpClient provideUploadOkHttpClient(SimpleEventBus simpleEventBus,
                                                          ApplicationPreferences applicationPreferences,
                                                          Context context,
                                                          ListenerDefault noInternetListener) {
        // Add the interceptor to OkHttpClient
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(TIMEOUT_UPLOAD_IN_MINUTES, TimeUnit.MINUTES);
        builder.readTimeout(TIMEOUT_UPLOAD_IN_MINUTES, TimeUnit.MINUTES);
        builder.writeTimeout(TIMEOUT_UPLOAD_IN_MINUTES, TimeUnit.MINUTES);
        builder.interceptors().add(new UploadInterceptor(context, applicationPreferences, simpleEventBus, noInternetListener));
        return builder.build();
    }

    /**
     * This method provides generic OkHttpClient builder
     */
    private static OkHttpClient.Builder provideGenericOkHttpClientBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.writeTimeout(WRITE_TIMEOUT, TimeUnit.MILLISECONDS);

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(httpLoggingInterceptor);
        }
        return builder;
    }

    /**
     * @param needAuthorization states whether authorization interceptor is required
     * @param applicationPreferences used to fetch user token
     * @return a new {@code OkHttpClient} instance for Jarvis SDK
     */
    private static OkHttpClient provideJarvisOkHttpClient(boolean needAuthorization, ApplicationPreferences applicationPreferences) {
        OkHttpClient.Builder builder = provideGenericOkHttpClientBuilder();
        if (needAuthorization) {
            builder.addInterceptor(new JarvisRequestAuthorizationInterceptor(applicationPreferences));
        }
        return builder.build();
    }

    /**
     * @param needAuthorization states whether authorization interceptor is required
     * @param applicationPreferences used to fetch user token
     * @return a default retrofit builder with {@code GsonConverterFactory} for serialization/deserialization and for {@code RxJava2CallAdapterFactory} for callbacks.
     * <p>The {@code RxJava2CallAdapterFactory} with {@link Schedulers#io()}.
     */
    public static Retrofit provideJarvisRetrofitBuilder(boolean needAuthorization, ApplicationPreferences applicationPreferences) {
        Gson gson = new GsonBuilder().setLenient().create();
        return provideRetrofitBuilder(BuildConfig.JARVIS_BASE_URL,
                RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()),
                provideJarvisOkHttpClient(needAuthorization, applicationPreferences),
                GsonConverterFactory.create(gson));
    }

    /**
     * @return a default retrofit builder with {@code GsonConverterFactory} for serialization/deserialization and for {@code RxJava2CallAdapterFactory} for callbacks.
     * <p>The {@code RxJava2CallAdapterFactory} with {@link Schedulers#io()}.
     */
    public static Retrofit provideGrabViewRetrofitBuilder() {
        Gson gson = new GsonBuilder().setLenient().create();
        return provideRetrofitBuilder(BuildConfig.GATEWAY_BASE_URL,
                RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()),
                provideGenericOkHttpClientBuilder().build(),
                GsonConverterFactory.create(gson));
    }
}
