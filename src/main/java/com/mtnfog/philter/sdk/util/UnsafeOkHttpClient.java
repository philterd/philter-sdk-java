/*******************************************************************************
 * Copyright 2020 Mountain Fog, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.mtnfog.philter.sdk.util;

import com.mtnfog.philter.sdk.PhilterClient;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class UnsafeOkHttpClient {

	public static OkHttpClient getUnsafeOkHttpClient() throws NoSuchAlgorithmException, KeyManagementException {

		final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

		} };

		final SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
		builder.connectTimeout(PhilterClient.TIMEOUT_SEC, TimeUnit.SECONDS);
		builder.writeTimeout(PhilterClient.TIMEOUT_SEC, TimeUnit.SECONDS);
		builder.readTimeout(PhilterClient.TIMEOUT_SEC, TimeUnit.SECONDS);
		builder.connectionPool(new ConnectionPool(PhilterClient.MAX_IDLE_CONNECTIONS, PhilterClient.KEEP_ALIVE_DURATION_MS, TimeUnit.MILLISECONDS));
		builder.hostnameVerifier((hostname, session) -> true);

		return builder.build();

	}

}
