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

import com.mtnfog.philter.sdk.model.Status;
import com.mtnfog.philter.sdk.service.FilterProfileRegistryService;
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
 * Client class for managing filter profiles either with Philter or with a Philter Profile Registry.
 */
public class FilterProfileRegistryClient {

	private static final Logger LOGGER = LogManager.getLogger(FilterProfileRegistryClient.class);

	private FilterProfileRegistryService service;

	public static final int TIMEOUT_SEC = 30;
	public static final int MAX_IDLE_CONNECTIONS = 20;
	public static final int KEEP_ALIVE_DURATION_MS = 30 * 1000;

	/**
	 * Creates a new client.
	 * @param endpoint The Philter endpoint or the Filter Profile Registry endpoint, e.g. <code>https://127.0.0.1:8080</code>.
	 */
	public FilterProfileRegistryClient(String endpoint) {

		this(endpoint, null);

	}

	/**
	 * Creates a new client.
	 * @param endpoint The Philter endpoint or the Filter Profile Registry endpoint, e.g. <code>https://127.0.0.1:8080</code>.
	 * @param okHttpClient A custom {@link OkHttpClient}.
	 */
	public FilterProfileRegistryClient(String endpoint, OkHttpClient okHttpClient) {

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

		service = retrofit.create(FilterProfileRegistryService.class);

	}

	/**
	 * Gets the status of Philter or the Filter Profile Registry.
	 * @return The {@link Status}.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public Status status() throws IOException {

		return service.status().execute().body();

	}

	/**
	 * Gets a list of filter profile names.
	 * @return A list of filter profile names.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public List<String> get() throws IOException {

		return service.get().execute().body();

	}

	/**
	 * Gets the content of a filter profile.
	 * @param filterProfileName The name of the filter profile to get.
	 * @return The content of the filter profile.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public String get(String filterProfileName) throws IOException {

		return service.get(filterProfileName).execute().body();

	}

	/**
	 * Saves (or overwrites) the filter profile.
	 * @param json The body of the filter profile.
	 * @return <code>true</code> if successful, otherwise <code>false</code>.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public boolean save(String json) throws IOException {

		final Response response = service.save(json).execute();
		return response.isSuccessful();

	}

	/**
	 * Deletes a filter profile.
	 * @param filterProfileName The name of the filter profile to delete.
	 * @return <code>true</code> if successful, otherwise <code>false</code>.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public boolean delete(String filterProfileName) throws IOException {

		final Response response = service.delete(filterProfileName).execute();
		return response.isSuccessful();

	}

}
