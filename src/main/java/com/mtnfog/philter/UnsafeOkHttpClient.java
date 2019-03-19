package com.mtnfog.philter;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class UnsafeOkHttpClient {

	public static OkHttpClient getUnsafeOkHttpClient() throws NoSuchAlgorithmException, KeyManagementException {

		final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws CertificateException {
			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws CertificateException {
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
		builder.hostnameVerifier(new HostnameVerifier() {
			
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
			
		});

		return builder.build();

	}
	
}