package com.mtnfog.philter.interceptors;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;

public class AuthorizationInterceptor implements Interceptor {

    final String token;

    public AuthorizationInterceptor(String token) {
        this.token = token;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        final String authentication = "Basic " + Base64.encodeBase64String(("token:" + token).getBytes());

        final Request original = chain.request();
        final Request.Builder requestBuilder = original.newBuilder().addHeader("Authorization", authentication);

        return chain.proceed(requestBuilder.build());

    }

}
