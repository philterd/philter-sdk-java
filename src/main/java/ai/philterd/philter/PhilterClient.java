/*******************************************************************************
 * Copyright 2026 Philterd, LLC
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package ai.philterd.philter;

import ai.philterd.philter.model.BinaryFilterResponse;
import ai.philterd.philter.model.ExplainResponse;
import ai.philterd.philter.model.FilterResponse;
import ai.philterd.philter.model.exceptions.ClientException;
import ai.philterd.philter.model.exceptions.ServiceUnavailableException;
import ai.philterd.philter.model.exceptions.UnauthorizedException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;

/**
 * Client class for Philter's API. Philter finds and manipulates sensitive information in text.
 * For more information on Philter see <a href="https://www.philterd.ai">Philterd</a>.
 */
public class PhilterClient {

	public static final String UNAUTHORIZED = "Unauthorized";
	public static final String SERVICE_UNAVAILABLE = "Service unavailable";

	public static final int DEFAULT_TIMEOUT_SEC = 30;

	/**
	 * @deprecated The JDK HTTP client does not expose per-client connection pool sizing. Use the
	 * {@code jdk.httpclient.connectionPoolSize} system property instead.
	 */
	@Deprecated
	public static final int DEFAULT_MAX_IDLE_CONNECTIONS = 20;

	/**
	 * @deprecated The JDK HTTP client does not expose per-client keep-alive tuning. Use the
	 * {@code jdk.httpclient.keepalive.timeout} system property instead.
	 */
	@Deprecated
	public static final int DEFAULT_KEEP_ALIVE_DURATION_MS = 30 * 1000;

	private static final String DOCUMENT_ID_HEADER = "x-document-id";

	private final HttpClient httpClient;
	private final URI endpoint;
	private final Duration timeout;
	private final Gson gson = new Gson();

	public static class PhilterClientBuilder {

		private String endpoint;
		private HttpClient.Builder httpClientBuilder;
		private long timeout = DEFAULT_TIMEOUT_SEC;
		private String keystore;
		private String keystorePassword;
		private String truststore;
		private String truststorePassword;

		public PhilterClientBuilder withEndpoint(String endpoint) {
			this.endpoint = endpoint;
			return this;
		}

		/**
		 * Supplies a pre-configured {@link HttpClient.Builder}, for cases such as proxies, a custom
		 * executor, or a bespoke {@link SSLContext}. When given, the {@code timeout} setting is not
		 * applied to the client and should be configured on the supplied builder instead. Any SSL
		 * configuration from {@link #withSslConfiguration} is still applied on top of it.
		 *
		 * <p>This replaces the {@code withOkHttpClientBuilder} method of earlier releases.</p>
		 *
		 * @param httpClientBuilder The HTTP client builder to use.
		 * @return This builder.
		 */
		public PhilterClientBuilder withHttpClientBuilder(HttpClient.Builder httpClientBuilder) {
			this.httpClientBuilder = httpClientBuilder;
			return this;
		}

		/**
		 * Sets the connect timeout and the per-request timeout, in seconds.
		 * @param timeout The timeout in seconds.
		 * @return This builder.
		 */
		public PhilterClientBuilder withTimeout(long timeout) {
			this.timeout = timeout;
			return this;
		}

		/**
		 * @param maxIdleConnections Ignored.
		 * @return This builder.
		 * @deprecated Has no effect. The JDK HTTP client sizes its connection pool through the
		 * {@code jdk.httpclient.connectionPoolSize} system property.
		 */
		@Deprecated
		public PhilterClientBuilder withMaxIdleConnections(int maxIdleConnections) {
			return this;
		}

		/**
		 * @param keepAliveDurationMs Ignored.
		 * @return This builder.
		 * @deprecated Has no effect. The JDK HTTP client tunes keep-alive through the
		 * {@code jdk.httpclient.keepalive.timeout} system property.
		 */
		@Deprecated
		public PhilterClientBuilder withKeepAliveDurationMs(int keepAliveDurationMs) {
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
			return new PhilterClient(endpoint, httpClientBuilder, timeout, keystore, keystorePassword, truststore, truststorePassword);
		}

	}

	private PhilterClient(String endpoint, HttpClient.Builder httpClientBuilder, long timeout, String keystore,
	                      String keystorePassword, String truststore, String truststorePassword)
			throws IOException, GeneralSecurityException {

		this.endpoint = URI.create(endpoint);
		this.timeout = Duration.ofSeconds(timeout);

		if(httpClientBuilder == null) {

			httpClientBuilder = HttpClient.newBuilder()
					.connectTimeout(Duration.ofSeconds(timeout))
					// OkHttp followed redirects by default; NORMAL matches that without following
					// an HTTPS to HTTP downgrade.
					.followRedirects(HttpClient.Redirect.NORMAL)
					// Pinned so the wire behaviour matches the previous OkHttp-based releases.
					// Callers wanting HTTP/2 can set it via withHttpClientBuilder.
					.version(HttpClient.Version.HTTP_1_1);

			// Unlike OkHttp, the JDK client ignores the http.proxyHost/https.proxyHost system
			// properties unless a selector is set explicitly.
			final ProxySelector proxySelector = ProxySelector.getDefault();

			if(proxySelector != null) {
				httpClientBuilder.proxy(proxySelector);
			}

		}

		if(keystore != null && !keystore.isEmpty()) {
			httpClientBuilder.sslContext(createSslContext(keystore, keystorePassword, truststore, truststorePassword));
		}

		this.httpClient = httpClientBuilder.build();

	}

	private static SSLContext createSslContext(String keystore, String keystorePassword, String truststore,
	                                           String truststorePassword) throws IOException, GeneralSecurityException {

		final char[] keystorePw = keystorePassword.toCharArray();

		final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(loadKeyStore(keystore, keystorePw), keystorePw);

		// When no truststore is given, fall back to the JDK's default trust material.
		final KeyStore trustStore = (truststore != null && !truststore.isEmpty())
				? loadKeyStore(truststore, truststorePassword == null ? null : truststorePassword.toCharArray())
				: null;

		final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);

		final SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

		return sslContext;

	}

	/**
	 * Loads a keystore or truststore from disk using the JVM's default keystore type. The JDK's
	 * keystore compatibility mode means both PKCS12 and JKS files are read.
	 * @param path The path to the keystore file.
	 * @param password The keystore password.
	 * @return The loaded {@link KeyStore}.
	 */
	private static KeyStore loadKeyStore(final String path, final char[] password) throws IOException, GeneralSecurityException {

		final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

		try (final InputStream inputStream = Files.newInputStream(Paths.get(path))) {
			keyStore.load(inputStream, password);
		}

		return keyStore;

	}

	// Request plumbing.

	/**
	 * Builds an absolute request URI. Query parameters are given as name/value pairs and a pair
	 * whose value is {@code null} is omitted from the query string.
	 */
	private URI uri(final String path, final String... queryParameters) {

		final StringBuilder builder = new StringBuilder(path);
		char separator = '?';

		for(int i = 0; i < queryParameters.length; i += 2) {

			final String value = queryParameters[i + 1];

			if(value == null) {
				continue;
			}

			builder.append(separator).append(encode(queryParameters[i])).append('=').append(encode(value));
			separator = '&';

		}

		return endpoint.resolve(builder.toString());

	}

	/**
	 * Percent-encodes a single path segment or query component. {@link URLEncoder} emits {@code +}
	 * for a space, which is only correct for form bodies, so it is rewritten to {@code %20}.
	 */
	private static String encode(final String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	/**
	 * Determines whether a response carries a 2xx status code.
	 */
	private static boolean isSuccessful(final HttpResponse<?> response) {
		return response.statusCode() >= 200 && response.statusCode() < 300;
	}

	/**
	 * Maps an HTTP status code onto the corresponding client exception.
	 */
	private static RuntimeException toException(final int code) {

		if(code == 401) {
			return new UnauthorizedException(UNAUTHORIZED);
		} else if(code == 503) {
			return new ServiceUnavailableException(SERVICE_UNAVAILABLE);
		} else {
			return new ClientException("Unknown error: HTTP " + code);
		}

	}

	private HttpRequest.Builder request(final URI uri) {
		return HttpRequest.newBuilder(uri).timeout(timeout);
	}

	private <T> HttpResponse<T> send(final HttpRequest request, final HttpResponse.BodyHandler<T> bodyHandler) throws IOException {

		try {

			return httpClient.send(request, bodyHandler);

		} catch (final InterruptedException ex) {

			Thread.currentThread().interrupt();
			throw new IOException("The request was interrupted.", ex);

		}

	}

	/**
	 * Sends a request whose response body is not used, failing on a non-2xx status.
	 */
	private void sendExpectingNoContent(final HttpRequest request) throws IOException {

		final HttpResponse<Void> response = send(request, HttpResponse.BodyHandlers.discarding());

		if(!isSuccessful(response)) {
			throw toException(response.statusCode());
		}

	}

	/**
	 * Sends a request and returns the response body as a string, failing on a non-2xx status.
	 */
	private String sendExpectingString(final HttpRequest request) throws IOException {

		final HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());

		if(isSuccessful(response)) {
			return response.body();
		}

		throw toException(response.statusCode());

	}

	// Filtering.

	/**
	 * Send text to Philter to be filtered.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the text.
	 * @param text The text to be filtered.
	 * @return The filtered text.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public FilterResponse filter(String context, String policyName, String text) throws IOException {

		final HttpRequest request = request(uri("/api/filter", "c", context, "p", policyName))
				.header("Accept", "text/plain")
				.header("Content-Type", "text/plain")
				.POST(HttpRequest.BodyPublishers.ofString(text, StandardCharsets.UTF_8))
				.build();

		final HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());

		if(isSuccessful(response)) {

			final String documentId = response.headers().firstValue(DOCUMENT_ID_HEADER).orElse(null);
			return new FilterResponse(response.body(), context, documentId);

		}

		throw toException(response.statusCode());

	}

	/**
	 * Send a PDF document to Philter to be filtered.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the text.
	 * @param file The PDF file to be filtered.
	 * @return The filtered document as a ZIP archive.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public BinaryFilterResponse filter(String context, String policyName, File file) throws IOException {
		return filterBinary(context, policyName, file, "application/zip");
	}

	/**
	 * Send a PDF document to Philter to be filtered, receiving the filtered document as a PDF
	 * rather than as a ZIP archive.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the text.
	 * @param file The PDF file to be filtered.
	 * @return The filtered document as a PDF.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public BinaryFilterResponse filterToPdf(String context, String policyName, File file) throws IOException {
		return filterBinary(context, policyName, file, "application/pdf");
	}

	private BinaryFilterResponse filterBinary(String context, String policyName, File file, String accept) throws IOException {

		final byte[] content = Files.readAllBytes(file.toPath());

		final HttpRequest request = request(uri("/api/filter", "c", context, "p", policyName))
				.header("Accept", accept)
				.header("Content-Type", "application/pdf")
				.POST(HttpRequest.BodyPublishers.ofByteArray(content))
				.build();

		final HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray());

		if(isSuccessful(response)) {

			final String documentId = response.headers().firstValue(DOCUMENT_ID_HEADER).orElse(null);
			return new BinaryFilterResponse(context, documentId, response.body());

		}

		throw toException(response.statusCode());

	}

	/**
	 * Send text to Philter to be filtered and get an explanation.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the text.
	 * @param text The text to be filtered.
	 * @return The filter {@link ExplainResponse}.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public ExplainResponse explain(String context, String policyName, String text) throws IOException {

		final HttpRequest request = request(uri("/api/explain", "c", context, "p", policyName))
				.header("Accept", "application/json")
				.header("Content-Type", "text/plain")
				.POST(HttpRequest.BodyPublishers.ofString(text, StandardCharsets.UTF_8))
				.build();

		return gson.fromJson(sendExpectingString(request), ExplainResponse.class);

	}

	// Status.

	/**
	 * Gets the status of Philter.
	 * @return A string containing the status of Philter.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String status() throws IOException {

		final HttpRequest request = request(uri("/api/status")).GET().build();

		final HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());

		if(isSuccessful(response)) {
			return response.body();
		}

		// Note: unlike every other operation here, a 401 is reported as a ClientException rather
		// than an UnauthorizedException. Preserved from the pre-rewrite behaviour.
		if(response.statusCode() == 503) {
			throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);
		}

		throw new ClientException("Unknown error: HTTP " + response.statusCode());

	}

	// Policies.

	/**
	 * Gets a list of policy names.
	 * @return A list of policy names.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public List<String> getPolicies() throws IOException {

		final HttpRequest request = request(uri("/api/policies")).header("Accept", "application/json").GET().build();

		return gson.fromJson(sendExpectingString(request), new TypeToken<List<String>>() {}.getType());

	}

	/**
	 * Gets the content of a policy.
	 * @param policyName The name of the policy to get.
	 * @return The content of the policy.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public String Policy(String policyName) throws IOException {

		final HttpRequest request = request(uri("/api/policies/" + encode(policyName)))
				// Philter serialises the policy as JSON; asking for text/plain draws a 406.
				.header("Accept", "application/json")
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Saves (or overwrites) the policy.
	 *
	 * <p>The name is sent as the {@code name} query parameter, which Philter requires. It is not
	 * carried in the policy body, so it cannot be derived from {@code json}.</p>
	 *
	 * @param name The name to save the policy under.
	 * @param json The body of the policy.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public void savePolicy(String name, String json) throws IOException {

		final HttpRequest request = request(uri("/api/policies", "name", name))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
				.build();

		sendExpectingNoContent(request);

	}

	/**
	 * Deletes a policy.
	 * @param policyName The name of the policy to delete.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public void deletePolicy(String policyName) throws IOException {
		sendExpectingNoContent(request(uri("/api/policies/" + encode(policyName))).DELETE().build());
	}

}
