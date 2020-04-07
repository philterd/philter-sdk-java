package com.mtnfog.philter.interceptors;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class AuthorizationInterceptor implements Interceptor {

    final String token;

    public AuthorizationInterceptor(String token) {
        this.token = token;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        final Request original = chain.request();
        final Request.Builder requestBuilder = original.newBuilder().addHeader("Authorization", "token:" + token);

        return chain.proceed(requestBuilder.build());

    }
}
