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

import ai.philterd.philter.model.AdminSettings;
import ai.philterd.philter.model.SigningKey;
import ai.philterd.philter.model.RedactListsRequest;
import ai.philterd.philter.model.RedactLists;
import ai.philterd.philter.model.LedgerExport;
import ai.philterd.philter.model.LedgerChain;
import ai.philterd.philter.model.GetLedgerResponse;
import ai.philterd.philter.model.GetDocumentsResponse;
import ai.philterd.philter.model.GetContextsResponse;
import ai.philterd.philter.model.GetContextsAcrossUsersResponse;
import ai.philterd.philter.model.GetContextEntriesResponse;
import ai.philterd.philter.model.DocumentStatus;
import ai.philterd.philter.model.CustomListSummary;
import ai.philterd.philter.model.ContextDetails;
import ai.philterd.philter.model.SignInResponse;
import ai.philterd.philter.model.SignInRequest;
import ai.philterd.philter.model.SignInMfaRequest;
import ai.philterd.philter.model.SetPasswordRequest;
import ai.philterd.philter.model.RevokedSessionKeysResponse;
import ai.philterd.philter.model.ManagedPolicySummary;
import ai.philterd.philter.model.MfaEnrollment;
import ai.philterd.philter.model.MfaCodeRequest;
import ai.philterd.philter.model.ChangePasswordRequest;
import ai.philterd.philter.model.ApiKey;
import ai.philterd.philter.model.AsyncFilterResponse;
import ai.philterd.philter.model.AuditLogExport;
import ai.philterd.philter.model.BinaryFilterResponse;
import ai.philterd.philter.model.CreateApiKeyRequest;
import ai.philterd.philter.model.CreateUserRequest;
import ai.philterd.philter.model.CreatedApiKeyResponse;
import ai.philterd.philter.model.CreatedUserResponse;
import ai.philterd.philter.model.CurrentUser;
import ai.philterd.philter.model.ExplainResponse;
import ai.philterd.philter.model.FilterResponse;
import ai.philterd.philter.model.GenericResponse;
import ai.philterd.philter.model.GetApiKeyScopesResponse;
import ai.philterd.philter.model.GetApiKeysResponse;
import ai.philterd.philter.model.GetAuditLogResponse;
import ai.philterd.philter.model.GetListsResponse;
import ai.philterd.philter.model.GetUsersResponse;
import ai.philterd.philter.model.LegalHoldRequest;
import ai.philterd.philter.model.LegalHoldResponse;
import ai.philterd.philter.model.OwnedLegalHoldResponse;
import ai.philterd.philter.model.OwnedName;
import ai.philterd.philter.model.PolicyDetails;
import ai.philterd.philter.model.PolicyRollbackResponse;
import ai.philterd.philter.model.PolicyVersionSummary;
import ai.philterd.philter.model.ReidentifyRequest;
import ai.philterd.philter.model.SetApiKeyScopesRequest;
import ai.philterd.philter.model.SetPolicyDetailsRequest;
import ai.philterd.philter.model.SetUserRoleRequest;
import ai.philterd.philter.model.SetWebhookRequest;
import ai.philterd.philter.model.StatusResponse;
import ai.philterd.philter.model.UpdateAdminSettingsRequest;
import ai.philterd.philter.model.User;
import ai.philterd.philter.model.Webhook;
import ai.philterd.philter.model.exceptions.ClientException;
import ai.philterd.philter.model.exceptions.ServiceUnavailableException;
import ai.philterd.philter.model.exceptions.SignInLockedException;
import ai.philterd.philter.model.exceptions.SignInRateLimitedException;
import ai.philterd.philter.model.exceptions.UnauthorizedException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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

	private static final String SIGNATURE_HEADER = "X-Philter-Signature";

	private static final String EXPORT_ROWS_HEADER = "X-Philter-Export-Rows";
	private static final String EXPORT_TRUNCATED_HEADER = "X-Philter-Export-Truncated";
	private static final String EXPORT_NEXT_OFFSET_HEADER = "X-Philter-Export-Next-Offset";
	private static final String EXPORT_TIME_ZONE_HEADER = "X-Philter-Export-Time-Zone";

	private static final String APPLICATION_JSON = "application/json";
	private static final String TEXT_PLAIN = "text/plain";

	private static final Type STRING_LIST = new TypeToken<List<String>>() {}.getType();
	private static final Type POLICY_VERSION_LIST = new TypeToken<List<PolicyVersionSummary>>() {}.getType();
	private static final Type LEGAL_HOLD_LIST = new TypeToken<List<LegalHoldResponse>>() {}.getType();
	private static final Type OWNED_LEGAL_HOLD_LIST = new TypeToken<List<OwnedLegalHoldResponse>>() {}.getType();
	private static final Type OWNED_NAME_LIST = new TypeToken<List<OwnedName>>() {}.getType();
	private static final Type CUSTOM_LIST_SUMMARY_LIST = new TypeToken<List<CustomListSummary>>() {}.getType();
	private static final Type MANAGED_POLICY_SUMMARY_LIST = new TypeToken<List<ManagedPolicySummary>>() {}.getType();

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
	 * Whether a context name, custom list name, or legal hold reference can be used in a request path.
	 * The same rule as Philter's: Tomcat or Spring refuses a path containing any of these, even
	 * percent-encoded, before Philter sees it. Philter refuses such names when they are created, but
	 * older items can still have one, so the delete methods send it in the query instead.
	 */
	private static boolean isPathSafe(final String name) {
		if (name == null) {
			return true;
		}
		if (".".equals(name) || "..".equals(name)) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			final char c = name.charAt(i);
			if (c == '/' || c == '\\' || c == ';' || c == '%' || Character.isISOControl(c)) {
				return false;
			}
		}
		return true;
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
			final String message = messageOf(body);
			return new UnauthorizedException(message == null ? UNAUTHORIZED : message);
		} else if(code == 503) {
			return new ServiceUnavailableException(SERVICE_UNAVAILABLE);
		} else {
			return new ClientException(describe(code, body), code, messageOf(body), fieldOf(body, "reason"));
		}

	}

	/**
	 * The {@code message} field of a JSON error body, or {@code null} when the body is not JSON or has
	 * none.
	 */
	private static String messageOf(final String body) {
		return fieldOf(body, "message");
	}

	/**
	 * A string field of a JSON error body, such as {@code message} or {@code reason}, or {@code null}
	 * when the body is empty, is not a JSON object, or has no such string field.
	 */
	private static String fieldOf(final String body, final String field) {

		if(body == null || body.isBlank()) {
			return null;
		}

		try {
			final JsonElement element = JsonParser.parseString(body);
			if(element.isJsonObject() && element.getAsJsonObject().has(field)
					&& element.getAsJsonObject().get(field).isJsonPrimitive()) {
				return element.getAsJsonObject().get(field).getAsString();
			}
		} catch (final JsonParseException ex) {
			// Not JSON, so there is no field to carry.
		}

		return null;

	}

	/**
	 * Maps a failed sign-in response to a client exception. Philter refuses both a locked username and an
	 * address over the rate limit with an HTTP 429, and its {@code reason} field says which.
	 */
	private static RuntimeException toSignInException(final HttpResponse<String> response) {

		if(response.statusCode() == 429) {

			final String message = messageOf(response.body());
			final Integer retryAfter = response.headers().firstValue("Retry-After").map(value -> {
				try {
					return Integer.valueOf(value.trim());
				} catch (final NumberFormatException ex) {
					return null;
				}
			}).orElse(null);

			final String reason = fieldOf(response.body(), "reason");

			if("locked".equals(reason)) {
				return new SignInLockedException(message, retryAfter);
			} else if("rate_limited".equals(reason)) {
				return new SignInRateLimitedException(message, retryAfter);
			}

		}

		return toException(response.statusCode(), response.body());

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
		return filter(context, policyName, filename, text, false);
	}

	/**
	 * Send text to Philter to be filtered, optionally asking for a signed response.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the text.
	 * @param filename The name of the file the text came from, recorded against the document. May be {@code null}.
	 * @param text The text to be filtered.
	 * @param sign {@code true} to ask Philter to sign the response, adding an ES256 JWT in the
	 * {@code X-Philter-Signature} header. A deployment with output signing enabled signs every response
	 * whatever this says; {@code false} cannot turn that off. Read the signature from
	 * {@code getSignature()} and verify it with the public key from {@link #getSigningKey(String)}, using
	 * the JWT's {@code kid}. The client does not verify signatures itself.
	 * @return The filtered text, and the signature when the response is signed.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public FilterResponse filter(String context, String policyName, String filename, String text, boolean sign)
			throws IOException {

		// Philter's text endpoint is always synchronous, so the filtered text comes back in the response body.
		// sign is sent only when true: false asks for nothing, so leaving it out keeps the request unchanged.
		final HttpRequest request = request(uri("/api/filter", "c", context, "p", policyName, "filename", filename,
				"sign", sign ? Boolean.TRUE : null))
				.header("Accept", TEXT_PLAIN)
				.header("Content-Type", TEXT_PLAIN)
				.POST(text(text))
				.build();

		final HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());

		if(isSuccessful(response)) {

			final String documentId = response.headers().firstValue(DOCUMENT_ID_HEADER).orElse(null);
			final String signature = response.headers().firstValue(SIGNATURE_HEADER).orElse(null);
			return new FilterResponse(response.body(), context, documentId, signature);

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
		return filter(context, policyName, filename, Files.readAllBytes(file.toPath()));
	}

	/**
	 * Send a PDF document to Philter to be filtered, waiting for the filtered document.
	 *
	 * <p>Takes the PDF in memory, so a caller holding an upload does not write an unredacted copy to
	 * disk first. Sends the same request as the {@link File} overload.</p>
	 *
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the document.
	 * @param filename The name of the file being filtered, recorded against the document. May be {@code null}.
	 * @param content The PDF.
	 * @return The filtered document as a ZIP archive.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public BinaryFilterResponse filter(String context, String policyName, String filename, byte[] content) throws IOException {
		return filterBinary(context, policyName, filename, content, "application/zip");
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
		return filterToPdf(context, policyName, filename, Files.readAllBytes(file.toPath()));
	}

	/**
	 * Send a PDF document to Philter to be filtered, waiting for the filtered document and receiving it
	 * as a PDF rather than as a ZIP archive.
	 *
	 * <p>Takes the PDF in memory, so a caller holding an upload does not write an unredacted copy to
	 * disk first. Sends the same request as the {@link File} overload.</p>
	 *
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the document.
	 * @param filename The name of the file being filtered, recorded against the document. May be {@code null}.
	 * @param content The PDF.
	 * @return The filtered document as a PDF.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public BinaryFilterResponse filterToPdf(String context, String policyName, String filename, byte[] content) throws IOException {
		return filterBinary(context, policyName, filename, content, "application/pdf");
	}

	private BinaryFilterResponse filterBinary(String context, String policyName, String filename, byte[] content,
	                                          String accept) throws IOException {

		final HttpRequest request = binaryFilterRequest(context, policyName, filename, content, accept, false);

		final HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray());

		if(isSuccessful(response)) {

			final String documentId = response.headers().firstValue(DOCUMENT_ID_HEADER).orElse(null);
			return new BinaryFilterResponse(context, documentId, response.body());

		}

		throw toException(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));

	}

	/**
	 * Submits a PDF document to Philter to be filtered asynchronously. Philter accepts the document and
	 * returns immediately; poll {@link #getDocumentState(String)} with the returned document ID and
	 * retrieve the result with {@link #getDocument(String)}.
	 * If the user has a webhook, Philter also notifies it when the redaction completes or fails; see
	 * {@link #setWebhook(String, String)}.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the document.
	 * @param filename The name of the file being filtered. May be {@code null}.
	 * @param file The PDF file to be filtered.
	 * @return The ID Philter assigned to the document.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String filterAsync(String context, String policyName, String filename, File file) throws IOException {
		return filterAsync(context, policyName, filename, Files.readAllBytes(file.toPath()));
	}

	/**
	 * Submits a PDF document to Philter to be filtered asynchronously. Philter accepts the document and
	 * returns immediately; poll {@link #getDocumentState(String)} with the returned document ID and
	 * retrieve the result with {@link #getDocument(String)}.
	 * If the user has a webhook, Philter also notifies it when the redaction completes or fails; see
	 * {@link #setWebhook(String, String)}.
	 *
	 * <p>Takes the PDF in memory, so a caller holding an upload does not write an unredacted copy to
	 * disk first. Sends the same request as the {@link File} overload.</p>
	 *
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the document.
	 * @param filename The name of the file being filtered, recorded against the document. May be {@code null}.
	 * @param content The PDF.
	 * @return The ID Philter assigned to the document.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String filterAsync(String context, String policyName, String filename, byte[] content) throws IOException {
		return filterBinaryAsync(context, policyName, filename, content, "application/zip");
	}

	/**
	 * Submits a PDF document to Philter to be filtered asynchronously, with the result stored as a PDF
	 * rather than as a ZIP archive. Philter accepts the document and returns immediately; poll
	 * {@link #getDocumentState(String)} with the returned document ID and retrieve the result with
	 * {@link #getDocument(String)}.
	 * If the user has a webhook, Philter also notifies it when the redaction completes or fails; see
	 * {@link #setWebhook(String, String)}.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the document.
	 * @param filename The name of the file being filtered. May be {@code null}.
	 * @param file The PDF file to be filtered.
	 * @return The ID Philter assigned to the document.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String filterToPdfAsync(String context, String policyName, String filename, File file) throws IOException {
		return filterToPdfAsync(context, policyName, filename, Files.readAllBytes(file.toPath()));
	}

	/**
	 * Submits a PDF document to Philter to be filtered asynchronously, with the result stored as a PDF
	 * rather than as a ZIP archive. Philter accepts the document and returns immediately; poll
	 * {@link #getDocumentState(String)} with the returned document ID and retrieve the result with
	 * {@link #getDocument(String)}.
	 * If the user has a webhook, Philter also notifies it when the redaction completes or fails; see
	 * {@link #setWebhook(String, String)}.
	 *
	 * <p>Takes the PDF in memory, so a caller holding an upload does not write an unredacted copy to
	 * disk first. Sends the same request as the {@link File} overload.</p>
	 *
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the document.
	 * @param filename The name of the file being filtered, recorded against the document. May be {@code null}.
	 * @param content The PDF.
	 * @return The ID Philter assigned to the document.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String filterToPdfAsync(String context, String policyName, String filename, byte[] content) throws IOException {
		return filterBinaryAsync(context, policyName, filename, content, "application/pdf");
	}

	private String filterBinaryAsync(String context, String policyName, String filename, byte[] content,
	                                 String accept) throws IOException {

		final HttpRequest request = binaryFilterRequest(context, policyName, filename, content, accept, true);

		// Philter answers an accepted submission with 202 and a JSON body carrying the document ID.
		final AsyncFilterResponse response = sendExpectingJson(request, AsyncFilterResponse.class);

		return response == null ? null : response.getDocumentId();

	}

	private HttpRequest binaryFilterRequest(String context, String policyName, String filename, byte[] content,
	                                        String accept, boolean async) {

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
		return explain(context, policyName, filename, text, false);
	}

	/**
	 * Send text to Philter to be filtered and explained, optionally asking for a signed response.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the text.
	 * @param filename The name of the file the text came from, recorded against the document. May be {@code null}.
	 * @param text The text to be filtered.
	 * @param sign {@code true} to ask Philter to sign the response, adding an ES256 JWT in the
	 * {@code X-Philter-Signature} header. A deployment with output signing enabled signs every response
	 * whatever this says; {@code false} cannot turn that off. Read the signature from
	 * {@code getSignature()} and verify it with the public key from {@link #getSigningKey(String)}, using
	 * the JWT's {@code kid}. The client does not verify signatures itself. A signature's {@code bodyHash} covers
	 * the response body exactly as sent, available from {@link ExplainResponse#getResponseBody()}.
	 * @return The explanation, and the signature when the response is signed.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public ExplainResponse explain(String context, String policyName, String filename, String text, boolean sign)
			throws IOException {

		final HttpRequest request = request(uri("/api/explain", "c", context, "p", policyName, "filename", filename,
				"sign", sign ? Boolean.TRUE : null))
				.header("Accept", APPLICATION_JSON)
				.header("Content-Type", TEXT_PLAIN)
				.POST(text(text))
				.build();

		final HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());

		if(!isSuccessful(response)) {
			throw toException(response.statusCode(), response.body());
		}

		final ExplainResponse explainResponse = gson.fromJson(response.body(), ExplainResponse.class);

		// An empty body parses to null, which is returned as before rather than dereferenced.
		if(explainResponse != null) {
			explainResponse.setResponseBody(response.body());
			explainResponse.setSignature(response.headers().firstValue(SIGNATURE_HEADER).orElse(null));
		}

		return explainResponse;

	}

	/**
	 * Compiles PhiSQL source into a native policy. Nothing is saved: pass the returned {@code policy}
	 * to {@link #savePolicy(String, String)} to store it.
	 *
	 * <p>Requires the {@code policies:read} scope. Does not require an administrator.</p>
	 *
	 * <p>The compiled policy is validated before it is returned. Source that fails to parse or compile,
	 * or a compiled policy that fails validation, is an HTTP 400, thrown as a {@link ClientException}
	 * carrying the compiler's message.</p>
	 *
	 * @param phiSql The PhiSQL source, for example {@code POLICY ssn_only; REDACT SSN WITH MASK;}.
	 * @return JSON with the compiled {@code policy}, the {@code name} from the source's {@code POLICY}
	 * declaration ({@code null} when it has none), and the {@code description} when the source declares
	 * one.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String compilePolicy(String phiSql) throws IOException {

		final HttpRequest request = request(uri("/api/policies/compile"))
				.header("Accept", APPLICATION_JSON)
				.header("Content-Type", TEXT_PLAIN)
				.POST(text(phiSql))
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
	 * @deprecated Use {@link #getSigningKeyDetails(String)}, which returns a typed model.
	 */
	@Deprecated
	public String getSigningKey() throws IOException {
		return sendExpectingString(json(uri("/api/signing-key")).GET().build());
	}

	/**
	 * Gets a retained public signing key by its ID. Like {@link #getSigningKey()}, this endpoint does
	 * not require authentication.
	 * @param keyId The ID of the signing key.
	 * @return The signing key.
	 * @throws IOException Thrown if the request can not be completed.
	 * @deprecated Use {@link #getSigningKeyDetails(String)}, which returns a typed model.
	 */
	@Deprecated
	public String getSigningKey(String keyId) throws IOException {
		return sendExpectingString(json(uri("/api/signing-key/" + encode(keyId))).GET().build());
	}

	/**
	 * Gets the active public signing key, with its JWK and fingerprint. Requires no API key.
	 * @return The active key.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public SigningKey getSigningKeyDetails() throws IOException {
		return gson.fromJson(getSigningKey(), SigningKey.class);
	}

	/**
	 * Gets a retained public signing key by ID, active or superseded, to verify output it signed.
	 * Requires no API key. An ID Philter does not retain is an HTTP 404, thrown as a
	 * {@link ClientException}.
	 * @param keyId The key ID, such as a signature's {@code kid}.
	 * @return The key, with whether it is the active one.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public SigningKey getSigningKeyDetails(String keyId) throws IOException {
		return gson.fromJson(getSigningKey(keyId), SigningKey.class);
	}


	/**
	 * Rotates the output signing key: Philter generates a new ES256 keypair and makes it the active
	 * signing key. The superseded key is retained and stays retrievable with
	 * {@link #getSigningKey(String)}, so signatures and ledger entries made with it remain verifiable.
	 * Verifiers should resolve each signature's key ID rather than caching one key.
	 *
	 * <p>Requires the {@code signing:write} scope and an administrator. A key without the scope is
	 * refused with an HTTP 403 whose message names the scope; a key that has it but does not belong to
	 * an administrator is refused with an HTTP 403 saying an administrator is required. Where the
	 * signing key is managed by {@code PHILTER_SIGNING_KEY_PATH}, Philter refuses with an HTTP 409:
	 * replace that file and restart every instance instead. Each is thrown as a
	 * {@link ClientException} carrying Philter's message.</p>
	 *
	 * <p>Each rotation is recorded in Philter's audit log as a {@code signing_key_regenerated} event.</p>
	 *
	 * @return The ID of the key that is now active.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String regenerateSigningKey() throws IOException {

		final HttpRequest request = json(uri("/api/signing-key/regenerate"))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		final JsonObject response = sendExpectingJson(request, JsonObject.class);

		return response == null || !response.has("keyId") ? null : response.get("keyId").getAsString();

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
	 * Gets the first page of every user's policies, each naming its owner. See
	 * {@link #getPoliciesAcrossUsers(Integer, Integer)}.
	 * @return Each policy's name and owner.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<OwnedName> getPoliciesAcrossUsers() throws IOException {
		return getPoliciesAcrossUsers(null, null);
	}

	/**
	 * Gets a page of every user's policies, each naming its owner.
	 *
	 * <p>Requires an administrator API key and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the
	 * Philter deployment (disabled by default). Otherwise Philter answers HTTP 404 in either case,
	 * thrown as a {@link ClientException}. Each call is recorded in Philter's audit log.</p>
	 *
	 * <p>Managed policies are not included.</p>
	 *
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return Each policy's name and owner.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<OwnedName> getPoliciesAcrossUsers(Integer offset, Integer limit) throws IOException {

		final HttpRequest request = json(uri("/api/policies", "all_users", true, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingJson(request, OWNED_NAME_LIST);

	}

	/**
	 * Gets the first page of the built-in managed policy names. Requires the {@code policies:read}
	 * scope. See {@link #getManagedPolicies(Integer, Integer)}.
	 * @return The managed policy names.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listManagedPolicies()}, which returns each policy's description too.
	 */
	@Deprecated
	public List<String> getManagedPolicies() throws IOException {
		return getManagedPolicies(null, null);
	}

	/**
	 * Gets a page of the built-in managed policy names. See {@link #listManagedPolicies(Integer, Integer)}.
	 * @param offset The number of names to skip. May be {@code null} for {@code 0}.
	 * @param limit The most names to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The managed policy names.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listManagedPolicies(Integer, Integer)}, which returns each policy's
	 * description too.
	 */
	@Deprecated
	public List<String> getManagedPolicies(Integer offset, Integer limit) throws IOException {

		final List<String> names = new ArrayList<>();
		for (final ManagedPolicySummary policy : listManagedPolicies(offset, limit)) {
			names.add(policy.getName());
		}
		return names;

	}

	/**
	 * Lists the first page of the built-in managed policies with their descriptions. Requires the
	 * {@code policies:read} scope. See {@link #listManagedPolicies(Integer, Integer)}.
	 * @return Each managed policy's name and description.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<ManagedPolicySummary> listManagedPolicies() throws IOException {
		return listManagedPolicies(null, null);
	}

	/**
	 * Lists a page of the built-in managed policies with their descriptions, ordered by name. Each name
	 * begins with {@code managed_}. Read one with {@link #getPolicy(String)} or
	 * {@link #getPolicyDetails(String)}, and create a policy of your own from one with
	 * {@link #copyPolicy(String, String)}. Managed policies cannot be changed.
	 *
	 * <p>Requires the {@code policies:read} scope. Does not require an administrator.</p>
	 *
	 * @param offset The number of policies to skip. May be {@code null} for {@code 0}.
	 * @param limit The most policies to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return Each managed policy's name and description. A policy with no description has an empty one.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<ManagedPolicySummary> listManagedPolicies(Integer offset, Integer limit) throws IOException {

		final HttpRequest request = json(uri("/api/policies", "managed", true, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingJson(request, MANAGED_POLICY_SUMMARY_LIST);

	}

	/**
	 * Gets the content of a policy. A name beginning with {@code managed_} gets that managed policy.
	 * The policy's description and notes are not part of its content; get them with
	 * {@link #getPolicyDetails(String)}.
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
	 * Creates a policy. It only creates: a name the owner already uses is a {@link ClientException}
	 * with status {@code 409} and {@link ClientException#getReason()} {@code policy_exists}. Replace an
	 * existing policy with {@link #replacePolicy(String, String)}.
	 * @param name The name of the policy.
	 * @param json The body of the policy.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void savePolicy(String name, String json) throws IOException {
		savePolicy(name, json, null);
	}

	/**
	 * Creates a policy. It only creates: a name the owner already uses is a {@link ClientException}
	 * with status {@code 409} and {@link ClientException#getReason()} {@code policy_exists}. Replace an
	 * existing policy with {@link #replacePolicy(String, String)}.
	 * @param name The name of the policy.
	 * @param json The body of the policy.
	 * @param owner The owner of the policy. May be {@code null}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void savePolicy(String name, String json, String owner) throws IOException {
		savePolicy(name, json, null, null, owner);
	}

	/**
	 * Creates a policy with a description and notes. Requires the {@code policies:write} scope. See
	 * {@link #savePolicy(String, String, String, String, String)}.
	 * @param name The name of the policy.
	 * @param json The body of the policy.
	 * @param description The description, up to 200 characters. May be {@code null}.
	 * @param notes The notes, up to 1000 characters. May be {@code null}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void savePolicy(String name, String json, String description, String notes) throws IOException {
		savePolicy(name, json, description, notes, null);
	}

	/**
	 * Creates a policy with a description and notes. It only creates: a name the owner already uses is
	 * a {@link ClientException} with status {@code 409} and {@link ClientException#getReason()}
	 * {@code policy_exists}, and nothing is changed. Replace an existing policy with
	 * {@link #replacePolicy(String, String, String, String, String)}.
	 *
	 * <p>Requires the {@code policies:write} scope. Does not require an administrator, except to save
	 * another user's policy with {@code owner}.</p>
	 *
	 * <p>Philter rejects a missing or invalid name (including one beginning with {@code managed_}) and
	 * an invalid policy with an HTTP 400, thrown as a {@link ClientException}.</p>
	 *
	 * <p>When {@code description} or {@code notes} is given, this is two requests: the policy is
	 * created, then they are set with {@link #setPolicyDetails(String, String, String, String)}. They
	 * are not atomic. If the second request fails, for example with an HTTP 400 for a description over
	 * 200 characters or notes over 1000, its {@link ClientException} is thrown but the policy has
	 * already been created, without the description or notes; call {@code setPolicyDetails} to set
	 * them.</p>
	 *
	 * @param name The name of the policy.
	 * @param json The body of the policy.
	 * @param description The description, up to 200 characters. May be {@code null}.
	 * @param notes The notes, up to 1000 characters. May be {@code null}.
	 * @param owner The owner of the policy. May be {@code null} for the caller's own. Another user's
	 * requires an administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the Philter
	 * deployment; otherwise, or for an owner that does not exist, Philter answers HTTP 404, thrown as a
	 * {@link ClientException}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void savePolicy(String name, String json, String description, String notes, String owner)
			throws IOException {

		final HttpRequest request = request(uri("/api/policies", "name", name, "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(json))
				.build();

		sendExpectingNoContent(request);

		// Philter takes the description and notes only through the details endpoint, in a JSON body.
		if (description != null || notes != null) {
			setPolicyDetails(name, description, notes, owner);
		}

	}

	/**
	 * Replaces an existing policy. Its description and notes are kept. Requires the
	 * {@code policies:write} scope. See {@link #replacePolicy(String, String, String, String, String)}.
	 * @param name The name of the policy.
	 * @param json The body of the policy.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void replacePolicy(String name, String json) throws IOException {
		replacePolicy(name, json, null, null, null);
	}

	/**
	 * Replaces another user's existing policy. Its description and notes are kept. See
	 * {@link #replacePolicy(String, String, String, String, String)}.
	 * @param name The name of the policy.
	 * @param json The body of the policy.
	 * @param owner The owner of the policy. May be {@code null} for the caller's own. Another user's
	 * requires an administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the Philter
	 * deployment; otherwise, or for an owner that does not exist, Philter answers HTTP 404, thrown as a
	 * {@link ClientException}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void replacePolicy(String name, String json, String owner) throws IOException {
		replacePolicy(name, json, null, null, owner);
	}

	/**
	 * Replaces an existing policy and sets its description and notes. Requires the
	 * {@code policies:write} scope. See {@link #replacePolicy(String, String, String, String, String)}.
	 * @param name The name of the policy.
	 * @param json The body of the policy.
	 * @param description The description, up to 200 characters. May be {@code null} to keep the
	 * current one.
	 * @param notes The notes, up to 1000 characters. May be {@code null} to keep the current notes.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void replacePolicy(String name, String json, String description, String notes) throws IOException {
		replacePolicy(name, json, description, notes, null);
	}

	/**
	 * Replaces an existing policy with a new revision. Use {@link #savePolicy(String, String)} to
	 * create one. A {@code null} description or notes keeps the current value.
	 *
	 * <p>Requires the {@code policies:write} scope. Does not require an administrator, except to
	 * replace another user's policy with {@code owner}.</p>
	 *
	 * <p>A policy that does not exist is an HTTP 404. A policy that changed concurrently is an HTTP 409
	 * whose {@link ClientException#getReason()} is {@code policy_changed}: reload it and retry. An
	 * invalid policy is an HTTP 400. Each is thrown as a {@link ClientException}.</p>
	 *
	 * <p>When {@code description} or {@code notes} is given, this is two requests: the policy is
	 * replaced, then they are set with {@link #setPolicyDetails(String, String, String, String)}. They
	 * are not atomic. If the second request fails, for example with an HTTP 400 for a description over
	 * 200 characters or notes over 1000, its {@link ClientException} is thrown but the new revision has
	 * already been stored, with the previous description and notes; call {@code setPolicyDetails} to
	 * set them.</p>
	 *
	 * @param name The name of the policy.
	 * @param json The body of the policy.
	 * @param description The description, up to 200 characters. May be {@code null} to keep the
	 * current one.
	 * @param notes The notes, up to 1000 characters. May be {@code null} to keep the current notes.
	 * @param owner The owner of the policy. May be {@code null} for the caller's own. Another user's
	 * requires an administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the Philter
	 * deployment; otherwise, or for an owner that does not exist, Philter answers HTTP 404, thrown as a
	 * {@link ClientException}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void replacePolicy(String name, String json, String description, String notes, String owner)
			throws IOException {

		final HttpRequest request = request(uri("/api/policies/" + encode(name), "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.PUT(text(json))
				.build();

		sendExpectingNoContent(request);

		if (description != null || notes != null) {
			setPolicyDetails(name, description, notes, owner);
		}

	}

	/**
	 * Gets a policy's details. Requires the {@code policies:read} scope. See
	 * {@link #getPolicyDetails(String, String)}.
	 * @param policyName The name of the policy.
	 * @return The policy's details.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public PolicyDetails getPolicyDetails(String policyName) throws IOException {
		return getPolicyDetails(policyName, null);
	}

	/**
	 * Gets a policy's details: its description, notes, revision, whether it is managed, and when it was
	 * created and last updated. Works for managed policies.
	 *
	 * <p>Requires the {@code policies:read} scope. Does not require an administrator, except to read
	 * another user's policy with {@code owner}.</p>
	 *
	 * <p>A policy that does not exist is an HTTP 404, thrown as a {@link ClientException}.</p>
	 *
	 * @param policyName The name of the policy.
	 * @param owner The owner of the policy. May be {@code null} for the caller's own. Another user's
	 * requires an administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the Philter
	 * deployment; otherwise, or for an owner that does not exist, Philter answers HTTP 404, thrown as a
	 * {@link ClientException}.
	 * @return The policy's details.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public PolicyDetails getPolicyDetails(String policyName, String owner) throws IOException {
		return sendExpectingJson(json(uri("/api/policies/" + encode(policyName) + "/details", "owner", owner))
				.GET().build(), PolicyDetails.class);
	}

	/**
	 * Sets a policy's description and notes. Requires the {@code policies:write} scope. See
	 * {@link #setPolicyDetails(String, String, String, String)}.
	 * @param policyName The name of the policy.
	 * @param description The description, up to 200 characters. {@code null} leaves it as it is, and
	 * an empty string clears it.
	 * @param notes The notes, up to 1000 characters. {@code null} leaves them as they are, and an empty
	 * string clears them.
	 * @return The policy's details after the change.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public PolicyDetails setPolicyDetails(String policyName, String description, String notes) throws IOException {
		return setPolicyDetails(policyName, description, notes, null);
	}

	/**
	 * Sets a policy's description and notes. They are not part of the policy's content, so this does
	 * not change its revision.
	 *
	 * <p>Requires the {@code policies:write} scope. Does not require an administrator, except to change
	 * another user's policy with {@code owner}.</p>
	 *
	 * <p>A description over 200 characters or notes over 1000 are an HTTP 400, a policy that does not
	 * exist an HTTP 404, and a managed policy an HTTP 409. Each is thrown as a
	 * {@link ClientException}.</p>
	 *
	 * @param policyName The name of the policy.
	 * @param description The description, up to 200 characters. {@code null} leaves it as it is, and
	 * an empty string clears it.
	 * @param notes The notes, up to 1000 characters. {@code null} leaves them as they are, and an empty
	 * string clears them.
	 * @param owner The owner of the policy. May be {@code null} for the caller's own. Another user's
	 * requires an administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the Philter
	 * deployment; otherwise, or for an owner that does not exist, Philter answers HTTP 404, thrown as a
	 * {@link ClientException}.
	 * @return The policy's details after the change.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public PolicyDetails setPolicyDetails(String policyName, String description, String notes, String owner)
			throws IOException {

		final HttpRequest request = json(uri("/api/policies/" + encode(policyName) + "/details", "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.PUT(text(gson.toJson(new SetPolicyDetailsRequest(description, notes))))
				.build();

		return sendExpectingJson(request, PolicyDetails.class);

	}

	/**
	 * Creates a policy by copying another. Requires the {@code policies:write} scope. See
	 * {@link #copyPolicy(String, String, String)}.
	 * @param policyName The name of the policy to copy, or of a managed policy.
	 * @param name The name of the new policy.
	 * @return The new policy's details.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public PolicyDetails copyPolicy(String policyName, String name) throws IOException {
		return copyPolicy(policyName, name, null);
	}

	/**
	 * Creates a policy by copying one of the caller's policies or, for a name beginning with
	 * {@code managed_}, a managed policy. The copy has the source's content and description, is active
	 * at once, and starts its own version history. A copy of a managed policy has the note
	 * {@code Created from managed policy <name>}; a copy of the caller's own policy keeps its notes.
	 *
	 * <p>Requires the {@code policies:write} scope. Does not require an administrator, except to copy
	 * within another user's account with {@code owner}.</p>
	 *
	 * <p>A missing or invalid new name is an HTTP 400, a source that does not exist an HTTP 404, and a
	 * new name already in use an HTTP 409 whose {@link ClientException#getReason()} is
	 * {@code policy_exists}. Each is thrown as a {@link ClientException}.</p>
	 *
	 * @param policyName The name of the policy to copy, or of a managed policy.
	 * @param name The name of the new policy.
	 * @param owner The owner of the policy. May be {@code null} for the caller's own. Another user's
	 * requires an administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the Philter
	 * deployment; otherwise, or for an owner that does not exist, Philter answers HTTP 404, thrown as a
	 * {@link ClientException}.
	 * @return The new policy's details.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public PolicyDetails copyPolicy(String policyName, String name, String owner) throws IOException {

		final HttpRequest request = json(uri("/api/policies/" + encode(policyName) + "/copy",
				"name", name, "owner", owner))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		return sendExpectingJson(request, PolicyDetails.class);

	}

	/**
	 * Deletes a policy.
	 *
	 * <p>A policy that does not exist is an HTTP 404. The {@code default} policy cannot be deleted: that
	 * is an HTTP 409 whose {@link ClientException#getReason()} is {@code policy_default}. Both are thrown
	 * as a {@link ClientException}.</p>
	 *
	 * @param policyName The name of the policy to delete.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deletePolicy(String policyName) throws IOException {
		deletePolicy(policyName, null);
	}

	/**
	 * Deletes a policy.
	 *
	 * <p>A policy that does not exist is an HTTP 404. The {@code default} policy cannot be deleted: that
	 * is an HTTP 409 whose {@link ClientException#getReason()} is {@code policy_default}. Both are thrown
	 * as a {@link ClientException}.</p>
	 *
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
	 * Rolls a policy back to a prior revision. See {@link #rollbackPolicy(String, int, String)}.
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
	 *
	 * <p>A policy or revision that does not exist is an HTTP 404 whose
	 * {@link ClientException#getErrorMessage()} says which, such as {@code Revision 99 does not exist.};
	 * it is {@code null} when the {@code owner} does not exist or may not be reached. Only the owner's
	 * own policies can be rolled back, so a managed policy is also an HTTP 404. A policy that changed
	 * concurrently is an HTTP 409 whose {@link ClientException#getReason()} is {@code policy_changed}:
	 * reload it and retry. Each is thrown as a {@link ClientException}.</p>
	 *
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
	 * @deprecated Use {@link #listContexts(String, Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
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
	 * @deprecated Use {@link #listContexts(String, Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
	public String getContexts(String owner, Integer offset, Integer limit) throws IOException {

		final HttpRequest request = request(uri("/api/contexts", "owner", owner, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Lists the names of the caller's contexts, paged. See {@link #listContexts(String, Integer, Integer)}.
	 * @return The context names.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetContextsResponse listContexts() throws IOException {
		return listContexts(null, null, null);
	}

	/**
	 * Lists the names of the caller's contexts, paged.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The context names.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetContextsResponse listContexts(String owner, Integer offset, Integer limit) throws IOException {
		return gson.fromJson(getContexts(owner, offset, limit), GetContextsResponse.class);
	}


	/**
	 * Gets the first page of every user's contexts, each naming its owner. See
	 * {@link #getContextsAcrossUsers(Integer, Integer)}.
	 * @return The contexts as JSON: a {@code contexts} array of objects, each with the context's {@code name}
	 * and its {@code owner}.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listContextsAcrossUsers(Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
	public String getContextsAcrossUsers() throws IOException {
		return getContextsAcrossUsers(null, null);
	}

	/**
	 * Gets a page of every user's contexts, each naming its owner.
	 *
	 * <p>Requires an administrator API key and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the
	 * Philter deployment (disabled by default). Otherwise Philter answers HTTP 404 in either case,
	 * thrown as a {@link ClientException}. Each call is recorded in Philter's audit log.</p>
	 *
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The contexts as JSON: a {@code contexts} array of objects, each with the context's {@code name}
	 * and its {@code owner}.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listContextsAcrossUsers(Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
	public String getContextsAcrossUsers(Integer offset, Integer limit) throws IOException {
		return sendExpectingString(request(uri("/api/contexts", "all_users", true, "offset", offset, "limit", limit))
				.GET().build());
	}

	/**
	 * Lists every user's contexts, paged, each naming its owner. Requires an administrator and
	 * {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404. See {@link #listContextsAcrossUsers(Integer, Integer)}.
	 * @return Each context's name and owner.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetContextsAcrossUsersResponse listContextsAcrossUsers() throws IOException {
		return listContextsAcrossUsers(null, null);
	}

	/**
	 * Lists every user's contexts, paged, each naming its owner. Requires an administrator and
	 * {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return Each context's name and owner.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetContextsAcrossUsersResponse listContextsAcrossUsers(Integer offset, Integer limit) throws IOException {
		return gson.fromJson(getContextsAcrossUsers(offset, limit), GetContextsAcrossUsersResponse.class);
	}


	/**
	 * Creates a context. See {@link #createContext(String, Boolean, Boolean, String)}.
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
	 *
	 * <p>The name is used in request paths, so Philter refuses one containing {@code /}, {@code \},
	 * {@code ;}, {@code %}, or a control character, or that is {@code .} or {@code ..}, with an HTTP 400,
	 * thrown as a {@link ClientException}.</p>
	 *
	 * @param name The name of the context.
	 * @param entityTypeDisambiguation Whether entity type disambiguation is enabled. {@code null} omits
	 * the parameter, which Philter reads as {@code false} rather than as "leave unchanged".
	 * @param ledger Whether the redaction ledger is enabled. {@code null} omits the parameter, which
	 * Philter reads as {@code false} rather than as "leave unchanged".
	 * @param owner The owner of the context. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 * @throws ClientException With status {@code 409} when the context was not created, and a
	 * {@link ClientException#getReason()} that says why: {@code context_exists} when the caller already has
	 * a context with that name, even at the limit, or {@code context_limit_reached} when the caller already
	 * has the most contexts a user may have (10, counting {@code default}).
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
	 * @return The context as JSON: {@code size}, the number of entries; {@code filterTypes}, a map of
	 * filter type to entry count; and {@code untyped}, the entries with no filter type. The counts sum
	 * to {@code size}.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #getContextDetails(String, String)}, which returns a typed model.
	 */
	@Deprecated
	public String getContext(String name) throws IOException {
		return getContext(name, null);
	}

	/**
	 * Gets a context by name.
	 * @param name The name of the context.
	 * @param owner The owner of the context. May be {@code null}.
	 * @return The context as JSON, as described on {@link #getContext(String)}.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #getContextDetails(String, String)}, which returns a typed model.
	 */
	@Deprecated
	public String getContext(String name, String owner) throws IOException {
		return sendExpectingString(request(uri("/api/contexts/" + encode(name), "owner", owner)).GET().build());
	}

	/**
	 * Gets a context's settings and its entries counted by filter type. A context that does not exist is an
	 * HTTP 404, thrown as a {@link ClientException}. See {@link #getContextDetails(String, String)}.
	 * @param name The name of the context.
	 * @return The context's settings and entry counts.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public ContextDetails getContextDetails(String name) throws IOException {
		return getContextDetails(name, null);
	}

	/**
	 * Gets a context's settings and its entries counted by filter type. A context that does not exist is an
	 * HTTP 404, thrown as a {@link ClientException}.
	 * @param name The name of the context.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @return The context's settings and entry counts.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public ContextDetails getContextDetails(String name, String owner) throws IOException {
		return gson.fromJson(getContext(name, owner), ContextDetails.class);
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
	 * Deletes a context. See {@link #deleteContext(String, String)}.
	 * @param name The name of the context.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteContext(String name) throws IOException {
		return deleteContext(name, null);
	}

	/**
	 * Deletes a context.
	 *
	 * <p>A name created before Philter refused names that cannot be used in a request path (containing
	 * {@code /}, {@code \}, {@code ;}, {@code %}, or a control character, or {@code .} or {@code ..}) is
	 * sent in the query string instead, so it can still be removed.</p>
	 *
	 * @param name The name of the context.
	 * @param owner The owner of the context. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteContext(String name, String owner) throws IOException {

		final URI uri = isPathSafe(name)
				? uri("/api/contexts/" + encode(name), "owner", owner)
				: uri("/api/contexts", "name", name, "owner", owner);
		final HttpRequest request = request(uri).DELETE().build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	/**
	 * Gets the entries for a context.
	 * @param name The name of the context.
	 * @return The context entries.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listContextEntries(String, String, Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
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
	 * @deprecated Use {@link #listContextEntries(String, String, Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
	public String getContextEntries(String name, String owner, Integer offset, Integer limit) throws IOException {

		final HttpRequest request = request(uri("/api/contexts/" + encode(name) + "/entries",
				"owner", owner, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Lists a context's entries, paged, with the total. The original values are never returned. See {@link #listContextEntries(String, String, Integer, Integer)}.
	 * @param name The name of the context.
	 * @return The page of entries and the total.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetContextEntriesResponse listContextEntries(String name) throws IOException {
		return listContextEntries(name, null, null, null);
	}

	/**
	 * Lists a context's entries, paged, with the total. The original values are never returned.
	 * @param name The name of the context.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The page of entries and the total.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetContextEntriesResponse listContextEntries(String name, String owner, Integer offset, Integer limit) throws IOException {
		return gson.fromJson(getContextEntries(name, owner, offset, limit), GetContextEntriesResponse.class);
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
	 * @deprecated Use {@link #listDocuments(String, Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
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
	 * @deprecated Use {@link #listDocuments(String, Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
	public String getDocuments(String owner, Integer offset, Integer limit) throws IOException {

		final HttpRequest request = json(uri("/api/documents", "owner", owner, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Lists the documents submitted for asynchronous redaction, paged. See {@link #listDocuments(String, Integer, Integer)}.
	 * @return The page of documents.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetDocumentsResponse listDocuments() throws IOException {
		return listDocuments(null, null, null);
	}

	/**
	 * Lists the documents submitted for asynchronous redaction, paged.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The page of documents.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetDocumentsResponse listDocuments(String owner, Integer offset, Integer limit) throws IOException {
		return gson.fromJson(getDocuments(owner, offset, limit), GetDocumentsResponse.class);
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
	 * @deprecated Use {@link #getDocumentState(String, String)}, which returns a typed model.
	 */
	@Deprecated
	public String getDocumentStatus(String documentId) throws IOException {
		return getDocumentStatus(documentId, null);
	}

	/**
	 * Gets the processing status of a document.
	 * @param documentId The document ID.
	 * @param owner The owner of the document. May be {@code null}.
	 * @return The document status.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #getDocumentState(String, String)}, which returns a typed model.
	 */
	@Deprecated
	public String getDocumentStatus(String documentId, String owner) throws IOException {

		final HttpRequest request = json(uri("/api/documents/" + encode(documentId) + "/status", "owner", owner))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Gets the status of a document submitted for asynchronous redaction. See {@link #getDocumentState(String, String)}.
	 * @param documentId The document ID.
	 * @return The document's status.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public DocumentStatus getDocumentState(String documentId) throws IOException {
		return getDocumentState(documentId, null);
	}

	/**
	 * Gets the status of a document submitted for asynchronous redaction.
	 * @param documentId The document ID.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @return The document's status.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public DocumentStatus getDocumentState(String documentId, String owner) throws IOException {
		return gson.fromJson(getDocumentStatus(documentId, owner), DocumentStatus.class);
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
	 * Gets the first page of every user's active legal holds, each naming its owner. See
	 * {@link #getHoldsAcrossUsers(Integer, Integer)}.
	 * @return The holds, most recently set first, each with its owner.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<OwnedLegalHoldResponse> getHoldsAcrossUsers() throws IOException {
		return getHoldsAcrossUsers(null, null);
	}

	/**
	 * Gets a page of every user's active legal holds, each naming its owner.
	 *
	 * <p>Requires an administrator API key and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the
	 * Philter deployment (disabled by default). Otherwise Philter answers HTTP 404 in either case,
	 * thrown as a {@link ClientException}. Each call is recorded in Philter's audit log.</p>
	 *
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The holds, most recently set first, each with its owner.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<OwnedLegalHoldResponse> getHoldsAcrossUsers(Integer offset, Integer limit) throws IOException {

		final HttpRequest request = json(uri("/api/holds", "all_users", true, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingJson(request, OWNED_LEGAL_HOLD_LIST);

	}

	/**
	 * Creates a legal hold. See {@link #createHold(LegalHoldRequest, String)}.
	 * @param request The {@link LegalHoldRequest}.
	 * @return The created {@link LegalHoldResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LegalHoldResponse createHold(LegalHoldRequest request) throws IOException {
		return createHold(request, null);
	}

	/**
	 * Creates a legal hold.
	 *
	 * <p>The reference is used in request paths, so Philter refuses one containing {@code /}, {@code \},
	 * {@code ;}, {@code %}, or a control character, or that is {@code .} or {@code ..}, with an HTTP 400,
	 * thrown as a {@link ClientException}.</p>
	 *
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
	 * Deletes a legal hold. See {@link #deleteHold(String, String)}.
	 * @param reference The legal hold reference.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteHold(String reference) throws IOException {
		deleteHold(reference, null);
	}

	/**
	 * Deletes (releases) a legal hold.
	 *
	 * <p>A reference created before Philter refused references that cannot be used in a request path (containing
	 * {@code /}, {@code \}, {@code ;}, {@code %}, or a control character, or {@code .} or {@code ..}) is
	 * sent in the query string instead, so it can still be removed.</p>
	 *
	 * @param reference The legal hold reference.
	 * @param owner The owner of the hold. May be {@code null}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteHold(String reference, String owner) throws IOException {

		final URI uri = isPathSafe(reference)
				? uri("/api/holds/" + encode(reference), "owner", owner)
				: uri("/api/holds", "reference", reference, "owner", owner);

		sendExpectingNoContent(request(uri).DELETE().build());

	}

	// Redaction ledger.

	/**
	 * Queries the redaction ledger.
	 * @param query The query. May be {@code null}.
	 * @return The matching ledger entries.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listLedgerChains(String, String, Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
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
	 * @deprecated Use {@link #listLedgerChains(String, String, Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
	public String getLedger(String query, String owner, Integer offset, Integer limit) throws IOException {

		final HttpRequest request = request(uri("/api/ledger", "q", query, "owner", owner,
				"offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Lists redaction-ledger chains, most recent first, paged, with the total matching. See {@link #listLedgerChains(String, String, Integer, Integer)}.
	 * @param query Matches a document ID or filename. May be {@code null} for every chain.
	 * @return The head of each chain and the total.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetLedgerResponse listLedgerChains(String query) throws IOException {
		return listLedgerChains(query, null, null, null);
	}

	/**
	 * Lists redaction-ledger chains, most recent first, paged, with the total matching.
	 * @param query Matches a document ID or filename. May be {@code null} for every chain.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The head of each chain and the total.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetLedgerResponse listLedgerChains(String query, String owner, Integer offset, Integer limit) throws IOException {
		return gson.fromJson(getLedger(query, owner, offset, limit), GetLedgerResponse.class);
	}


	/**
	 * Gets the first page of every user's redaction-ledger chains, each naming its owner. See
	 * {@link #getLedgerAcrossUsers(Integer, Integer)}.
	 * @return The chains as JSON: a {@code chains} array of chain heads, most recent first, each with an
	 * {@code owner} field, and a {@code total} counting every user's chains.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listLedgerChainsAcrossUsers(Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
	public String getLedgerAcrossUsers() throws IOException {
		return getLedgerAcrossUsers(null, null);
	}

	/**
	 * Gets a page of every user's redaction-ledger chains, each naming its owner.
	 *
	 * <p>Requires an administrator API key and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the
	 * Philter deployment (disabled by default). Otherwise Philter answers HTTP 404 in either case,
	 * thrown as a {@link ClientException}. Each call is recorded in Philter's audit log.</p>
	 *
	 * <p>Unlike {@link #getLedger(String)}, this cannot filter by query: Philter rejects the combination.</p>
	 *
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The chains as JSON: a {@code chains} array of chain heads, most recent first, each with an
	 * {@code owner} field, and a {@code total} counting every user's chains.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listLedgerChainsAcrossUsers(Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
	public String getLedgerAcrossUsers(Integer offset, Integer limit) throws IOException {
		return sendExpectingString(request(uri("/api/ledger", "all_users", true, "offset", offset, "limit", limit))
				.GET().build());
	}

	/**
	 * Lists every user's redaction-ledger chains, paged, each naming its owner. Requires an administrator and
	 * {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404. See {@link #listLedgerChainsAcrossUsers(Integer, Integer)}.
	 * @return The head of each chain, with its owner, and the total.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetLedgerResponse listLedgerChainsAcrossUsers() throws IOException {
		return listLedgerChainsAcrossUsers(null, null);
	}

	/**
	 * Lists every user's redaction-ledger chains, paged, each naming its owner. Requires an administrator and
	 * {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The head of each chain, with its owner, and the total.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetLedgerResponse listLedgerChainsAcrossUsers(Integer offset, Integer limit) throws IOException {
		return gson.fromJson(getLedgerAcrossUsers(offset, limit), GetLedgerResponse.class);
	}


	/**
	 * Gets the ledger entry for a document.
	 * @param documentId The document ID.
	 * @return The ledger entry.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #getLedgerChain(String, String)}, which returns a typed model.
	 */
	@Deprecated
	public String getLedgerEntry(String documentId) throws IOException {
		return getLedgerEntry(documentId, null);
	}

	/**
	 * Gets the ledger entry for a document.
	 * @param documentId The document ID.
	 * @param owner The owner of the entry. May be {@code null}.
	 * @return The ledger entry.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #getLedgerChain(String, String)}, which returns a typed model.
	 */
	@Deprecated
	public String getLedgerEntry(String documentId, String owner) throws IOException {
		return sendExpectingString(request(uri("/api/ledger/" + encode(documentId), "owner", owner)).GET().build());
	}

	/**
	 * Gets a document's redaction-ledger chain and whether it verifies. The original redacted values are not
	 * returned; see {@link #getLedgerExport(String)}. See {@link #getLedgerChain(String, String)}.
	 * @param documentId The document ID.
	 * @return The chain.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LedgerChain getLedgerChain(String documentId) throws IOException {
		return getLedgerChain(documentId, null);
	}

	/**
	 * Gets a document's redaction-ledger chain and whether it verifies. The original redacted values are not
	 * returned; see {@link #getLedgerExport(String)}.
	 *
	 * <p>A chain that could not be checked, for example because an entry can no longer be decrypted, is
	 * returned with {@link LedgerChain#isValid()} {@code false} and a {@link LedgerChain#getValidationError()};
	 * that is not evidence of tampering. A chain that failed a check has no validation error, and
	 * {@link LedgerChain#getHashChainValid()} and {@link LedgerChain#getSignaturesValid()} say which; an
	 * unsigned entry fails the signature check. See {@link LedgerChain}. A document with no chain is an HTTP 404, thrown as a {@link ClientException}.</p>
	 *
	 * @param documentId The document ID.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @return The chain.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LedgerChain getLedgerChain(String documentId, String owner) throws IOException {
		return gson.fromJson(getLedgerEntry(documentId, owner), LedgerChain.class);
	}


	/**
	 * Exports the ledger entry for a document.
	 * @param documentId The document ID.
	 * @return The exported ledger entry.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #getLedgerExport(String, String)}, which returns a typed model.
	 */
	@Deprecated
	public String exportLedger(String documentId) throws IOException {
		return exportLedger(documentId, null);
	}

	/**
	 * Exports the ledger entry for a document.
	 * @param documentId The document ID.
	 * @param owner The owner of the entry. May be {@code null}.
	 * @return The exported ledger entry.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #getLedgerExport(String, String)}, which returns a typed model.
	 */
	@Deprecated
	public String exportLedger(String documentId, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/ledger/" + encode(documentId) + "/export", "owner", owner))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Exports a document's redaction-ledger chain with the public keys that verify it. Requires the
	 * {@code ledger:export} scope. The export carries the original redacted values, so treat it as
	 * sensitive. See {@link #getLedgerExport(String, String)}.
	 * @param documentId The document ID.
	 * @return The export.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LedgerExport getLedgerExport(String documentId) throws IOException {
		return getLedgerExport(documentId, null);
	}

	/**
	 * Exports a document's redaction-ledger chain with the public keys that verify it. Requires the
	 * {@code ledger:export} scope. The export carries the original redacted values, so treat it as
	 * sensitive.
	 * @param documentId The document ID.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @return The export.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LedgerExport getLedgerExport(String documentId, String owner) throws IOException {
		return gson.fromJson(exportLedger(documentId, owner), LedgerExport.class);
	}


	/**
	 * Checks whether the ledger for a document is valid.
	 * @param documentId The document ID.
	 * @return The validity result.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #verifyLedgerChain(String, String)}, which returns a typed model.
	 */
	@Deprecated
	public String isLedgerValid(String documentId) throws IOException {
		return isLedgerValid(documentId, null);
	}

	/**
	 * Checks whether the ledger for a document is valid.
	 * @param documentId The document ID.
	 * @param owner The owner of the entry. May be {@code null}.
	 * @return The validity result.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #verifyLedgerChain(String, String)}, which returns a typed model.
	 */
	@Deprecated
	public String isLedgerValid(String documentId, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/ledger/" + encode(documentId) + "/valid", "owner", owner))
				.GET()
				.build();

		return sendExpectingString(request);

	}

	/**
	 * Checks whether a document's redaction-ledger chain verifies, without returning its entries. See {@link #verifyLedgerChain(String, String)}.
	 * @param documentId The document ID.
	 * @return Whether the chain verifies; {@link LedgerChain#getEntries()} is {@code null}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LedgerChain verifyLedgerChain(String documentId) throws IOException {
		return verifyLedgerChain(documentId, null);
	}

	/**
	 * Checks whether a document's redaction-ledger chain verifies, without returning its entries.
	 *
	 * <p>A chain that could not be checked, for example because an entry can no longer be decrypted, is
	 * returned with {@link LedgerChain#isValid()} {@code false} and a {@link LedgerChain#getValidationError()};
	 * that is not evidence of tampering. A chain that failed a check has no validation error, and
	 * {@link LedgerChain#getHashChainValid()} and {@link LedgerChain#getSignaturesValid()} say which; an
	 * unsigned entry fails the signature check. See {@link LedgerChain}. A document with no chain is an HTTP 404, thrown as a {@link ClientException}.</p>
	 *
	 * @param documentId The document ID.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @return Whether the chain verifies; {@link LedgerChain#getEntries()} is {@code null}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LedgerChain verifyLedgerChain(String documentId, String owner) throws IOException {
		return gson.fromJson(isLedgerValid(documentId, owner), LedgerChain.class);
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
	 * @deprecated Use {@link #listCustomLists(String)}, which returns a typed model.
	 */
	@Deprecated
	public String getLists() throws IOException {
		return getLists(null);
	}

	/**
	 * Gets the custom lists.
	 * @param owner The owner of the lists. May be {@code null}.
	 * @return The custom lists.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listCustomLists(String)}, which returns a typed model.
	 */
	@Deprecated
	public String getLists(String owner) throws IOException {
		return sendExpectingString(request(uri("/api/lists", "owner", owner)).GET().build());
	}

	/**
	 * Lists the caller's custom lists with their descriptions and sizes. Get a list's terms with
	 * {@link #getList(String)}. See {@link #listCustomLists(String)}.
	 * @return Each list's name, description, and size.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<CustomListSummary> listCustomLists() throws IOException {
		return listCustomLists(null);
	}

	/**
	 * Lists the caller's custom lists with their descriptions and sizes. Get a list's terms with
	 * {@link #getList(String)}.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @return Each list's name, description, and size.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<CustomListSummary> listCustomLists(String owner) throws IOException {
		return gson.fromJson(getLists(owner), CUSTOM_LIST_SUMMARY_LIST);
	}


	/**
	 * Gets the first page of every user's custom lists, each naming its owner. See
	 * {@link #getListsAcrossUsers(Integer, Integer)}.
	 * @return The lists as JSON: an array of objects, each with the list's {@code name}, {@code description},
	 * {@code size}, and {@code owner}.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listCustomListsAcrossUsers(Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
	public String getListsAcrossUsers() throws IOException {
		return getListsAcrossUsers(null, null);
	}

	/**
	 * Gets a page of every user's custom lists, each naming its owner.
	 *
	 * <p>Requires an administrator API key and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the
	 * Philter deployment (disabled by default). Otherwise Philter answers HTTP 404 in either case,
	 * thrown as a {@link ClientException}. Each call is recorded in Philter's audit log.</p>
	 *
	 * <p>Unlike {@link #getLists()}, which returns every one of the caller's lists, this listing is paged.</p>
	 *
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The lists as JSON: an array of objects, each with the list's {@code name}, {@code description},
	 * {@code size}, and {@code owner}.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listCustomListsAcrossUsers(Integer, Integer)}, which returns a typed model.
	 */
	@Deprecated
	public String getListsAcrossUsers(Integer offset, Integer limit) throws IOException {
		return sendExpectingString(request(uri("/api/lists", "all_users", true, "offset", offset, "limit", limit))
				.GET().build());
	}

	/**
	 * Lists every user's custom lists, paged, each naming its owner. Requires an administrator and
	 * {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404. See {@link #listCustomListsAcrossUsers(Integer, Integer)}.
	 * @return Each list's name, description, size, and owner.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<CustomListSummary> listCustomListsAcrossUsers() throws IOException {
		return listCustomListsAcrossUsers(null, null);
	}

	/**
	 * Lists every user's custom lists, paged, each naming its owner. Requires an administrator and
	 * {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @param offset The number of items to skip. May be {@code null} for {@code 0}.
	 * @param limit The most items to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return Each list's name, description, size, and owner.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<CustomListSummary> listCustomListsAcrossUsers(Integer offset, Integer limit) throws IOException {
		return gson.fromJson(getListsAcrossUsers(offset, limit), CUSTOM_LIST_SUMMARY_LIST);
	}


	/**
	 * Creates a custom list. It only creates: a name the owner already uses is a {@link ClientException}
	 * with status {@code 409} and {@link ClientException#getReason()} {@code list_exists}. Replace an
	 * existing list with {@link #replaceList(String, String, List)}. A list holds up to 100 items of up
	 * to 50 characters; more is an HTTP 400, thrown as a {@link ClientException}.
	 *
	 * <p>The name is used in request paths, so one containing {@code /}, {@code \}, {@code ;},
	 * {@code %}, or a control character, or that is {@code .} or {@code ..}, is refused with an HTTP
	 * 400, thrown as a {@link ClientException}. Some of these are refused by the web server before the
	 * request reaches Philter, so {@link ClientException#getErrorMessage()} may be {@code null}.</p>
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
	 * Creates a custom list. It only creates: a name the owner already uses is a {@link ClientException}
	 * with status {@code 409} and {@link ClientException#getReason()} {@code list_exists}. Replace an
	 * existing list with {@link #replaceList(String, String, List)}. A list holds up to 100 items of up
	 * to 50 characters; more is an HTTP 400, thrown as a {@link ClientException}.
	 *
	 * <p>The name is used in request paths, so one containing {@code /}, {@code \}, {@code ;},
	 * {@code %}, or a control character, or that is {@code .} or {@code ..}, is refused with an HTTP
	 * 400, thrown as a {@link ClientException}. Some of these are refused by the web server before the
	 * request reaches Philter, so {@link ClientException#getErrorMessage()} may be {@code null}.</p>
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
	 * Replaces an existing custom list's items. Requires the {@code lists:write} scope. See
	 * {@link #replaceList(String, String, List, String)}.
	 * @param list The name of the list.
	 * @param description The description. {@code null} keeps the current one, and an empty string clears it.
	 * @param values The items, which replace the list's current items.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse replaceList(String list, String description, List<String> values) throws IOException {
		return replaceList(list, description, values, null);
	}

	/**
	 * Replaces an existing custom list's items, and optionally its description. Use
	 * {@link #saveList(String, String, List)} to create one.
	 *
	 * <p>Requires the {@code lists:write} scope. Does not require an administrator, except to replace
	 * another user's list with {@code owner}.</p>
	 *
	 * <p>A list that does not exist is an HTTP 404. More than 100 items, or an item over 50 characters,
	 * is an HTTP 400. Both are thrown as a {@link ClientException}.</p>
	 *
	 * @param list The name of the list.
	 * @param description The description. {@code null} keeps the current one, and an empty string clears it.
	 * @param values The items, which replace the list's current items.
	 * @param owner The owner of the list. May be {@code null} for the caller's own. Another user's
	 * requires an administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the Philter
	 * deployment; otherwise, or for an owner that does not exist, Philter answers HTTP 404, thrown as a
	 * {@link ClientException}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse replaceList(String list, String description, List<String> values, String owner)
			throws IOException {

		final HttpRequest request = request(uri("/api/lists/" + encode(list), "description", description, "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.PUT(text(gson.toJson(values)))
				.build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	/**
	 * Deletes a custom list. See {@link #deleteList(String, String)}.
	 * @param list The name of the list.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteList(String list) throws IOException {
		deleteList(list, null);
	}

	/**
	 * Deletes a custom list.
	 *
	 * <p>A name created before Philter refused names that cannot be used in a request path (containing
	 * {@code /}, {@code \}, {@code ;}, {@code %}, or a control character, or {@code .} or {@code ..}) is
	 * sent in the query string instead, so it can still be removed.</p>
	 *
	 * @param list The name of the list.
	 * @param owner The owner of the list. May be {@code null}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteList(String list, String owner) throws IOException {

		final URI uri = isPathSafe(list)
				? uri("/api/lists/" + encode(list), "owner", owner)
				: uri("/api/lists", "name", list, "owner", owner);

		sendExpectingNoContent(request(uri).DELETE().build());

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
	 * @deprecated Use {@link #listRedactLists(String)}, which returns a typed model.
	 */
	@Deprecated
	public String getRedactLists() throws IOException {
		return getRedactLists(null);
	}

	/**
	 * Gets the redact lists.
	 * @param owner The owner of the redact lists. May be {@code null}.
	 * @return The redact lists.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #listRedactLists(String)}, which returns a typed model.
	 */
	@Deprecated
	public String getRedactLists(String owner) throws IOException {
		return sendExpectingString(json(uri("/api/redact-lists", "owner", owner)).GET().build());
	}

	/**
	 * Gets the caller's always-redact and never-redact lists. See {@link #listRedactLists(String)}.
	 * @return Both lists.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public RedactLists listRedactLists() throws IOException {
		return listRedactLists(null);
	}

	/**
	 * Gets the caller's always-redact and never-redact lists.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @return Both lists.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public RedactLists listRedactLists(String owner) throws IOException {
		return gson.fromJson(getRedactLists(owner), RedactLists.class);
	}


	/**
	 * Creates a redact list.
	 * @param json The redact list as JSON.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #createRedactList(RedactListsRequest, String)}, which takes a typed request.
	 */
	@Deprecated
	public GenericResponse createRedactList(String json) throws IOException {
		return createRedactList(json, null);
	}

	/**
	 * Creates a redact list.
	 * @param json The redact list as JSON.
	 * @param owner The owner of the redact list. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #createRedactList(RedactListsRequest, String)}, which takes a typed request.
	 */
	@Deprecated
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
	 * @deprecated Use {@link #updateRedactList(RedactListsRequest, String)}, which takes a typed request.
	 */
	@Deprecated
	public GenericResponse updateRedactList(String json) throws IOException {
		return updateRedactList(json, null);
	}

	/**
	 * Updates a redact list.
	 * @param json The redact list as JSON.
	 * @param owner The owner of the redact list. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 * @deprecated Use {@link #updateRedactList(RedactListsRequest, String)}, which takes a typed request.
	 */
	@Deprecated
	public GenericResponse updateRedactList(String json, String owner) throws IOException {

		final HttpRequest request = request(uri("/api/redact-lists", "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.PUT(text(json))
				.build();

		return sendExpectingJson(request, GenericResponse.class);

	}

	/**
	 * Replaces the always-redact and never-redact lists in full. This is not a merge: each list in the
	 * request is the complete contents of that list, and a list left {@code null} or empty is cleared. Use
	 * {@link #updateRedactList(RedactListsRequest)} to add terms instead. Each list holds up to 1000 terms of
	 * up to 100 characters; more is an HTTP 400, thrown as a {@link ClientException}.
	 * @param request The lists.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse createRedactList(RedactListsRequest request) throws IOException {
		return createRedactList(request, null);
	}

	/**
	 * Replaces the always-redact and never-redact lists in full. See {@link #createRedactList(RedactListsRequest)}.
	 * @param request The lists.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse createRedactList(RedactListsRequest request, String owner) throws IOException {
		return createRedactList(gson.toJson(request), owner);
	}

	/**
	 * Appends terms to the always-redact and never-redact lists, keeping the terms already there. Use
	 * {@link #createRedactList(RedactListsRequest)} to replace the lists instead. A resulting list over 1000
	 * terms, or a term over 100 characters, is an HTTP 400, thrown as a {@link ClientException}.
	 * @param request The terms to add.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse updateRedactList(RedactListsRequest request) throws IOException {
		return updateRedactList(request, null);
	}

	/**
	 * Appends terms to the always-redact and never-redact lists. See {@link #updateRedactList(RedactListsRequest)}.
	 * @param request The terms to add.
	 * @param owner The owner. May be {@code null} for the caller's own. Another user's requires an
	 * administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true}; otherwise Philter answers HTTP 404.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse updateRedactList(RedactListsRequest request, String owner) throws IOException {
		return updateRedactList(gson.toJson(request), owner);
	}


	// Audit log.

	/**
	 * Gets the first page of the audit log, most recent first, with no filters. Requires the
	 * {@code audit:read} scope and an administrator. See
	 * {@link #getAuditLog(String, Instant, Instant, String, Integer, Integer)}.
	 * @return The page of events and the total.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetAuditLogResponse getAuditLog() throws IOException {
		return getAuditLog(null, null, null, null, null, null);
	}

	/**
	 * Gets a page of the audit log, most recent first, filtered by event type, time, and acting user.
	 * The log covers the whole deployment. Reading it is itself recorded as an
	 * {@code audit_log_retrieved} event.
	 *
	 * <p>Requires the {@code audit:read} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown
	 * as a {@link ClientException} carrying that message.</p>
	 *
	 * <p>An event type Philter does not record, or {@code from} after {@code to}, is an HTTP 400, thrown
	 * as a {@link ClientException}. An unknown event type is refused rather than returning an empty
	 * page.</p>
	 *
	 * @param event The event type to return, such as {@code policy_deleted}. May be {@code null} for
	 * every type.
	 * @param from The earliest time to return, inclusive. May be {@code null}.
	 * @param to The time to return events before, exclusive. May be {@code null}.
	 * @param owner The username of the acting user whose events to return. May be {@code null} for
	 * every user. Another user requires {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the Philter
	 * deployment; otherwise, or for a username that does not exist, Philter answers HTTP 404, thrown as
	 * a {@link ClientException}. Some events record the affected entity rather than the acting user, so
	 * an {@code owner} filter does not return them.
	 * @param offset The number of events to skip. May be {@code null} for {@code 0}.
	 * @param limit The most events to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The page of events and the total number matching the filters.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetAuditLogResponse getAuditLog(String event, Instant from, Instant to, String owner, Integer offset,
			Integer limit) throws IOException {

		final HttpRequest request = json(uri("/api/audit", "event", event, "from", from, "to", to,
				"owner", owner, "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingJson(request, GetAuditLogResponse.class);

	}

	/**
	 * Exports the first page of the audit log as CSV for a range of whole days, reading the dates in
	 * the Philter server's time zone. See
	 * {@link #exportAuditLog(LocalDate, LocalDate, ZoneId, Integer, Integer)}.
	 * @param from The first day, included in full.
	 * @param to The last day, included in full. At most 30 days after {@code from}.
	 * @return The page of the export.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public AuditLogExport exportAuditLog(LocalDate from, LocalDate to) throws IOException {
		return exportAuditLog(from, to, null, null, null);
	}

	/**
	 * Exports a page of the audit log as CSV for a range of whole days, most recent first.
	 *
	 * <p>The calling key must hold the {@code audit:read} scope and belong to an administrator. The
	 * export is itself recorded in the audit log.</p>
	 *
	 * <p>{@code from} and {@code to} are both inclusive and are read in {@code zone}, or in the Philter
	 * server's time zone when {@code zone} is {@code null}. {@link AuditLogExport#getTimeZone()} reports
	 * the zone used. Timestamps in the CSV are UTC whatever the zone.</p>
	 *
	 * <p>A page holds at most {@code limit} events. When {@link AuditLogExport#isTruncated()} is
	 * {@code true}, more events remain: request the next page with {@code offset} set to
	 * {@link AuditLogExport#getNextOffset()}. A range that includes the current day can repeat events
	 * across pages.</p>
	 *
	 * <p>Philter rejects a missing date, a reversed range, a {@code to} more than 30 days after
	 * {@code from}, an unknown zone, or a negative offset with an HTTP 400, thrown as a
	 * {@link ClientException} carrying Philter's reason. A caller who is not an administrator
	 * gets an HTTP 403, also as a {@link ClientException}.</p>
	 *
	 * @param from The first day, included in full.
	 * @param to The last day, included in full. At most 30 days after {@code from}.
	 * @param zone The time zone the days are read in. May be {@code null} for the server's time zone.
	 * @param offset The number of events to skip. May be {@code null} for {@code 0}.
	 * @param limit The most events to return, up to 1000. Philter treats a larger value as 1000, and zero
	 * or less as its default of 100. May be {@code null} for the default.
	 * @return The page of the export.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public AuditLogExport exportAuditLog(LocalDate from, LocalDate to, ZoneId zone, Integer offset, Integer limit)
			throws IOException {

		final HttpRequest request = request(uri("/api/audit/export", "from", from, "to", to,
				"zone", zone == null ? null : zone.getId(), "offset", offset, "limit", limit))
				.GET()
				.build();

		final HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray());

		if(!isSuccessful(response)) {
			throw toException(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
		}

		// Without these headers a truncated page is indistinguishable from a complete one, so refuse it.
		final String rows = response.headers().firstValue(EXPORT_ROWS_HEADER).orElse(null);
		final String truncated = response.headers().firstValue(EXPORT_TRUNCATED_HEADER).orElse(null);
		if(rows == null || truncated == null) {
			throw new ClientException("The audit log export response is missing the "
					+ EXPORT_ROWS_HEADER + " or " + EXPORT_TRUNCATED_HEADER + " header.");
		}

		try {

			final Integer nextOffset = response.headers().firstValue(EXPORT_NEXT_OFFSET_HEADER)
					.map(Integer::valueOf).orElse(null);

			return new AuditLogExport(response.body(), Integer.parseInt(rows), Boolean.parseBoolean(truncated),
					nextOffset, response.headers().firstValue(EXPORT_TIME_ZONE_HEADER).orElse(null));

		} catch (final NumberFormatException ex) {
			throw new ClientException("The audit log export response has a malformed "
					+ EXPORT_ROWS_HEADER + " or " + EXPORT_NEXT_OFFSET_HEADER + " header.");
		}

	}

	// Sign-in.

	/**
	 * Signs a person in with a username and password, for a user interface acting as that person.
	 * Requires no API key: Philter ignores any the client was built with, so a client holding an expired
	 * session key can still sign in.
	 *
	 * <p>The result is either a session key or, for a user enrolled in MFA, a challenge: when
	 * {@link SignInResponse#isMfaRequired()} is {@code true}, pass {@link SignInResponse#getChallenge()}
	 * and a code from the person's authenticator app to {@link #completeSignIn(String, String)}.
	 * Otherwise use {@link SignInResponse#getApiKey()} for the person's requests: build a client with
	 * {@code withApiKey("Bearer " + response.getApiKey())}. The key expires after a period without a
	 * request ({@link SignInResponse#getIdleExpiresAt()}) or at the end of its maximum lifetime
	 * ({@link SignInResponse#getExpiresAt()}); a request with an expired or revoked key is refused with
	 * an {@link UnauthorizedException}. When {@link SignInResponse#isPasswordChangeRequired()} or
	 * {@link SignInResponse#isMfaEnrollmentRequired()} is {@code true}, the key can only change the
	 * password or enroll in MFA, and sign out.</p>
	 *
	 * <p>Password sign-in is disabled unless the deployment sets {@code PASSWORD_SIGN_IN_ENABLED=true};
	 * otherwise Philter answers HTTP 404, thrown as a {@link ClientException}. A wrong password, an
	 * unknown username, a user without a password, and a deactivated user are all refused with the same
	 * HTTP 401, thrown as an {@link UnauthorizedException}. A user whose MFA is locked after repeated bad
	 * codes is refused with an HTTP 403, thrown as a {@link ClientException}, until an administrator
	 * unlocks them.</p>
	 *
	 * <p>A username locked after repeated failed sign-ins is refused with a
	 * {@link ai.philterd.philter.model.exceptions.SignInLockedException}, even with the right password,
	 * and a client address over the sign-in rate limit with a
	 * {@link ai.philterd.philter.model.exceptions.SignInRateLimitedException}. Both carry the seconds to
	 * wait.</p>
	 *
	 * <p>An application signing people in should pass each person's address with
	 * {@link #signIn(String, String, String)}, or they all share its rate limit.</p>
	 *
	 * @param username The username.
	 * @param password The password.
	 * @return The session key, or an MFA challenge.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public SignInResponse signIn(String username, String password) throws IOException {
		return signIn(username, password, null);
	}

	/**
	 * Signs a person in on behalf of an application, such as a web front end, passing the person's own
	 * address so that Philter rate-limits and audits the sign-in by that address rather than by the
	 * application's. Otherwise the same as {@link #signIn(String, String)}.
	 *
	 * <p>The address is sent as {@code X-Forwarded-For}. Philter believes the header only when the request
	 * reaches it from an address in its {@code TRUSTED_PROXIES}, which by default are the loopback, private,
	 * link-local, and IPv6 unique-local ranges; from any other address, and for a value that is not an IP
	 * address (a port is allowed and ignored), Philter uses the connection's own address. Pass an address
	 * the application determined itself, such as the remote address of the person's connection to it, not
	 * one the person's browser supplied, which they can set to anything.</p>
	 *
	 * @param username The username.
	 * @param password The password.
	 * @param clientAddress The person's IP address. {@code null}, empty, or only spaces sends no header.
	 * @return The session key, or an MFA challenge.
	 * @throws IllegalArgumentException If {@code clientAddress} contains a comma, a control character such
	 * as a carriage return or line feed, a character outside ASCII, or a space other than at either end.
	 * Nothing is sent.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public SignInResponse signIn(String username, String password, String clientAddress) throws IOException {
		return sendSignIn("/api/sign-in", new SignInRequest(username, password), clientAddress);
	}

	/**
	 * Completes a sign-in for a user enrolled in MFA, with the challenge {@link #signIn(String, String)}
	 * returned and a code from the person's authenticator app. Requires no API key.
	 *
	 * <p>The challenge expires after five minutes and is used up by any attempt, right or wrong, so a
	 * wrong code means signing in again with the password. A wrong code, or an unknown, used, or expired
	 * challenge, is refused with an HTTP 401, thrown as an {@link UnauthorizedException}; Philter does not
	 * say which. The fifth consecutive bad code locks the user's MFA, which is then refused with an HTTP
	 * 403, thrown as a {@link ClientException}, until an administrator unlocks it. A client address over
	 * the sign-in rate limit is refused with a
	 * {@link ai.philterd.philter.model.exceptions.SignInRateLimitedException}. An application signing
	 * people in passes each person's address with {@link #completeSignIn(String, String, String)}.</p>
	 *
	 * @param challenge The challenge from {@link SignInResponse#getChallenge()}.
	 * @param code The code from the person's authenticator app.
	 * @return The session key.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public SignInResponse completeSignIn(String challenge, String code) throws IOException {
		return completeSignIn(challenge, code, null);
	}

	/**
	 * Completes an MFA sign-in on behalf of an application, passing the person's own address as
	 * {@code X-Forwarded-For}. Otherwise the same as {@link #completeSignIn(String, String)}. See
	 * {@link #signIn(String, String, String)} for when Philter uses the address and which address to pass.
	 *
	 * @param challenge The challenge from {@link SignInResponse#getChallenge()}.
	 * @param code The code from the person's authenticator app.
	 * @param clientAddress The person's IP address. {@code null}, empty, or only spaces sends no header.
	 * @return The session key.
	 * @throws IllegalArgumentException If {@code clientAddress} contains a comma, a control character such
	 * as a carriage return or line feed, a character outside ASCII, or a space other than at either end.
	 * Nothing is sent.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public SignInResponse completeSignIn(String challenge, String code, String clientAddress) throws IOException {
		return sendSignIn("/api/sign-in/mfa", new SignInMfaRequest(challenge, code), clientAddress);
	}

	private SignInResponse sendSignIn(final String path, final Object body, final String clientAddress)
			throws IOException {

		final String forwardedFor = forwardedFor(clientAddress);

		final HttpRequest.Builder builder = json(uri(path))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(gson.toJson(body)));

		if (forwardedFor != null) {
			builder.header("X-Forwarded-For", forwardedFor);
		}

		final HttpRequest request = builder.build();

		final HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());

		if(!isSuccessful(response)) {
			throw toSignInException(response);
		}

		return gson.fromJson(response.body(), SignInResponse.class);

	}

	/**
	 * The {@code X-Forwarded-For} value for a client address, or {@code null} to send none. One address
	 * only, in printable ASCII as every IP address is: a comma would add entries, and a line break would
	 * start another header.
	 */
	private static String forwardedFor(final String clientAddress) {

		if (clientAddress == null) {
			return null;
		}

		// Checked before trimming, which would quietly drop a trailing line break.
		for (int i = 0; i < clientAddress.length(); i++) {
			final char c = clientAddress.charAt(i);
			if (c == ',' || c < 0x20 || c > 0x7e) {
				throw invalidClientAddress();
			}
		}

		final String address = clientAddress.strip();
		if (address.isEmpty()) {
			return null;
		}
		if (address.indexOf(' ') >= 0) {
			throw invalidClientAddress();
		}

		return address;

	}

	private static IllegalArgumentException invalidClientAddress() {
		return new IllegalArgumentException("The client address must be a single address, in printable ASCII "
				+ "without commas or whitespace.");
	}

	/**
	 * Signs out: revokes the session key this client was built with. Any key can call it, whatever its
	 * scopes, and no administrator is needed. A long-lived key cannot revoke itself, so calling this with
	 * one is refused with an HTTP 409, thrown as a {@link ClientException}; revoke it with another key
	 * with {@link #revokeApiKey(String)}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void signOut() throws IOException {
		sendExpectingNoContent(request(uri("/api/api-keys/current")).DELETE().build());
	}

	// Admin settings.

	/**
	 * Gets the deployment's admin settings: differential-privacy counts, output signing, the webhook
	 * destination allowlist, and Phield publishing. The Phield API key is never returned, only whether
	 * one is set.
	 *
	 * <p>Requires the {@code settings:read} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown
	 * as a {@link ClientException} carrying that message.</p>
	 *
	 * @return The settings. {@link AdminSettings#getWarnings()} is empty on a read.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public AdminSettings getAdminSettings() throws IOException {
		return sendExpectingJson(json(uri("/api/settings")).GET().build(), AdminSettings.class);
	}

	/**
	 * Changes the deployment's admin settings. Only the fields set on the request are sent, and Philter
	 * leaves the rest as they are. Set {@code phieldApiKey} to an empty string to remove the key.
	 *
	 * <p>Requires the {@code settings:write} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown
	 * as a {@link ClientException} carrying that message.</p>
	 *
	 * <p>Philter validates every value before saving any. A {@code webhookAllowlist} entry that is not
	 * a hostname, an IP address, or a CIDR range, a {@code phieldUrl} that is not an absolute
	 * {@code http} or {@code https} URL, or a request that sets {@code phieldEnabled} or
	 * {@code phieldUrl} and would leave Phield enabled without a URL is an HTTP 400, thrown as a
	 * {@link ClientException} carrying Philter's reason. Nothing is changed.</p>
	 *
	 * <p>Each Philter instance caches the settings for up to {@code ADMIN_SETTINGS_CACHE_TTL_SECONDS},
	 * so a change applies at once on the instance that made it and on others when their cache
	 * expires.</p>
	 *
	 * @param request The settings to change.
	 * @return The settings as saved, with any warnings, such as a Phield API key that will be sent over
	 * {@code http}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public AdminSettings updateAdminSettings(UpdateAdminSettingsRequest request) throws IOException {

		final HttpRequest httpRequest = json(uri("/api/settings"))
				.header("Content-Type", APPLICATION_JSON)
				.method("PATCH", text(gson.toJson(request)))
				.build();

		return sendExpectingJson(httpRequest, AdminSettings.class);

	}

	// Users.

	/**
	 * Gets the first page of users. Requires the {@code users:read} scope and an administrator. See
	 * {@link #getUsers(Integer, Integer)}.
	 * @return The page of users and the total.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetUsersResponse getUsers() throws IOException {
		return getUsers(null, null);
	}

	/**
	 * Gets a page of users, sorted by username, including deactivated users.
	 *
	 * <p>Requires the {@code users:read} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown
	 * as a {@link ClientException} carrying that message.</p>
	 *
	 * @param offset The number of users to skip. May be {@code null} for {@code 0}.
	 * @param limit The most users to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The page of users and the total number of users.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetUsersResponse getUsers(Integer offset, Integer limit) throws IOException {

		final HttpRequest request = json(uri("/api/users", "offset", offset, "limit", limit))
				.GET()
				.build();

		return sendExpectingJson(request, GetUsersResponse.class);

	}

	/**
	 * Gets a user by username, active or deactivated.
	 *
	 * <p>Requires the {@code users:read} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown
	 * as a {@link ClientException} carrying that message.</p>
	 *
	 * <p>A username that does not exist is an HTTP 404, thrown as a {@link ClientException}. The
	 * username {@code me} is reserved and returns the calling key's user, as
	 * {@link #getCurrentUser()} does.</p>
	 *
	 * @param username The username.
	 * @return The user.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public User getUser(String username) throws IOException {
		return sendExpectingJson(json(uri("/api/users/" + encode(username))).GET().build(), User.class);
	}

	/**
	 * Gets the user that owns the calling API key, with the deployment's MFA settings, so a user
	 * interface can decide whether to offer MFA enrollment without reading the admin settings.
	 *
	 * <p>Requires the {@code users:read} scope. Does not require an administrator.</p>
	 *
	 * @return The calling key's user, with {@link CurrentUser#isMfaAvailable()} and
	 * {@link CurrentUser#isMfaRequired()}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public CurrentUser getCurrentUser() throws IOException {
		return sendExpectingJson(json(uri("/api/users/me")).GET().build(), CurrentUser.class);
	}

	/**
	 * Creates a user with the {@code user} role. Requires the {@code users:write} scope and an
	 * administrator. See {@link #createUser(CreateUserRequest)}.
	 * @param username The username. Required.
	 * @param email The email address. May be {@code null}.
	 * @return The created {@link CreatedUserResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public CreatedUserResponse createUser(String username, String email) throws IOException {
		return createUser(username, email, null);
	}

	/**
	 * Creates a user. Requires the {@code users:write} scope and an administrator. See
	 * {@link #createUser(CreateUserRequest)}.
	 * @param username The username. Required.
	 * @param email The email address. May be {@code null}.
	 * @param role {@code user} or {@code admin}. May be {@code null} for {@code user}.
	 * @return The created {@link CreatedUserResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public CreatedUserResponse createUser(String username, String email, String role) throws IOException {
		return createUser(username, email, role, null);
	}

	/**
	 * Creates a user with a password, for a person who signs in. The user must change the password at
	 * next sign-in, because an administrator chose it. See {@link #createUser(CreateUserRequest)}.
	 * @param username The username. Required.
	 * @param email The email address. May be {@code null}.
	 * @param role {@code user} or {@code admin}. May be {@code null} for {@code user}.
	 * @param password The password, 16 characters to 72 UTF-8 bytes. May be {@code null} for a user who
	 * can only use API keys.
	 * @return The created {@link CreatedUserResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public CreatedUserResponse createUser(String username, String email, String role, String password)
			throws IOException {

		final CreateUserRequest request = new CreateUserRequest();
		request.setUsername(username);
		request.setEmail(email);
		request.setRole(role);
		request.setPassword(password);

		return createUser(request);

	}

	/**
	 * Creates a user, with a default policy and context. A user without a password authenticates only
	 * with API keys; create one with {@link #createApiKey(String, List)}. A user with a password can also
	 * sign in with {@link #signIn(String, String)}, and must change the password at next sign-in.
	 *
	 * <p>Requires the {@code users:write} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown
	 * as a {@link ClientException} carrying that message.</p>
	 *
	 * <p>A missing or reserved username, a role other than {@code user} or {@code admin}, or a password
	 * shorter than 16 characters or longer than 72 UTF-8 bytes, is an HTTP 400. A username already taken, by an active or a deactivated user, is an HTTP 409. Both are
	 * thrown as a {@link ClientException} carrying Philter's reason.</p>
	 *
	 * @param request The {@link CreateUserRequest}.
	 * @return The created {@link CreatedUserResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public CreatedUserResponse createUser(CreateUserRequest request) throws IOException {

		final HttpRequest httpRequest = json(uri("/api/users"))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(gson.toJson(request)))
				.build();

		return sendExpectingJson(httpRequest, CreatedUserResponse.class);

	}

	/**
	 * Sets a user's role.
	 *
	 * <p>Requires the {@code users:write} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown
	 * as a {@link ClientException} carrying that message.</p>
	 *
	 * <p>A role other than {@code user} or {@code admin} is an HTTP 400, an unknown username an
	 * HTTP 404, and demoting the last active administrator an HTTP 409, each thrown as a
	 * {@link ClientException}.</p>
	 *
	 * @param username The username.
	 * @param role {@code user} or {@code admin}.
	 * @return The user, with its new role.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public User setUserRole(String username, String role) throws IOException {

		final HttpRequest request = json(uri("/api/users/" + encode(username) + "/role"))
				.header("Content-Type", APPLICATION_JSON)
				.PUT(text(gson.toJson(new SetUserRoleRequest(role))))
				.build();

		return sendExpectingJson(request, User.class);

	}

	/**
	 * Deactivates a user. Its API keys stop working, and the user and its data are retained so it can
	 * be reactivated with {@link #reactivateUser(String)}. The username stays reserved.
	 *
	 * <p>Requires the {@code users:write} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown
	 * as a {@link ClientException} carrying that message.</p>
	 *
	 * <p>An unknown username is an HTTP 404. A user that is already deactivated, is the caller, or is
	 * the last active administrator is an HTTP 409. Both are thrown as a {@link ClientException}.</p>
	 *
	 * @param username The username.
	 * @return The user.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public User deactivateUser(String username) throws IOException {

		final HttpRequest request = json(uri("/api/users/" + encode(username) + "/deactivate"))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		return sendExpectingJson(request, User.class);

	}

	/**
	 * Reactivates a deactivated user, restoring its API keys.
	 *
	 * <p>Requires the {@code users:write} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown
	 * as a {@link ClientException} carrying that message.</p>
	 *
	 * <p>An unknown username is an HTTP 404, and a user that is already active an HTTP 409. Both are
	 * thrown as a {@link ClientException}.</p>
	 *
	 * @param username The username.
	 * @return The user.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public User reactivateUser(String username) throws IOException {

		final HttpRequest request = json(uri("/api/users/" + encode(username) + "/reactivate"))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		return sendExpectingJson(request, User.class);

	}

	/**
	 * Changes the calling key's own user's password. Clears a required change. Setting a password revokes
	 * the user's session keys, including the calling key if it is one, so a person then signs in again
	 * with the new password. Long-lived keys are not affected.
	 *
	 * <p>Requires the {@code users:write} scope. Does not require an administrator.</p>
	 *
	 * <p>A missing field, a new password shorter than 16 characters or longer than 72 UTF-8 bytes, or one
	 * the same as the current password is an HTTP 400. A wrong current password is an HTTP 403. A user
	 * with no password, whose first password an administrator sets, is an HTTP 409. Each is thrown as a
	 * {@link ClientException} carrying Philter's reason.</p>
	 *
	 * @param currentPassword The current password.
	 * @param newPassword The new password, 16 characters to 72 UTF-8 bytes.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void changePassword(String currentPassword, String newPassword) throws IOException {

		final HttpRequest request = request(uri("/api/users/me/password"))
				.header("Content-Type", APPLICATION_JSON)
				.PUT(text(gson.toJson(new ChangePasswordRequest(currentPassword, newPassword))))
				.build();

		sendExpectingNoContent(request);

	}

	/**
	 * Sets or resets a user's password, without the current one, and marks it as one the user must
	 * change at next sign-in. It revokes the user's session keys. Long-lived keys are not affected.
	 *
	 * <p>On the calling administrator's own user it sets only the first password, with no change
	 * required: this is how the {@code admin} user gets a password, with the bootstrap API key. Once the
	 * user has a password, change it with {@link #changePassword(String, String)}, which needs the
	 * current one; setting it again here is an HTTP 409.</p>
	 *
	 * <p>Requires the {@code users:write} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown as
	 * a {@link ClientException} carrying that message.</p>
	 *
	 * <p>A missing password, or one shorter than 16 characters or longer than 72 UTF-8 bytes, is an HTTP
	 * 400, and a username that does not exist an HTTP 404. Both are thrown as a
	 * {@link ClientException}.</p>
	 *
	 * @param username The username.
	 * @param password The password, 16 characters to 72 UTF-8 bytes.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void setPassword(String username, String password) throws IOException {

		final HttpRequest request = request(uri("/api/users/" + encode(username) + "/password"))
				.header("Content-Type", APPLICATION_JSON)
				.PUT(text(gson.toJson(new SetPasswordRequest(password))))
				.build();

		sendExpectingNoContent(request);

	}

	/**
	 * Starts MFA enrollment for the calling key's own user: Philter generates a TOTP secret. Show
	 * {@link MfaEnrollment#getOtpauthUri()} as a QR code for an authenticator app to scan, or have the
	 * person type in {@link MfaEnrollment#getSecret()}; neither is returned again. The enrollment does
	 * not apply until {@link #confirmMfaEnrollment(String)}. Starting again replaces an unconfirmed
	 * secret.
	 *
	 * <p>Requires the {@code users:write} scope. Does not require an administrator. Where MFA is not
	 * available on the deployment (the {@code mfaAvailable} admin setting), or the user is already
	 * enrolled, Philter answers HTTP 409, thrown as a {@link ClientException}.</p>
	 *
	 * @return The secret and the {@code otpauth://} URI.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public MfaEnrollment startMfaEnrollment() throws IOException {
		return sendExpectingJson(json(uri("/api/users/me/mfa")).POST(HttpRequest.BodyPublishers.noBody()).build(),
				MfaEnrollment.class);
	}

	/**
	 * Confirms the calling key's own user's MFA enrollment with a code from the authenticator app. It
	 * revokes the user's session keys, so a person signs in again, this time with a code.
	 *
	 * <p>Requires the {@code users:write} scope. Does not require an administrator. A missing or invalid
	 * code is an HTTP 400. MFA not being available, the user already being enrolled, or no enrollment
	 * having been started is an HTTP 409. Both are thrown as a {@link ClientException}.</p>
	 *
	 * @param code The code from the authenticator app.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void confirmMfaEnrollment(String code) throws IOException {
		sendMfaCode("/api/users/me/mfa/confirm", code);
	}

	/**
	 * Removes the calling key's own user's MFA enrollment. It takes a valid code, so a stolen key cannot
	 * turn MFA off, and a bad code counts toward the lock. A person who has lost their device asks an
	 * administrator to use {@link #removeUserMfa(String)}.
	 *
	 * <p>Requires the {@code users:write} scope. Does not require an administrator. A missing code is an
	 * HTTP 400, an invalid code or a locked user an HTTP 403, and a user not enrolled an HTTP 409. Each is
	 * thrown as a {@link ClientException}.</p>
	 *
	 * @param code The code from the authenticator app.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void removeMfaEnrollment(String code) throws IOException {
		sendMfaCode("/api/users/me/mfa/remove", code);
	}

	private void sendMfaCode(final String path, final String code) throws IOException {

		final HttpRequest request = request(uri(path))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(gson.toJson(new MfaCodeRequest(code))))
				.build();

		sendExpectingNoContent(request);

	}

	/**
	 * Removes another user's MFA enrollment, for a person who has lost their device, and clears any lock.
	 * An administrator removes their own with {@link #removeMfaEnrollment(String)}.
	 *
	 * <p>Requires the {@code users:write} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown as
	 * a {@link ClientException} carrying that message.</p>
	 *
	 * <p>A username that does not exist is an HTTP 404, and a user who is the caller or is not enrolled an
	 * HTTP 409. Both are thrown as a {@link ClientException}.</p>
	 *
	 * @param username The username.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void removeUserMfa(String username) throws IOException {
		sendExpectingNoContent(request(uri("/api/users/" + encode(username) + "/mfa")).DELETE().build());
	}

	/**
	 * Unlocks a user whose MFA is locked after five consecutive bad codes, and resets the count.
	 *
	 * <p>Requires the {@code users:write} scope and an administrator. A key without the scope is refused
	 * with an HTTP 403 whose message names the scope; a key that has it but does not belong to an
	 * administrator is refused with an HTTP 403 saying an administrator is required. Both are thrown as
	 * a {@link ClientException} carrying that message.</p>
	 *
	 * <p>A username that does not exist is an HTTP 404, and a user who is not locked an HTTP 409. Both
	 * are thrown as a {@link ClientException}.</p>
	 *
	 * @param username The username.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void unlockUserMfa(String username) throws IOException {
		sendExpectingNoContent(request(uri("/api/users/" + encode(username) + "/mfa/unlock"))
				.POST(HttpRequest.BodyPublishers.noBody()).build());
	}

	// Webhook.

	/**
	 * Gets the caller's webhook. See {@link #getWebhook(String)}.
	 * @return The webhook URL and whether a secret is set.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public Webhook getWebhook() throws IOException {
		return getWebhook(null);
	}

	/**
	 * Gets a user's webhook: its URL and whether a secret is set. The secret is never returned.
	 *
	 * <p>Requires the {@code webhooks:read} scope. Does not require an administrator, except to read
	 * another user's webhook with {@code owner}.</p>
	 *
	 * @param owner The user whose webhook this is. May be {@code null} for the caller's own. Another
	 * user's requires an administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the Philter
	 * deployment; otherwise, or for an owner that does not exist, Philter answers HTTP 404, thrown as a
	 * {@link ClientException}.
	 * @return The webhook. With none set, {@link Webhook#getUrl()} is {@code null} and
	 * {@link Webhook#isSecretSet()} is {@code false}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public Webhook getWebhook(String owner) throws IOException {
		return sendExpectingJson(json(uri("/api/webhook", "owner", owner)).GET().build(), Webhook.class);
	}

	/**
	 * Sets the caller's webhook. See {@link #setWebhook(String, String, String)}.
	 * @param url The URL Philter will POST results to.
	 * @param secret The shared secret Philter signs each delivery with. At least 16 characters.
	 * @return The webhook URL and whether a secret is set.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public Webhook setWebhook(String url, String secret) throws IOException {
		return setWebhook(url, secret, null);
	}

	/**
	 * Sets a user's webhook, replacing any URL and secret already set. Philter POSTs the result of each
	 * asynchronous redaction to the URL, signed with the secret.
	 *
	 * <p>Requires the {@code webhooks:write} scope. Does not require an administrator, except to set
	 * another user's webhook with {@code owner}.</p>
	 *
	 * <p>Philter rejects a missing URL or secret, a URL that is not {@code http} or {@code https},
	 * a host the administrator's webhook destination allowlist does not permit (with no allowlist,
	 * a private or loopback address), and a secret shorter than 16 characters. Each is an HTTP 400,
	 * thrown as a {@link ClientException} carrying Philter's reason. Nothing is saved.</p>
	 *
	 * @param url The URL Philter will POST results to.
	 * @param secret The shared secret Philter signs each delivery with. At least 16 characters.
	 * @param owner The user whose webhook this is. May be {@code null} for the caller's own. Another
	 * user's requires an administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the Philter
	 * deployment; otherwise, or for an owner that does not exist, Philter answers HTTP 404, thrown as a
	 * {@link ClientException}.
	 * @return The webhook URL and whether a secret is set.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public Webhook setWebhook(String url, String secret, String owner) throws IOException {

		final HttpRequest request = json(uri("/api/webhook", "owner", owner))
				.header("Content-Type", APPLICATION_JSON)
				.PUT(text(gson.toJson(new SetWebhookRequest(url, secret))))
				.build();

		return sendExpectingJson(request, Webhook.class);

	}

	/**
	 * Removes the caller's webhook. See {@link #removeWebhook(String)}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void removeWebhook() throws IOException {
		removeWebhook(null);
	}

	/**
	 * Removes a user's webhook URL and secret, so asynchronous results are no longer delivered.
	 * Succeeds when no webhook is set.
	 *
	 * <p>Requires the {@code webhooks:write} scope. Does not require an administrator, except to
	 * remove another user's webhook with {@code owner}.</p>
	 *
	 * @param owner The user whose webhook this is. May be {@code null} for the caller's own. Another
	 * user's requires an administrator and {@code ADMIN_CROSS_USER_ACCESS_ENABLED=true} on the Philter
	 * deployment; otherwise, or for an owner that does not exist, Philter answers HTTP 404, thrown as a
	 * {@link ClientException}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void removeWebhook(String owner) throws IOException {
		sendExpectingNoContent(request(uri("/api/webhook", "owner", owner)).DELETE().build());
	}

	// API keys.

	/**
	 * Gets the first page of the calling key's user's API keys. Requires the {@code api-keys:read}
	 * scope. See {@link #getApiKeys(String, Integer, Integer)}.
	 * @return The page of keys and the total.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetApiKeysResponse getApiKeys() throws IOException {
		return getApiKeys(null, null, null);
	}

	/**
	 * Gets a page of a user's active API keys, oldest first. Each key has its ID, prefix, scopes,
	 * creation time, and whether it is the bootstrap key. The key itself is never returned.
	 *
	 * <p>Requires the {@code api-keys:read} scope. Listing the calling key's own user's keys does not
	 * require an administrator; listing another user's with {@code owner} does. A key that has the
	 * scope but does not belong to an administrator is refused with an HTTP 403 saying an administrator
	 * is required, thrown as a {@link ClientException}. Unlike the other {@code owner} overloads, this
	 * does not also require {@code ADMIN_CROSS_USER_ACCESS_ENABLED}.</p>
	 *
	 * @param owner The username whose keys to list. May be {@code null} for the calling key's user. A
	 * username that does not exist is an HTTP 404, thrown as a {@link ClientException}.
	 * @param offset The number of keys to skip. May be {@code null} for {@code 0}.
	 * @param limit The most keys to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @return The page of keys and the total number of the user's active keys.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetApiKeysResponse getApiKeys(String owner, Integer offset, Integer limit) throws IOException {
		return getApiKeys(owner, offset, limit, null);
	}

	/**
	 * Gets a page of a user's active API keys, oldest first, optionally only session keys or only
	 * long-lived keys. See {@link #getApiKeys(String, Integer, Integer)} for the scope and administrator
	 * rules. The calling session key's own ID is {@link SignInResponse#getId()}.
	 *
	 * @param owner The username whose keys to list. May be {@code null} for the calling key's user. A
	 * username that does not exist is an HTTP 404, thrown as a {@link ClientException}.
	 * @param offset The number of keys to skip. May be {@code null} for {@code 0}.
	 * @param limit The most keys to return, up to 100. May be {@code null} for Philter's default of 25.
	 * @param session {@code true} for only session keys, {@code false} for only long-lived keys, or
	 * {@code null} for both.
	 * @return The page of keys and the total, which counts only the keys the filter lists.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetApiKeysResponse getApiKeys(String owner, Integer offset, Integer limit, Boolean session)
			throws IOException {

		final String path = owner == null ? "/api/api-keys" : "/api/users/" + encode(owner) + "/api-keys";

		final HttpRequest request = json(uri(path, "offset", offset, "limit", limit, "session", session))
				.GET()
				.build();

		return sendExpectingJson(request, GetApiKeysResponse.class);

	}

	/**
	 * Lists every scope an API key can carry, with what each allows, in the order Philter declares them.
	 * Offer these when creating or re-scoping a key with {@link #createApiKey(List)},
	 * {@link #createApiKey(String, List)}, or {@link #setApiKeyScopes(String, List)}, rather than
	 * hard-coding the names: a scope Philter adds later appears here.
	 *
	 * <p>Any valid API key can call this, whatever its scopes, and no administrator is needed. A request
	 * without a valid key is refused with an {@link UnauthorizedException}.</p>
	 *
	 * @return The scopes.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetApiKeyScopesResponse listApiKeyScopes() throws IOException {
		return sendExpectingJson(json(uri("/api/api-keys/scopes")).GET().build(), GetApiKeyScopesResponse.class);
	}

	/**
	 * Creates an API key for the calling key's user. See {@link #createApiKey(CreateApiKeyRequest)}.
	 * @param scopes The scopes to grant the key, for example {@code redact} or {@code policies:read}.
	 *               At least one is required.
	 * @return The created {@link CreatedApiKeyResponse}, carrying the key's value.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public CreatedApiKeyResponse createApiKey(List<String> scopes) throws IOException {

		final CreateApiKeyRequest request = new CreateApiKeyRequest();
		request.setScopes(scopes);

		return createApiKey(request);

	}

	/**
	 * Creates an API key for the calling key's user. To rotate a key, create its replacement with this,
	 * switch the integration to the new key, then revoke the old key with
	 * {@link #revokeApiKey(String)} using the new one.
	 *
	 * <p>Requires the {@code api-keys:write} scope. Does not require an administrator. The requested
	 * scopes must be a subset of those the calling key holds; asking for one it does not hold is an HTTP
	 * 403. No scopes, or a name that is not a scope, is an HTTP 400. Both are thrown as a
	 * {@link ClientException}.</p>
	 *
	 * <p>The key's value is returned only here. Philter stores only its hash, so a value not captured
	 * from the response cannot be recovered.</p>
	 *
	 * @param request The {@link CreateApiKeyRequest}.
	 * @return The created {@link CreatedApiKeyResponse}, carrying the key's value and its ID.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public CreatedApiKeyResponse createApiKey(CreateApiKeyRequest request) throws IOException {

		final HttpRequest httpRequest = json(uri("/api/api-keys"))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(gson.toJson(request)))
				.build();

		return sendExpectingJson(httpRequest, CreatedApiKeyResponse.class);

	}

	/**
	 * Creates an API key for another user. See {@link #createApiKey(String, CreateApiKeyRequest)}.
	 * @param username The user the key will belong to.
	 * @param scopes The scopes to grant the key, for example {@code redact} or {@code policies:read}.
	 *               At least one is required.
	 * @return The created {@link CreatedApiKeyResponse}, carrying the key's value.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public CreatedApiKeyResponse createApiKey(String username, List<String> scopes) throws IOException {

		final CreateApiKeyRequest request = new CreateApiKeyRequest();
		request.setScopes(scopes);

		return createApiKey(username, request);

	}

	/**
	 * Creates an API key for a named user.
	 *
	 * <p>Requires the {@code api-keys:write} scope and an administrator. The requested scopes must be a
	 * subset of those the calling key holds. A user that does not exist or is deactivated is an HTTP
	 * 404, thrown as a {@link ClientException}. To create a key for the calling key's own user, which
	 * does not need an administrator, use {@link #createApiKey(CreateApiKeyRequest)}.</p>
	 *
	 * <p>The key's value is returned only here. Philter stores only its hash, so a value not captured
	 * from the response cannot be recovered.</p>
	 *
	 * @param username The user the key will belong to.
	 * @param request The {@link CreateApiKeyRequest}.
	 * @return The created {@link CreatedApiKeyResponse}, carrying the key's value and its ID.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public CreatedApiKeyResponse createApiKey(String username, CreateApiKeyRequest request) throws IOException {

		final HttpRequest httpRequest = json(uri("/api/users/" + encode(username) + "/api-keys"))
				.header("Content-Type", APPLICATION_JSON)
				.POST(text(gson.toJson(request)))
				.build();

		return sendExpectingJson(httpRequest, CreatedApiKeyResponse.class);

	}

	/**
	 * Replaces an API key's scopes. The key's value does not change, so integrations keep working with
	 * the same credential.
	 *
	 * <p>Requires the {@code api-keys:write} scope. A caller can change its own user's keys; an
	 * administrator can change any user's, by ID, so this has no {@code owner} overload. The new scopes
	 * must be a subset of those the calling key holds, and the calling key cannot change a key holding a
	 * scope it does not hold; either is an HTTP 403. No scopes, or a name that is not a scope, is an
	 * HTTP 400. A key the caller may not manage, including another user's key for a non-administrator,
	 * is an HTTP 404. The key making the request cannot change its own scopes, even to narrow them,
	 * which is an HTTP 409; change them with another key. Each is thrown as a {@link ClientException}.</p>
	 *
	 * <p>Other Philter instances that do not share a cache with the one handling the request can apply
	 * the old scopes for up to {@code API_KEY_CACHE_TTL_SECONDS} (60 by default).</p>
	 *
	 * @param keyId The key's ID, from {@link ApiKey#getId()} or {@link CreatedApiKeyResponse#getId()}.
	 * @param scopes The scopes, which replace the key's current set. At least one is required.
	 * @return The key with its new scopes.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public ApiKey setApiKeyScopes(String keyId, List<String> scopes) throws IOException {

		final HttpRequest request = json(uri("/api/api-keys/" + encode(keyId) + "/scopes"))
				.header("Content-Type", APPLICATION_JSON)
				.PUT(text(gson.toJson(new SetApiKeyScopesRequest(scopes))))
				.build();

		return sendExpectingJson(request, ApiKey.class);

	}

	/**
	 * Revokes an API key. A revoked key cannot be restored.
	 *
	 * <p>Requires the {@code api-keys:write} scope. A caller can revoke its own user's keys; an
	 * administrator can revoke any user's, by ID, so this has no {@code owner} overload. The key making
	 * the request cannot revoke itself, which is an HTTP 409; revoke it with another key. The calling
	 * key cannot revoke a key holding a scope it does not hold, which is an HTTP 403. A key the caller
	 * may not manage, including another user's key for a non-administrator, is an HTTP 404. Each is
	 * thrown as a {@link ClientException}.</p>
	 *
	 * <p>Revoking evicts the key from the cache of the Philter instance that handles the request, and of
	 * every instance sharing its Valkey or Redis cache. Other nodes in a Philter cluster may accept the
	 * revoked key for up to {@code API_KEY_CACHE_TTL_SECONDS} (60 by default).</p>
	 *
	 * @param keyId The key's ID, from {@link ApiKey#getId()} or {@link CreatedApiKeyResponse#getId()}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void revokeApiKey(String keyId) throws IOException {
		sendExpectingNoContent(request(uri("/api/api-keys/" + encode(keyId))).DELETE().build());
	}

	/**
	 * Revokes every session key a user holds, signing the person out everywhere. Long-lived keys are not
	 * affected. Session keys are checked on every request, so the revocation applies at once on every
	 * Philter instance. To revoke one session key, use {@link #revokeApiKey(String)} with its ID from
	 * {@link #getApiKeys(String, Integer, Integer)}.
	 *
	 * <p>Requires the {@code api-keys:write} scope and an administrator. A username that does not exist
	 * is an HTTP 404, thrown as a {@link ClientException}.</p>
	 *
	 * @param username The username.
	 * @return The number of session keys revoked.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public int revokeSessionKeys(String username) throws IOException {

		final HttpRequest request = json(uri("/api/users/" + encode(username) + "/session-keys"))
				.DELETE()
				.build();

		final RevokedSessionKeysResponse response = sendExpectingJson(request, RevokedSessionKeysResponse.class);

		return response.getRevoked();

	}

}
