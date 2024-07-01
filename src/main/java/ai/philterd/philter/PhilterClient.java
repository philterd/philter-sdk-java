/*******************************************************************************
 * Copyright 2023 Philterd, LLC
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
package ai.philterd.philter;

import ai.philterd.philter.model.*;
import ai.philterd.philter.model.exceptions.ClientException;
import ai.philterd.philter.model.exceptions.ServiceUnavailableException;
import ai.philterd.philter.model.exceptions.UnauthorizedException;
import ai.philterd.philter.services.PhilterService;
import nl.altindag.sslcontext.SSLFactory;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client class for Philter's API. Philter finds and manipulates sensitive information in text.
 * For more information on Philter see https://www.philterd.ai.
 */
public class PhilterClient extends AbstractClient {

	public static final int DEFAULT_TIMEOUT_SEC = 30;
	public static final int DEFAULT_MAX_IDLE_CONNECTIONS = 20;
	public static final int DEFAULT_KEEP_ALIVE_DURATION_MS = 30 * 1000;

	private PhilterService service;

	public static class PhilterClientBuilder {

		private String endpoint;
		private OkHttpClient.Builder okHttpClientBuilder;
		private long timeout = DEFAULT_TIMEOUT_SEC;
		private int maxIdleConnections = DEFAULT_MAX_IDLE_CONNECTIONS;
		private int keepAliveDurationMs = DEFAULT_KEEP_ALIVE_DURATION_MS;
		private String keystore;
		private String keystorePassword;
		private String truststore;
		private String truststorePassword;

		public PhilterClientBuilder withEndpoint(String endpoint) {
			this.endpoint = endpoint;
			return this;
		}

		public PhilterClientBuilder withOkHttpClientBuilder(OkHttpClient.Builder okHttpClientBuilder) {
			this.okHttpClientBuilder = okHttpClientBuilder;
			return this;
		}

		public PhilterClientBuilder withTimeout(long timeout) {
			this.timeout = timeout;
			return this;
		}

		public PhilterClientBuilder withMaxIdleConnections(int maxIdleConnections) {
			this.maxIdleConnections = maxIdleConnections;
			return this;
		}

		public PhilterClientBuilder withKeepAliveDurationMs(int keepAliveDurationMs) {
			this.keepAliveDurationMs = keepAliveDurationMs;
			return this;
		}

		public PhilterClientBuilder withSslConfiguration(String keystore, String keystorePassword, String truststore, String truststorePassword) {
			this.keystore = keystore;
			this.keystorePassword = keystorePassword;
			this.truststore = truststore;
			this.truststorePassword = truststorePassword;
			return this;
		}

		public PhilterClient build() throws Exception {
			return new PhilterClient(endpoint, okHttpClientBuilder, timeout, maxIdleConnections, keepAliveDurationMs, keystore,
					keystorePassword, truststore, truststorePassword);
		}

	}

	private PhilterClient(String endpoint, OkHttpClient.Builder okHttpClientBuilder, long timeout, int maxIdleConnections, int keepAliveDurationMs,
		String keystore, String keystorePassword, String truststore, String truststorePassword) throws Exception {

		if(okHttpClientBuilder == null) {

			okHttpClientBuilder = new OkHttpClient.Builder()
					.connectTimeout(timeout, TimeUnit.SECONDS)
					.writeTimeout(timeout, TimeUnit.SECONDS)
					.readTimeout(timeout, TimeUnit.SECONDS)
					.connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDurationMs, TimeUnit.MILLISECONDS));

		}

		if(StringUtils.isNotEmpty(keystore)) {
			configureSSL(okHttpClientBuilder, keystore, keystorePassword, truststore, truststorePassword);
		}

		final OkHttpClient okHttpClient = okHttpClientBuilder.build();

		final Retrofit.Builder builder = new Retrofit.Builder()
				.baseUrl(endpoint)
				.client(okHttpClient)
				.addConverterFactory(ScalarsConverterFactory.create())
				.addConverterFactory(GsonConverterFactory.create());

		final Retrofit retrofit = builder.build();

		service = retrofit.create(PhilterService.class);

	}

	private void configureSSL(final OkHttpClient.Builder okHttpClientBuilder, String keystore, String keystorePassword,
							 String truststore, String truststorePassword) {

		final SSLFactory sslFactory = SSLFactory.builder()
				.withIdentityMaterial(Paths.get(keystore), keystorePassword.toCharArray())
				.withTrustMaterial(Paths.get(truststore), truststorePassword.toCharArray())
				.build();

		okHttpClientBuilder.sslSocketFactory(sslFactory.getSslSocketFactory(), sslFactory.getTrustManager().get());

	}

	/**
	 * Send text to Philter to be filtered.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param documentId The document ID. Leave empty for Philter to assign a document ID to the request.
	 * @param policyName The name of the policy to apply to the text.
	 * @param text The text to be filtered.
	 * @return The filtered text.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public FilterResponse filter(String context, String documentId, String policyName, String text) throws IOException {

		final Response<String> response = service.filter(context, documentId, policyName, text).execute();

		if(response.isSuccessful()) {

			documentId = response.headers().get("x-document-id");
			return new FilterResponse(response.body(), context, documentId);

		} else {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Send a PDF document to Philter to be filtered.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param documentId The document ID. Leave empty for Philter to assign a document ID to the request.
	 * @param policyName The name of the policy to apply to the text.
	 * @param file The PDF file to be filtered.
	 * @return The filtered text.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public BinaryFilterResponse filter(String context, String documentId, String policyName, File file) throws IOException {

		final byte[] params = FileUtils.readFileToByteArray(file);
		final RequestBody body = RequestBody.create(MediaType.parse("application/pdf"), params);

		final Response<ResponseBody> response = service.filter(context, documentId, policyName, body).execute();

		if(response.isSuccessful()) {

			documentId = response.headers().get("x-document-id");
			return new BinaryFilterResponse(context, documentId, response.body().bytes());

		} else {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Send text to Philter to be filtered and get an explanation.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param documentId The document ID. Leave empty for Philter to assign a document ID to the request.
	 * @param policyName The name of the policy to apply to the text.
	 * @param text The text to be filtered.
	 * @return The filter {@link ExplainResponse}.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public ExplainResponse explain(String context, String documentId, String policyName, String text) throws IOException {

		final Response<ExplainResponse> response = service.explain(context, documentId, policyName, text).execute();

		if(response.isSuccessful()) {

			return response.body();

		} else {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Gets the values replaced during a previous filter request. Philter's store feature must be enabled
	 * for this call to work. Check Philter's documentation for how to enable the store.
	 * @param documentId The document ID.
	 * @return A list of {@link FilteredSpan spans}.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public List<FilteredSpan> replacements(String documentId) throws IOException {

		final Response<List<FilteredSpan>> response = service.replacements(documentId).execute();

		if(response.isSuccessful()) {

			return response.body();

		} else {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Gets the status of Philter.
	 * @return A string containing the status of Philter.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String status() throws IOException {

		final Response<String> response = service.status().execute();

		if(response.isSuccessful()) {

			return response.body();

		} else {

			if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Gets a list of policy names.
	 * @return A list of policy names.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public List<String> getPolicies() throws IOException {

		final Response<List<String>> response = service.Policy().execute();

		if(response.isSuccessful()) {

			return response.body();

		} else {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Gets the content of a policy.
	 * @param policyName The name of the policy to get.
	 * @return The content of the policy.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public String Policy(String policyName) throws IOException {

		final Response<String> response = service.Policy(policyName).execute();

		if(response.isSuccessful()) {

			return response.body();

		} else {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Saves (or overwrites) the policy.
	 * @param json The body of the policy.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public void savePolicy(String json) throws IOException {

		final Response<Void> response = service.savePolicy(json).execute();

		if(!response.isSuccessful()) {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Deletes a policy.
	 * @param policyName The name of the policy to delete.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public void deletePolicy(String policyName) throws IOException {

		final Response<Void> response = service.deletePolicy(policyName).execute();

		if(!response.isSuccessful()) {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Get alerts.
	 * @return A list of {@link Alert alerts}.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public List<Alert> getAlerts() throws IOException {

		final Response<List<Alert>> response = service.getAlerts().execute();

		if(response.isSuccessful()) {

			return response.body();

		} else {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Delete an alert.
	 * @param alertId The ID of the alert to delete.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public void deleteAlert(String alertId) throws IOException {

		final Response<Void> response = service.deleteAlert(alertId).execute();

		if(!response.isSuccessful()) {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

}
