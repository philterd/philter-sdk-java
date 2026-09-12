/*******************************************************************************
 * Copyright 2026 Philterd, LLC
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

import ai.philterd.philter.model.AsyncFilterResponse;
import ai.philterd.philter.model.BinaryFilterResponse;
import ai.philterd.philter.model.ExplainResponse;
import ai.philterd.philter.model.FilterResponse;
import ai.philterd.philter.model.GenericResponse;
import ai.philterd.philter.model.GetListsResponse;
import ai.philterd.philter.model.LegalHoldRequest;
import ai.philterd.philter.model.LegalHoldResponse;
import ai.philterd.philter.model.PolicyRollbackResponse;
import ai.philterd.philter.model.PolicyVersionSummary;
import ai.philterd.philter.model.ReidentifyRequest;
import ai.philterd.philter.model.StatusResponse;
import ai.philterd.philter.model.exceptions.ClientException;
import ai.philterd.philter.model.exceptions.ServiceUnavailableException;
import ai.philterd.philter.model.exceptions.UnauthorizedException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

/**
 * Client class for Philter's API. Philter finds and manipulates sensitive information in text.
 * This client targets the Philter 4.0.0 API. For more information on Philter see https://www.philterd.ai.
 *
 * <p>Requests are made with the JDK's {@link HttpClient}; the client has no third-party HTTP
 * dependency.</p>
 *
 * <p>Instances are created with the {@link PhilterClientBuilder}, for example:</p>
 *
 * <pre>{@code
 * PhilterClient client = new PhilterClient.PhilterClientBuilder()
 *         .withEndpoint("https://localhost:8080")
 *         .withApiKey("your-api-key")
 *         .build();
 * }</pre>
 *
 * <p>In addition to the checked {@link IOException} thrown when a request cannot be executed, the operation
 * methods throw an unchecked {@link ai.philterd.philter.model.exceptions.UnauthorizedException} on an HTTP 401,
 * a {@link ai.philterd.philter.model.exceptions.ServiceUnavailableException} on an HTTP 503, and a
 * {@link ai.philterd.philter.model.exceptions.ClientException} on any other non-successful response.</p>
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

	private static final String APPLICATION_JSON = "application/json";
	private static final String TEXT_PLAIN = "text/plain";

	private static final Type STRING_LIST = new TypeToken<List<String>>() {}.getType();
	private static final Type POLICY_VERSION_LIST = new TypeToken<List<PolicyVersionSummary>>() {}.getType();
	private static final Type LEGAL_HOLD_LIST = new TypeToken<List<LegalHoldResponse>>() {}.getType();

	private final HttpClient httpClient;
	private final URI endpoint;
	private final Duration timeout;
	private final String apiKey;
	private final Gson gson = new Gson();

	/**
	 * Builds {@link PhilterClient} instances. Only {@link #withEndpoint(String)} is required; all other
	 * settings are optional and fall back to sensible defaults.
	 */
	public static class PhilterClientBuilder {

		private String endpoint;
		private HttpClient.Builder httpClientBuilder;
		private long timeout = DEFAULT_TIMEOUT_SEC;
		private String apiKey;

		/**
		 * Sets the base URL of the Philter instance, for example {@code https://localhost:8080}. Required.
		 * @param endpoint The Philter endpoint URL.
		 * @return This builder.
		 */
		public PhilterClientBuilder withEndpoint(String endpoint) {
			this.endpoint = endpoint;
			return this;
		}

		/**
		 * Supplies a pre-configured {@link HttpClient.Builder}, for cases such as proxies, a custom
		 * executor, or a bespoke {@link javax.net.ssl.SSLContext}. When given, the connect timeout is
		 * not applied to the client and should be configured on the supplied builder instead; the
		 * per-request timeout from {@link #withTimeout(long)} and the {@code Authorization} header
		 * still apply.
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
		 * Sets the connect timeout and the per-request timeout, in seconds. Defaults to
		 * {@link #DEFAULT_TIMEOUT_SEC}.
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

		/**
		 * Sets the value sent in the {@code Authorization} header on every request.
		 * The value is sent verbatim, so include any scheme prefix (for example
		 * {@code "Bearer "}) if your Philter deployment requires it.
		 * @param apiKey The Authorization header value.
		 * @return This builder.
		 */
		public PhilterClientBuilder withApiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		/**
		 * Builds the configured {@link PhilterClient}.
		 * @return A new {@link PhilterClient}.
		 */
		public PhilterClient build() {
			return new PhilterClient(endpoint, httpClientBuilder, timeout, apiKey);
		}

	}

	private PhilterClient(String endpoint, HttpClient.Builder httpClientBuilder, long timeout, String apiKey) {

		this.endpoint = URI.create(endpoint);
		this.timeout = Duration.ofSeconds(timeout);
		this.apiKey = apiKey;

		if(httpClientBuilder == null) {

			httpClientBuilder = HttpClient.newBuilder()
					.connectTimeout(Duration.ofSeconds(timeout))
					// OkHttp followed redirects by default; NORMAL matches that without following
					// an HTTPS to HTTP downgrade.
					.followRedirects(HttpClient.Redirect.NORMAL)
					// Pinned so the wire behavior matches the previous OkHttp-based releases.
					// Callers wanting HTTP/2 can set it via withHttpClientBuilder.
					.version(HttpClient.Version.HTTP_1_1);

			// Unlike OkHttp, the JDK client ignores the http.proxyHost/https.proxyHost system
			// properties unless a selector is set explicitly.
			final ProxySelector proxySelector = ProxySelector.getDefault();

			if(proxySelector != null) {
				httpClientBuilder.proxy(proxySelector);
			}

		}

		this.httpClient = httpClientBuilder.build();

	}

	// Request plumbing.

	/**
	 * Builds an absolute request URI. Query parameters are given as name/value pairs and a pair
	 * whose value is {@code null} is omitted from the query string.
	 */
	private URI uri(final String path, final Object... queryParameters) {

		final StringBuilder builder = new StringBuilder(path);
		char separator = '?';

		for(int i = 0; i < queryParameters.length; i += 2) {

			final Object value = queryParameters[i + 1];

			if(value == null) {
				continue;
			}

			builder.append(separator).append(encode(String.valueOf(queryParameters[i])))
					.append('=').append(encode(String.valueOf(value)));
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

	/** How much of an error response body is carried in the exception message. */
	private static final int MAX_ERROR_BODY_LENGTH = 512;

	/**
	 * Maps an HTTP status code to a client exception. Philter explains a rejected request in the
	 * response body, so the body is carried into the {@link ClientException} message; a caller left
	 * with only a status code has nothing to act on.
	 * @param code The HTTP status code.
	 * @param body The response body, which may be {@code null} or empty.
	 * @return A {@link RuntimeException} describing the error.
	 */
	private static RuntimeException toException(final int code, final String body) {

		if(code == 401) {
			return new UnauthorizedException(UNAUTHORIZED);
		} else if(code == 503) {
			return new ServiceUnavailableException(SERVICE_UNAVAILABLE);
		} else {
			return new ClientException(describe(code, body));
		}

	}

	private static String describe(final int code, final String body) {

		final String message = "Unknown error: HTTP " + code;

		if(body == null || body.isBlank()) {
			return message;
		}

		final String trimmed = body.strip();

		return message + ": " + (trimmed.length() > MAX_ERROR_BODY_LENGTH
				? trimmed.substring(0, MAX_ERROR_BODY_LENGTH) + "..."
				: trimmed);

	}

	/**
	 * Starts a request, applying the per-request timeout and the {@code Authorization} header.
	 * The JDK client has no interceptor mechanism, so the header is set per request rather than
	 * on the client.
	 */
	private HttpRequest.Builder request(final URI uri) {

		final HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(timeout);

		if(apiKey != null && !apiKey.isEmpty()) {
			builder.header("Authorization", apiKey);
		}

		return builder;

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

		// Read rather than discard the body: on a failure it carries Philter's explanation.
		final HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());

		if(!isSuccessful(response)) {
			throw toException(response.statusCode(), response.body());
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

		throw toException(response.statusCode(), response.body());

	}

	/**
	 * Sends a request and deserializes the JSON response body, failing on a non-2xx status.
	 */
	private <T> T sendExpectingJson(final HttpRequest request, final Type type) throws IOException {
		return gson.fromJson(sendExpectingString(request), type);
	}

	/**
	 * Sends a request and returns the response body as bytes, failing on a non-2xx status.
	 */
	private byte[] sendExpectingBytes(final HttpRequest request) throws IOException {

		final HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray());

		if(isSuccessful(response)) {
			return response.body();
		}

		throw toException(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));

	}

	private static HttpRequest.BodyPublisher text(final String body) {
		return HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
	}

	/**
	 * Starts a request that accepts a JSON response.
	 */
	private HttpRequest.Builder json(final URI uri) {
		return request(uri).header("Accept", APPLICATION_JSON);
	}

	// Filtering and explanation.

	/**
	 * Send text to Philter to be filtered.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the text.
	 * @param text The text to be filtered.
	 * @return The filtered text.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public FilterResponse filter(String context, String policyName, String text) throws IOException {
		return filter(context, policyName, null, text);
	}

	/**
	 * Send text to Philter to be filtered.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the text.
	 * @param filename The name of the file the text came from, recorded against the document. May be {@code null}.
	 * @param text The text to be filtered.
	 * @return The filtered text.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public FilterResponse filter(String context, String policyName, String filename, String text) throws IOException {

		// Philter's text endpoint is always synchronous, so the filtered text comes back in the response body.
		final HttpRequest request = request(uri("/api/filter", "c", context, "p", policyName, "filename", filename))
				.header("Accept", TEXT_PLAIN)
				.header("Content-Type", TEXT_PLAIN)
				.POST(text(text))
				.build();

		final HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());

		if(isSuccessful(response)) {

			final String documentId = response.headers().firstValue(DOCUMENT_ID_HEADER).orElse(null);
			return new FilterResponse(response.body(), context, documentId);

		}

		throw toException(response.statusCode(), response.body());

	}

	/**
	 * Send a PDF document to Philter to be filtered, waiting for the filtered document.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the document.
	 * @param filename The name of the file being filtered. May be {@code null}.
	 * @param file The PDF file to be filtered.
	 * @return The filtered document as a ZIP archive.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public BinaryFilterResponse filter(String context, String policyName, String filename, File file) throws IOException {
		return filterBinary(context, policyName, filename, file, "application/zip");
	}

	/**
	 * Send a PDF document to Philter to be filtered, waiting for the filtered document and receiving it
	 * as a PDF rather than as a ZIP archive.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the document.
	 * @param filename The name of the file being filtered. May be {@code null}.
	 * @param file The PDF file to be filtered.
	 * @return The filtered document as a PDF.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public BinaryFilterResponse filterToPdf(String context, String policyName, String filename, File file) throws IOException {
		return filterBinary(context, policyName, filename, file, "application/pdf");
	}

	private BinaryFilterResponse filterBinary(String context, String policyName, String filename, File file,
	                                          String accept) throws IOException {

		final HttpRequest request = binaryFilterRequest(context, policyName, filename, file, accept, false);

		final HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray());

		if(isSuccessful(response)) {

			final String documentId = response.headers().firstValue(DOCUMENT_ID_HEADER).orElse(null);
			return new BinaryFilterResponse(context, documentId, response.body());

		}

		throw toException(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));

	}

	/**
	 * Submits a PDF document to Philter to be filtered asynchronously. Philter accepts the document and
	 * returns immediately; poll {@link #getDocumentStatus(String)} with the returned document ID and
	 * retrieve the result with {@link #getDocument(String)}.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the document.
	 * @param filename The name of the file being filtered. May be {@code null}.
	 * @param file The PDF file to be filtered.
	 * @return The ID Philter assigned to the document.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String filterAsync(String context, String policyName, String filename, File file) throws IOException {
		return filterBinaryAsync(context, policyName, filename, file, "application/zip");
	}

	/**
	 * Submits a PDF document to Philter to be filtered asynchronously, with the result stored as a PDF
	 * rather than as a ZIP archive. Philter accepts the document and returns immediately; poll
	 * {@link #getDocumentStatus(String)} with the returned document ID and retrieve the result with
	 * {@link #getDocument(String)}.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the document.
	 * @param filename The name of the file being filtered. May be {@code null}.
	 * @param file The PDF file to be filtered.
	 * @return The ID Philter assigned to the document.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String filterToPdfAsync(String context, String policyName, String filename, File file) throws IOException {
		return filterBinaryAsync(context, policyName, filename, file, "application/pdf");
	}

	private String filterBinaryAsync(String context, String policyName, String filename, File file,
	                                 String accept) throws IOException {

		final HttpRequest request = binaryFilterRequest(context, policyName, filename, file, accept, true);

		// Philter answers an accepted submission with 202 and a JSON body carrying the document ID.
		final AsyncFilterResponse response = sendExpectingJson(request, AsyncFilterResponse.class);

		return response == null ? null : response.getDocumentId();

	}

	private HttpRequest binaryFilterRequest(String context, String policyName, String filename, File file,
	                                        String accept, boolean async) throws IOException {

		final byte[] content = Files.readAllBytes(file.toPath());

		return request(uri("/api/filter", "c", context, "p", policyName, "filename", filename, "async", async))
				.header("Accept", accept)
				.header("Content-Type", "application/pdf")
				.POST(HttpRequest.BodyPublishers.ofByteArray(content))
				.build();

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
		return explain(context, policyName, null, text);
	}

	/**
	 * Send text to Philter to be filtered and get an explanation.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the text.
	 * @param filename The name of the file the text came from, recorded against the document. May be {@code null}.
	 * @param text The text to be filtered.
	 * @return The filter {@link ExplainResponse}.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public ExplainResponse explain(String context, String policyName, String filename, String text) throws IOException {

		final HttpRequest request = request(uri("/api/explain", "c", context, "p", policyName, "filename", filename))
				.header("Accept", APPLICATION_JSON)
				.header("Content-Type", TEXT_PLAIN)
				.POST(text(text))
				.build();

		return sendExpectingJson(request, ExplainResponse.class);

	}

	/**
	 * Compiles a policy, returning the compiled representation.
	 * @param policy The policy to compile.
	 * @return The compiled policy.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String compilePolicy(String policy) throws IOException {

		final HttpRequest request = request(uri("/api/policies/compile"))
				.header("Accept", APPLICATION_JSON)
				.header("Content-Type", TEXT_PLAIN)
				.POST(text(policy))
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Re-identifies previously redacted values.
	 * @param owner The owner of the values. May be {@code null}.
	 * @param request The {@link ReidentifyRequest}.
	 * @return The re-identification result.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String reidentify(String owner, ReidentifyRequest request) throws IOException {

		final HttpRequest httpRequest = request(uri("/api/reidentify", "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(gson.toJson(request)))
				.build();

		return sendExpectingString(httpRequest);

	}

	// Status.

	/**
	 * Gets the health of Philter. This endpoint does not require authentication.
	 * @return The {@link StatusResponse}.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public StatusResponse health() throws IOException {
		return sendExpectingJson(json(uri("/api/health")).GET().build(), StatusResponse.class);
	}

	/**
	 * Gets Philter's public signing key. This endpoint does not require authentication so that a
	 * recipient can verify a signature without credentials.
	 * @return The signing key.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String getSigningKey() throws IOException {
		return sendExpectingString(json(uri("/api/signing-key")).GET().build());
	}

	/**
	 * Gets a retained public signing key by its ID. Like {@link #getSigningKey()}, this endpoint does
	 * not require authentication.
	 * @param keyId The ID of the signing key.
	 * @return The signing key.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String getSigningKey(String keyId) throws IOException {
		return sendExpectingString(json(uri("/api/signing-key/" + encode(keyId))).GET().build());
	}

	// Policies.

	/**
	 * Gets a list of policy names.
	 * @return A list of policy names.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<String> getPolicies() throws IOException {
		return getPolicies(null, null, null);
	}

	/**
	 * Gets a list of policy names.
	 * @param owner The owner of the policies. May be {@code null}.
	 * @param offset The pagination offset. May be {@code null}.
	 * @param limit The pagination limit. May be {@code null}.
	 * @return A list of policy names.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<String> getPolicies(String owner, Integer offset, Integer limit) throws IOException {

		final HttpRequest request = json(uri("/api/policies", "owner", owner, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingJson(request, STRING_LIST);

	}

	/**
	 * Gets the content of a policy.
	 * @param policyName The name of the policy to get.
	 * @return The content of the policy as JSON.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getPolicy(String policyName) throws IOException {
		return getPolicy(policyName, null);
	}

	/**
	 * Gets the content of a policy.
	 * @param policyName The name of the policy to get.
	 * @param owner The owner of the policy. May be {@code null}.
	 * @return The content of the policy as JSON.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getPolicy(String policyName, String owner) throws IOException {
		return sendExpectingString(json(uri("/api/policies/" + encode(policyName), "owner", owner)).GET().build());
	}

	/**
	 * Saves (or overwrites) the policy.
	 * @param name The name of the policy.
	 * @param json The body of the policy.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void savePolicy(String name, String json) throws IOException {
		savePolicy(name, json, null);
	}

	/**
	 * Saves (or overwrites) the policy.
	 * @param name The name of the policy.
	 * @param json The body of the policy.
	 * @param owner The owner of the policy. May be {@code null}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void savePolicy(String name, String json, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/policies", "name", name, "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(json))
				.build();

		sendExpectingNoContent(request);

	}

	/**
	 * Deletes a policy.
	 * @param policyName The name of the policy to delete.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deletePolicy(String policyName) throws IOException {
		deletePolicy(policyName, null);
	}

	/**
	 * Deletes a policy.
	 * @param policyName The name of the policy to delete.
	 * @param owner The owner of the policy. May be {@code null}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deletePolicy(String policyName, String owner) throws IOException {
		sendExpectingNoContent(request(uri("/api/policies/" + encode(policyName), "owner", owner)).DELETE().build());
	}

	/**
	 * Gets the revision history for a policy.
	 * @param policyName The name of the policy.
	 * @return A list of {@link PolicyVersionSummary}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<PolicyVersionSummary> getPolicyVersions(String policyName) throws IOException {
		return getPolicyVersions(policyName, null, null, null);
	}

	/**
	 * Gets the revision history for a policy.
	 * @param policyName The name of the policy.
	 * @param owner The owner of the policy. May be {@code null}.
	 * @param offset The pagination offset. May be {@code null}.
	 * @param limit The pagination limit. May be {@code null}.
	 * @return A list of {@link PolicyVersionSummary}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<PolicyVersionSummary> getPolicyVersions(String policyName, String owner, Integer offset, Integer limit)
			throws IOException {

		final HttpRequest request = json(uri("/api/policies/" + encode(policyName) + "/versions",
				"owner", owner, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingJson(request, POLICY_VERSION_LIST);

	}

	/**
	 * Gets a specific revision of a policy.
	 * @param policyName The name of the policy.
	 * @param revision The revision number.
	 * @return The policy content as JSON.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getPolicyVersion(String policyName, int revision) throws IOException {
		return getPolicyVersion(policyName, revision, null);
	}

	/**
	 * Gets a specific revision of a policy.
	 * @param policyName The name of the policy.
	 * @param revision The revision number.
	 * @param owner The owner of the policy. May be {@code null}.
	 * @return The policy content as JSON.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getPolicyVersion(String policyName, int revision, String owner) throws IOException {

		final HttpRequest request = json(uri("/api/policies/" + encode(policyName) + "/versions/" + revision,
				"owner", owner))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Gets the difference between two revisions of a policy.
	 * @param policyName The name of the policy.
	 * @param from The starting revision number.
	 * @param to The ending revision number.
	 * @return The difference.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getPolicyDiff(String policyName, int from, int to) throws IOException {
		return getPolicyDiff(policyName, from, to, null);
	}

	/**
	 * Gets the difference between two revisions of a policy.
	 * @param policyName The name of the policy.
	 * @param from The starting revision number.
	 * @param to The ending revision number.
	 * @param owner The owner of the policy. May be {@code null}.
	 * @return The difference.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getPolicyDiff(String policyName, int from, int to, String owner) throws IOException {

		final HttpRequest request = json(uri("/api/policies/" + encode(policyName) + "/diff",
				"from", from, "to", to, "owner", owner))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Rolls a policy back to a prior revision.
	 * @param policyName The name of the policy.
	 * @param revision The revision number to roll back to.
	 * @return The {@link PolicyRollbackResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public PolicyRollbackResponse rollbackPolicy(String policyName, int revision) throws IOException {
		return rollbackPolicy(policyName, revision, null);
	}

	/**
	 * Rolls a policy back to a prior revision.
	 * @param policyName The name of the policy.
	 * @param revision The revision number to roll back to.
	 * @param owner The owner of the policy. May be {@code null}.
	 * @return The {@link PolicyRollbackResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public PolicyRollbackResponse rollbackPolicy(String policyName, int revision, String owner) throws IOException {

		final HttpRequest request = json(uri("/api/policies/" + encode(policyName) + "/rollback",
				"revision", revision, "owner", owner))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		return sendExpectingJson(request, PolicyRollbackResponse.class);

	}

	// Contexts.

	/**
	 * Gets the configured contexts.
	 * @return The contexts.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getContexts() throws IOException {
		return getContexts(null, null, null);
	}

	/**
	 * Gets the configured contexts.
	 * @param owner The owner of the contexts. May be {@code null}.
	 * @param offset The pagination offset. May be {@code null}.
	 * @param limit The pagination limit. May be {@code null}.
	 * @return The contexts.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getContexts(String owner, Integer offset, Integer limit) throws IOException {

		final HttpRequest request = request(uri("/api/contexts", "owner", owner, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Creates a context.
	 * @param name The name of the context.
	 * @param entityTypeDisambiguation Whether entity type disambiguation is enabled. {@code null} omits
	 * the parameter, which Philter reads as {@code false} rather than as "leave unchanged".
	 * @param ledger Whether the redaction ledger is enabled. {@code null} omits the parameter, which
	 * Philter reads as {@code false} rather than as "leave unchanged".
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse createContext(String name, Boolean entityTypeDisambiguation, Boolean ledger) throws IOException {
		return createContext(name, entityTypeDisambiguation, ledger, null);
	}

	/**
	 * Creates a context.
	 * @param name The name of the context.
	 * @param entityTypeDisambiguation Whether entity type disambiguation is enabled. {@code null} omits
	 * the parameter, which Philter reads as {@code false} rather than as "leave unchanged".
	 * @param ledger Whether the redaction ledger is enabled. {@code null} omits the parameter, which
	 * Philter reads as {@code false} rather than as "leave unchanged".
	 * @param owner The owner of the context. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse createContext(String name, Boolean entityTypeDisambiguation, Boolean ledger, String owner)
			throws IOException {

		final HttpRequest request = request(uri("/api/contexts", "name", name,
				"entity_type_disambiguation", entityTypeDisambiguation, "ledger", ledger, "owner", owner))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	/**
	 * Gets a context by name.
	 * @param name The name of the context.
	 * @return The context.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getContext(String name) throws IOException {
		return getContext(name, null);
	}

	/**
	 * Gets a context by name.
	 * @param name The name of the context.
	 * @param owner The owner of the context. May be {@code null}.
	 * @return The context.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getContext(String name, String owner) throws IOException {
		return sendExpectingString(request(uri("/api/contexts/" + encode(name), "owner", owner)).GET().build());
	}

	/**
	 * Updates a context.
	 * @param name The name of the context.
	 * @param entityTypeDisambiguation Whether entity type disambiguation is enabled. {@code null} omits
	 * the parameter, which Philter reads as {@code false} rather than as "leave unchanged".
	 * @param ledger Whether the redaction ledger is enabled. {@code null} omits the parameter, which
	 * Philter reads as {@code false} rather than as "leave unchanged".
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse updateContext(String name, Boolean entityTypeDisambiguation, Boolean ledger) throws IOException {
		return updateContext(name, entityTypeDisambiguation, ledger, null);
	}

	/**
	 * Updates a context.
	 * @param name The name of the context.
	 * @param entityTypeDisambiguation Whether entity type disambiguation is enabled. {@code null} omits
	 * the parameter, which Philter reads as {@code false} rather than as "leave unchanged".
	 * @param ledger Whether the redaction ledger is enabled. {@code null} omits the parameter, which
	 * Philter reads as {@code false} rather than as "leave unchanged".
	 * @param owner The owner of the context. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse updateContext(String name, Boolean entityTypeDisambiguation, Boolean ledger, String owner)
			throws IOException {

		final HttpRequest request = request(uri("/api/contexts/" + encode(name),
				"entity_type_disambiguation", entityTypeDisambiguation, "ledger", ledger, "owner", owner))
				.PUT(HttpRequest.BodyPublishers.noBody())
				.build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	/**
	 * Deletes a context.
	 * @param name The name of the context.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteContext(String name) throws IOException {
		return deleteContext(name, null);
	}

	/**
	 * Deletes a context.
	 * @param name The name of the context.
	 * @param owner The owner of the context. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteContext(String name, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/contexts/" + encode(name), "owner", owner)).DELETE().build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	/**
	 * Gets the entries for a context.
	 * @param name The name of the context.
	 * @return The context entries.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getContextEntries(String name) throws IOException {
		return getContextEntries(name, null, null, null);
	}

	/**
	 * Gets the entries for a context.
	 * @param name The name of the context.
	 * @param owner The owner of the context. May be {@code null}.
	 * @param offset The pagination offset. May be {@code null}.
	 * @param limit The pagination limit. May be {@code null}.
	 * @return The context entries.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getContextEntries(String name, String owner, Integer offset, Integer limit) throws IOException {

		final HttpRequest request = request(uri("/api/contexts/" + encode(name) + "/entries",
				"owner", owner, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Deletes all entries for a context.
	 * @param name The name of the context.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteContextEntries(String name) throws IOException {
		return deleteContextEntries(name, null);
	}

	/**
	 * Deletes all entries for a context.
	 * @param name The name of the context.
	 * @param owner The owner of the context. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteContextEntries(String name, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/contexts/" + encode(name) + "/entries", "owner", owner))
				.DELETE()
				.build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	/**
	 * Exports the entries for a context.
	 * @param name The name of the context.
	 * @param owner The owner. May be {@code null}.
	 * @return The exported entries.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String exportContextEntries(String name, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/contexts/" + encode(name) + "/entries/export", "owner", owner))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Imports entries into a context.
	 * @param name The name of the context.
	 * @param onConflict The conflict resolution strategy. May be {@code null} to use the server default.
	 * @param owner The owner. May be {@code null}.
	 * @param json The entries to import as JSON.
	 * @return The import result.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String importContextEntries(String name, String onConflict, String owner, String json) throws IOException {

		final HttpRequest request = request(uri("/api/contexts/" + encode(name) + "/entries/import",
				"on_conflict", onConflict, "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(json))
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Deletes a single entry from a context.
	 * @param name The name of the context.
	 * @param entryId The entry ID.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteContextEntry(String name, String entryId) throws IOException {
		return deleteContextEntry(name, entryId, null);
	}

	/**
	 * Deletes a single entry from a context.
	 * @param name The name of the context.
	 * @param entryId The entry ID.
	 * @param owner The owner of the context. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteContextEntry(String name, String entryId, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/contexts/" + encode(name) + "/entries/" + encode(entryId),
				"owner", owner))
				.DELETE()
				.build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	// Documents.

	/**
	 * Gets the stored documents.
	 * @return The documents.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getDocuments() throws IOException {
		return getDocuments(null, null, null);
	}

	/**
	 * Gets the stored documents.
	 * @param owner The owner of the documents. May be {@code null}.
	 * @param offset The pagination offset. May be {@code null}.
	 * @param limit The pagination limit. May be {@code null}.
	 * @return The documents.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getDocuments(String owner, Integer offset, Integer limit) throws IOException {

		final HttpRequest request = json(uri("/api/documents", "owner", owner, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Gets a stored document by ID.
	 * @param documentId The document ID.
	 * @return The document bytes.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public byte[] getDocument(String documentId) throws IOException {
		return getDocument(documentId, null);
	}

	/**
	 * Gets a stored document by ID.
	 * @param documentId The document ID.
	 * @param owner The owner of the document. May be {@code null}.
	 * @return The document bytes.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public byte[] getDocument(String documentId, String owner) throws IOException {
		return sendExpectingBytes(request(uri("/api/documents/" + encode(documentId), "owner", owner)).GET().build());
	}

	/**
	 * Deletes a stored document.
	 * @param documentId The document ID.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteDocument(String documentId) throws IOException {
		deleteDocument(documentId, null);
	}

	/**
	 * Deletes a stored document.
	 * @param documentId The document ID.
	 * @param owner The owner of the document. May be {@code null}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteDocument(String documentId, String owner) throws IOException {
		sendExpectingNoContent(request(uri("/api/documents/" + encode(documentId), "owner", owner)).DELETE().build());
	}

	/**
	 * Gets the processing status of a document.
	 * @param documentId The document ID.
	 * @return The document status.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getDocumentStatus(String documentId) throws IOException {
		return getDocumentStatus(documentId, null);
	}

	/**
	 * Gets the processing status of a document.
	 * @param documentId The document ID.
	 * @param owner The owner of the document. May be {@code null}.
	 * @return The document status.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getDocumentStatus(String documentId, String owner) throws IOException {

		final HttpRequest request = json(uri("/api/documents/" + encode(documentId) + "/status", "owner", owner))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	// Legal holds.

	/**
	 * Gets the legal holds.
	 * @return A list of {@link LegalHoldResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<LegalHoldResponse> getHolds() throws IOException {
		return getHolds(null, null, null);
	}

	/**
	 * Gets the legal holds.
	 * @param owner The owner of the holds. May be {@code null}.
	 * @param offset The pagination offset. May be {@code null}.
	 * @param limit The pagination limit. May be {@code null}.
	 * @return A list of {@link LegalHoldResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<LegalHoldResponse> getHolds(String owner, Integer offset, Integer limit) throws IOException {

		final HttpRequest request = json(uri("/api/holds", "owner", owner, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingJson(request, LEGAL_HOLD_LIST);

	}

	/**
	 * Creates a legal hold.
	 * @param request The {@link LegalHoldRequest}.
	 * @return The created {@link LegalHoldResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LegalHoldResponse createHold(LegalHoldRequest request) throws IOException {
		return createHold(request, null);
	}

	/**
	 * Creates a legal hold.
	 * @param request The {@link LegalHoldRequest}.
	 * @param owner The owner of the hold. May be {@code null}.
	 * @return The created {@link LegalHoldResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LegalHoldResponse createHold(LegalHoldRequest request, String owner) throws IOException {

		final HttpRequest httpRequest = json(uri("/api/holds", "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(gson.toJson(request)))
				.build();

		return sendExpectingJson(httpRequest, LegalHoldResponse.class);

	}

	/**
	 * Gets a legal hold by reference.
	 * @param reference The legal hold reference.
	 * @return The {@link LegalHoldResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LegalHoldResponse getHold(String reference) throws IOException {
		return getHold(reference, null);
	}

	/**
	 * Gets a legal hold by reference.
	 * @param reference The legal hold reference.
	 * @param owner The owner of the hold. May be {@code null}.
	 * @return The {@link LegalHoldResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LegalHoldResponse getHold(String reference, String owner) throws IOException {

		final HttpRequest request = json(uri("/api/holds/" + encode(reference), "owner", owner)).GET().build();

		return sendExpectingJson(request, LegalHoldResponse.class);

	}

	/**
	 * Deletes a legal hold.
	 * @param reference The legal hold reference.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteHold(String reference) throws IOException {
		deleteHold(reference, null);
	}

	/**
	 * Deletes a legal hold.
	 * @param reference The legal hold reference.
	 * @param owner The owner of the hold. May be {@code null}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteHold(String reference, String owner) throws IOException {
		sendExpectingNoContent(request(uri("/api/holds/" + encode(reference), "owner", owner)).DELETE().build());
	}

	// Redaction ledger.

	/**
	 * Queries the redaction ledger.
	 * @param query The query. May be {@code null}.
	 * @return The matching ledger entries.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getLedger(String query) throws IOException {
		return getLedger(query, null, null, null);
	}

	/**
	 * Queries the redaction ledger.
	 * @param query The query. May be {@code null}.
	 * @param owner The owner of the entries. May be {@code null}.
	 * @param offset The pagination offset. May be {@code null}.
	 * @param limit The pagination limit. May be {@code null}.
	 * @return The matching ledger entries.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getLedger(String query, String owner, Integer offset, Integer limit) throws IOException {

		final HttpRequest request = request(uri("/api/ledger", "q", query, "owner", owner,
				"offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Gets the ledger entry for a document.
	 * @param documentId The document ID.
	 * @return The ledger entry.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getLedgerEntry(String documentId) throws IOException {
		return getLedgerEntry(documentId, null);
	}

	/**
	 * Gets the ledger entry for a document.
	 * @param documentId The document ID.
	 * @param owner The owner of the entry. May be {@code null}.
	 * @return The ledger entry.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getLedgerEntry(String documentId, String owner) throws IOException {
		return sendExpectingString(request(uri("/api/ledger/" + encode(documentId), "owner", owner)).GET().build());
	}

	/**
	 * Exports the ledger entry for a document.
	 * @param documentId The document ID.
	 * @return The exported ledger entry.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String exportLedger(String documentId) throws IOException {
		return exportLedger(documentId, null);
	}

	/**
	 * Exports the ledger entry for a document.
	 * @param documentId The document ID.
	 * @param owner The owner of the entry. May be {@code null}.
	 * @return The exported ledger entry.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String exportLedger(String documentId, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/ledger/" + encode(documentId) + "/export", "owner", owner))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Checks whether the ledger for a document is valid.
	 * @param documentId The document ID.
	 * @return The validity result.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String isLedgerValid(String documentId) throws IOException {
		return isLedgerValid(documentId, null);
	}

	/**
	 * Checks whether the ledger for a document is valid.
	 * @param documentId The document ID.
	 * @param owner The owner of the entry. May be {@code null}.
	 * @return The validity result.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String isLedgerValid(String documentId, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/ledger/" + encode(documentId) + "/valid", "owner", owner))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Deletes a document's ledger chain. Philter restricts this to administrators and to deployments
	 * that set {@code LEDGER_DELETION_ENABLED=true}.
	 * @param documentId The document ID.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteLedgerEntry(String documentId) throws IOException {
		return deleteLedgerEntry(documentId, null);
	}

	/**
	 * Deletes a document's ledger chain. Philter restricts this to administrators and to deployments
	 * that set {@code LEDGER_DELETION_ENABLED=true}.
	 * @param documentId The document ID.
	 * @param owner The owner of the entry. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteLedgerEntry(String documentId, String owner) throws IOException {

		final HttpRequest request = json(uri("/api/ledger/" + encode(documentId), "owner", owner)).DELETE().build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	/**
	 * Purges completed ledger chains older than the given number of days. Philter restricts this to
	 * administrators and to deployments that set {@code LEDGER_DELETION_ENABLED=true}.
	 * @param olderThanDays The age in days beyond which chains are purged. Must be zero or greater.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse purgeLedger(int olderThanDays) throws IOException {
		return purgeLedger(olderThanDays, null);
	}

	/**
	 * Purges completed ledger chains older than the given number of days. Philter restricts this to
	 * administrators and to deployments that set {@code LEDGER_DELETION_ENABLED=true}.
	 * @param olderThanDays The age in days beyond which chains are purged. Must be zero or greater.
	 * @param owner The owner of the entries. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse purgeLedger(int olderThanDays, String owner) throws IOException {

		final HttpRequest request = json(uri("/api/ledger", "older_than_days", olderThanDays, "owner", owner))
				.DELETE()
				.build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	// Custom lists.

	/**
	 * Gets the custom lists.
	 * @return The custom lists.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getLists() throws IOException {
		return getLists(null);
	}

	/**
	 * Gets the custom lists.
	 * @param owner The owner of the lists. May be {@code null}.
	 * @return The custom lists.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getLists(String owner) throws IOException {
		return sendExpectingString(request(uri("/api/lists", "owner", owner)).GET().build());
	}

	/**
	 * Saves (or overwrites) a custom list.
	 * @param list The name of the list.
	 * @param description The description of the list. May be {@code null}.
	 * @param values The values in the list.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse saveList(String list, String description, List<String> values) throws IOException {
		return saveList(list, description, values, null);
	}

	/**
	 * Saves (or overwrites) a custom list.
	 * @param list The name of the list.
	 * @param description The description of the list. May be {@code null}.
	 * @param values The values in the list.
	 * @param owner The owner of the list. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse saveList(String list, String description, List<String> values, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/lists/" + encode(list), "description", description, "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(gson.toJson(values)))
				.build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	/**
	 * Deletes a custom list.
	 * @param list The name of the list.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteList(String list) throws IOException {
		deleteList(list, null);
	}

	/**
	 * Deletes a custom list.
	 * @param list The name of the list.
	 * @param owner The owner of the list. May be {@code null}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteList(String list, String owner) throws IOException {
		sendExpectingNoContent(request(uri("/api/lists/" + encode(list), "owner", owner)).DELETE().build());
	}

	/**
	 * Gets the values of a custom list.
	 * @param name The name of the list.
	 * @return The {@link GetListsResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetListsResponse getList(String name) throws IOException {
		return getList(name, null);
	}

	/**
	 * Gets the values of a custom list.
	 * @param name The name of the list.
	 * @param owner The owner of the list. May be {@code null}.
	 * @return The {@link GetListsResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetListsResponse getList(String name, String owner) throws IOException {

		final HttpRequest request = json(uri("/api/lists/" + encode(name), "owner", owner)).GET().build();

		return sendExpectingJson(request, GetListsResponse.class);

	}

	// Redact lists.

	/**
	 * Gets the redact lists.
	 * @return The redact lists.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getRedactLists() throws IOException {
		return getRedactLists(null);
	}

	/**
	 * Gets the redact lists.
	 * @param owner The owner of the redact lists. May be {@code null}.
	 * @return The redact lists.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getRedactLists(String owner) throws IOException {
		return sendExpectingString(json(uri("/api/redact-lists", "owner", owner)).GET().build());
	}

	/**
	 * Creates a redact list.
	 * @param json The redact list as JSON.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse createRedactList(String json) throws IOException {
		return createRedactList(json, null);
	}

	/**
	 * Creates a redact list.
	 * @param json The redact list as JSON.
	 * @param owner The owner of the redact list. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse createRedactList(String json, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/redact-lists", "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(json))
				.build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	/**
	 * Updates a redact list.
	 * @param json The redact list as JSON.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse updateRedactList(String json) throws IOException {
		return updateRedactList(json, null);
	}

	/**
	 * Updates a redact list.
	 * @param json The redact list as JSON.
	 * @param owner The owner of the redact list. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse updateRedactList(String json, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/redact-lists", "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.PUT(text(json))
				.build();

		return sendExpectingJson(request, GenericResponse.class);

	}

}
