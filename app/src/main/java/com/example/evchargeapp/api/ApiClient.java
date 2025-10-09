package com.example.evchargeapp.api;

import android.content.Context;
import com.example.evchargeapp.utils.SessionManager;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit;

    public static Retrofit get(Context ctx, String baseUrl) {
        if (retrofit == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor auth = chain -> {
                Request original = chain.request();
                String token = SessionManager.getToken(ctx);
                Request.Builder b = original.newBuilder();
                if (token != null && !token.isEmpty()) {
                    b.header("Authorization", "Bearer " + token);
                }
                return chain.proceed(b.build());
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(auth)
                    .addInterceptor(log)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}
