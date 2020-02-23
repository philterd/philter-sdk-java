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

import com.mtnfog.philter.sdk.model.ExplainResponse;
import com.mtnfog.philter.sdk.model.FilterResponse;
import com.mtnfog.philter.sdk.model.FilteredSpan;
import com.mtnfog.philter.sdk.model.StatusResponse;
import com.mtnfog.philter.sdk.service.PhilterService;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client class for Philter's API. Philter finds and manipulates sensitive information in text.
 */
public class PhilterClient {

	private static final Logger LOGGER = LogManager.getLogger(PhilterClient.class);

	public static final int TIMEOUT_SEC = 30;
	public static final int MAX_IDLE_CONNECTIONS = 20;
	public static final int KEEP_ALIVE_DURATION_MS = 30 * 1000;

	private PhilterService service;

	/**
	 * Create a new client.
	 * @param endpoint The Philter endpoint, e.g. <code>https://127.0.0.1:8080</code>.
	 */
	public PhilterClient(String endpoint) {

		this(endpoint, null);

	}

	/**
	 * Create a new client.
	 * @param endpoint The Philter endpoint, e.g. <code>https://127.0.0.1:8080</code>.
	 * @param okHttpClient Provide a custom {@link OkHttpClient}.
	 */
	public PhilterClient(String endpoint, OkHttpClient okHttpClient) {

		if(okHttpClient == null) {

			okHttpClient = new OkHttpClient.Builder()
					.connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
					.writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
					.readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
					.connectionPool(new ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_DURATION_MS, TimeUnit.MILLISECONDS))
					.build();

		}

		final Retrofit.Builder builder = new Retrofit.Builder()
		        .baseUrl(endpoint)
		        .client(okHttpClient)
				.addConverterFactory(ScalarsConverterFactory.create())
		        .addConverterFactory(GsonConverterFactory.create());

		final Retrofit retrofit = builder.build();

		service = retrofit.create(PhilterService.class);

	}

	/**
	 * Send text to Philter to be filtered.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param documentId The document ID. Leave empty for Philter to assign a document ID to the request.
	 * @param filterProfileName The name of the filter profile to apply to the text.
	 * @param text The text to be filtered.
	 * @return The filtered text.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public FilterResponse filter(String context, String documentId, String filterProfileName, String text) throws IOException {

		final Response<String> response = service.filter(context, documentId, filterProfileName, text).execute();

		if(response.isSuccessful()) {

			documentId = response.headers().get("x-document-id");
			return new FilterResponse(response.body(), context, documentId);

		}

		throw new IOException("Unable to process text. Check Philter log for details.");

	}

	/**
	 * Send text to Philter to be filtered and get an explanation.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param documentId The document ID. Leave empty for Philter to assign a document ID to the request.
	 * @param filterProfileName The name of the filter profile to apply to the text.
	 * @param text The text to be filtered.
	 * @return The filter {@link ExplainResponse}.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public ExplainResponse explain(String context, String documentId, String filterProfileName, String text) throws IOException {

		final Response<ExplainResponse> response = service.explain(context, documentId, filterProfileName, text).execute();

		if(response.isSuccessful()) {

			documentId = response.headers().get("x-document-id");
			return response.body();

		}

		throw new IOException("Unable to process text. Check Philter log for details.");

	}

	/**
	 * Gets the values replaced during a previous filter request. Philter's store feature must be enabled
	 * for this call to work. Check Philter's documentation for how to enable the store.
	 * @param documentId The document ID.
	 * @return A list of {@link FilteredSpan spans}.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public List<FilteredSpan> replacements(String documentId) throws IOException {

		return service.replacements(documentId).execute().body();

	}

	/**
	 * Gets the status of Philter.
	 * @return A {@link StatusResponse} object.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public StatusResponse status() throws IOException {

		return service.status().execute().body();

	}

}
