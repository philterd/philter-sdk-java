package com.mtnfog.philter;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mtnfog.philter.model.Status;
import com.mtnfog.philter.service.PhilterService;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PhilterClient {
	
	private static final Logger LOGGER = LogManager.getLogger(PhilterClient.class);
	
	private PhilterService service;

	public PhilterClient(String endpoint, boolean verifySslCertificate) {
		
		OkHttpClient okHttpClient = new OkHttpClient();
		
		if(!verifySslCertificate) {
			
			try {
			
				LOGGER.warn("Allowing all SSL certificates is not recommended.");
				okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();
				
			} catch (NoSuchAlgorithmException | KeyManagementException ex) {
				
				LOGGER.error("Cannot create unsafe HTTP client.", ex);
				
			}
			
		}
		
		Retrofit.Builder builder = new Retrofit.Builder()  
		        .baseUrl(endpoint)
		        .client(okHttpClient)
		        .addConverterFactory(GsonConverterFactory.create());

		Retrofit retrofit = builder.build();

		service = retrofit.create(PhilterService.class);

	}
	
	public String filter(String text) throws IOException {
	
		return service.filter(text).execute().body();
		
	}
	
	public Double detect(String text) throws IOException {
		
		return service.detect(text).execute().body();
		
	}
	
	public Status status() throws IOException {
		
		return service.status().execute().body();
		
	}
	
}
