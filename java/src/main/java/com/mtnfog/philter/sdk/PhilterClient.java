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
package com.mtnfog.philter.sdk;

import com.mtnfog.philter.sdk.model.FilterResponse;
import com.mtnfog.philter.sdk.model.FilteredSpan;
import com.mtnfog.philter.sdk.model.Status;
import com.mtnfog.philter.sdk.service.PhilterService;
import com.mtnfog.philter.sdk.util.UnsafeOkHttpClient;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PhilterClient {

	private static final Logger LOGGER = LogManager.getLogger(PhilterClient.class);

	public static final int TIMEOUT_SEC = 30;

	private PhilterService service;

	public PhilterClient(String endpoint, boolean verifySslCertificate) {

		OkHttpClient okHttpClient = new OkHttpClient.Builder()
			.connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
			.writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
			.readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
			.build();

		if(!verifySslCertificate) {

			try {

				LOGGER.warn("Allowing all SSL certificates is not recommended.");
				okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();

			} catch (NoSuchAlgorithmException | KeyManagementException ex) {

				LOGGER.error("Cannot create unsafe HTTP client.", ex);
				throw new RuntimeException("Cannot create unsafe HTTP client.", ex);

			}

		}

		final Retrofit.Builder builder = new Retrofit.Builder()
		        .baseUrl(endpoint)
		        .client(okHttpClient)
				.addConverterFactory(ScalarsConverterFactory.create())
		        .addConverterFactory(GsonConverterFactory.create());

		final Retrofit retrofit = builder.build();

		service = retrofit.create(PhilterService.class);

	}

	public FilterResponse filter(String context, String filterProfileName, String text) throws IOException {

		final AtomicReference<String> documentId = new AtomicReference<>();

		final Call<String> call = service.filter(context, filterProfileName, text);

		call.enqueue(new Callback<String>() {

			@Override
			public void onResponse(Call<String> call, Response<String> response) {

				documentId.set(response.headers().get("x-document-id"));

			}

			@Override
			public void onFailure(Call<String> call, Throwable t) {

			}

		});

		final String filteredText = service.filter(context, filterProfileName, text).execute().body();

		return new FilterResponse(filteredText, context, documentId.get());

	}

	public List<FilteredSpan> replacements(String documentId) throws IOException {

		return service.replacements(documentId).execute().body();

	}

	public Status status() throws IOException {

		return service.status().execute().body();

	}

}
