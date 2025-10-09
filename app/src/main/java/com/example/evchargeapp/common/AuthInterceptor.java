package com.example.evchargeapp.common;

import android.content.Context;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final Context ctx;
    public AuthInterceptor(Context ctx){ this.ctx = ctx; }

    @Override public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = SessionManager.token(ctx);
        if (token == null) return chain.proceed(original);
        Request withAuth = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(withAuth);
    }
}
