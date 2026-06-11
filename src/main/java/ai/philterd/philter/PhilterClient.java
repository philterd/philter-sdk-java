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
import ai.philterd.philter.services.PhilterService;

import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client class for Philter's API. Philter finds and manipulates sensitive information in text.
 * This client targets the Philter 4.0.0 API. For more information on Philter see https://www.philterd.ai.
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
public class PhilterClient extends AbstractClient {

	public static final int DEFAULT_TIMEOUT_SEC = 30;
	public static final int DEFAULT_MAX_IDLE_CONNECTIONS = 20;
	public static final int DEFAULT_KEEP_ALIVE_DURATION_MS = 30 * 1000;

	private final PhilterService service;

	/**
	 * Builds {@link PhilterClient} instances. Only {@link #withEndpoint(String)} is required; all other
	 * settings are optional and fall back to sensible defaults.
	 */
	public static class PhilterClientBuilder {

		private String endpoint;
		private OkHttpClient.Builder okHttpClientBuilder;
		private long timeout = DEFAULT_TIMEOUT_SEC;
		private int maxIdleConnections = DEFAULT_MAX_IDLE_CONNECTIONS;
		private int keepAliveDurationMs = DEFAULT_KEEP_ALIVE_DURATION_MS;
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
		 * Supplies a pre-configured OkHttp client builder. When provided, the {@code timeout},
		 * {@code maxIdleConnections}, and {@code keepAliveDurationMs} settings are not applied and should be
		 * configured on the supplied builder instead. The {@code Authorization} header and any SSL
		 * configuration are still applied on top of it.
		 * @param okHttpClientBuilder The OkHttp client builder to use.
		 * @return This builder.
		 */
		public PhilterClientBuilder withOkHttpClientBuilder(OkHttpClient.Builder okHttpClientBuilder) {
			this.okHttpClientBuilder = okHttpClientBuilder;
			return this;
		}

		/**
		 * Sets the connect, read, and write timeout in seconds. Defaults to {@link #DEFAULT_TIMEOUT_SEC}.
		 * Ignored when a client builder is supplied via {@link #withOkHttpClientBuilder(OkHttpClient.Builder)}.
		 * @param timeout The timeout in seconds.
		 * @return This builder.
		 */
		public PhilterClientBuilder withTimeout(long timeout) {
			this.timeout = timeout;
			return this;
		}

		/**
		 * Sets the maximum number of idle connections in the connection pool. Defaults to
		 * {@link #DEFAULT_MAX_IDLE_CONNECTIONS}. Ignored when a client builder is supplied via
		 * {@link #withOkHttpClientBuilder(OkHttpClient.Builder)}.
		 * @param maxIdleConnections The maximum number of idle connections.
		 * @return This builder.
		 */
		public PhilterClientBuilder withMaxIdleConnections(int maxIdleConnections) {
			this.maxIdleConnections = maxIdleConnections;
			return this;
		}

		/**
		 * Sets the connection keep-alive duration in milliseconds. Defaults to
		 * {@link #DEFAULT_KEEP_ALIVE_DURATION_MS}. Ignored when a client builder is supplied via
		 * {@link #withOkHttpClientBuilder(OkHttpClient.Builder)}.
		 * @param keepAliveDurationMs The keep-alive duration in milliseconds.
		 * @return This builder.
		 */
		public PhilterClientBuilder withKeepAliveDurationMs(int keepAliveDurationMs) {
			this.keepAliveDurationMs = keepAliveDurationMs;
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
			return new PhilterClient(endpoint, okHttpClientBuilder, timeout, maxIdleConnections, keepAliveDurationMs, apiKey);
		}

	}

	private PhilterClient(String endpoint, OkHttpClient.Builder okHttpClientBuilder, long timeout, int maxIdleConnections, int keepAliveDurationMs,
	                      String apiKey) {

		if(okHttpClientBuilder == null) {

			okHttpClientBuilder = new OkHttpClient.Builder()
					.connectTimeout(timeout, TimeUnit.SECONDS)
					.writeTimeout(timeout, TimeUnit.SECONDS)
					.readTimeout(timeout, TimeUnit.SECONDS)
					.connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDurationMs, TimeUnit.MILLISECONDS));

		}

		if(StringUtils.isNotEmpty(apiKey)) {
			okHttpClientBuilder.addInterceptor(new AuthorizationInterceptor(apiKey));
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

	/**
	 * Adds the {@code Authorization} header to each outgoing request.
	 */
	private static final class AuthorizationInterceptor implements Interceptor {

		private final String apiKey;

		private AuthorizationInterceptor(final String apiKey) {
			this.apiKey = apiKey;
		}

		@Override
		public okhttp3.Response intercept(final Chain chain) throws IOException {
			final Request request = chain.request().newBuilder()
					.header("Authorization", apiKey)
					.build();
			return chain.proceed(request);
		}

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

		// The text filter is always synchronous so that the filtered text is returned in the response body.
		final Response<String> response = service.filter(context, policyName, null, false, text).execute();

		if(response.isSuccessful()) {

			final String documentId = response.headers().get("x-document-id");
			return new FilterResponse(response.body(), context, documentId);

		} else {

			throw toException(response.code());

		}

	}

	/**
	 * Send a PDF document to Philter to be filtered.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param policyName The name of the policy to apply to the document.
	 * @param filename The name of the file being filtered. May be {@code null}.
	 * @param file The PDF file to be filtered.
	 * @return The filtered document as a ZIP archive.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public BinaryFilterResponse filter(String context, String policyName, String filename, File file) throws IOException {

		final byte[] params = FileUtils.readFileToByteArray(file);
		final RequestBody body = RequestBody.create(MediaType.parse("application/pdf"), params);

		final Response<ResponseBody> response = service.filter(context, policyName, filename, false, body).execute();

		if(response.isSuccessful()) {

			final String documentId = response.headers().get("x-document-id");
			return new BinaryFilterResponse(context, documentId, response.body().bytes());

		} else {

			throw toException(response.code());

		}

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
		return execute(service.explain(context, policyName, null, text));
	}

	/**
	 * Compiles a policy, returning the compiled representation.
	 * @param policy The policy to compile.
	 * @return The compiled policy.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String compilePolicy(String policy) throws IOException {
		return execute(service.compilePolicy(policy));
	}

	/**
	 * Re-identifies previously redacted values.
	 * @param owner The owner of the values. May be {@code null}.
	 * @param request The {@link ReidentifyRequest}.
	 * @return The re-identification result.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String reidentify(String owner, ReidentifyRequest request) throws IOException {
		return execute(service.reidentify(owner, request));
	}

	// Status.

	/**
	 * Gets the health of Philter. This endpoint does not require authentication.
	 * @return The {@link StatusResponse}.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public StatusResponse health() throws IOException {
		return execute(service.health());
	}

	/**
	 * Gets the status of Philter.
	 * @return The {@link StatusResponse}.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public StatusResponse status() throws IOException {
		return execute(service.status());
	}

	/**
	 * Gets Philter's public signing key.
	 * @return The signing key.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public String getSigningKey() throws IOException {
		return execute(service.signingKey());
	}

	// Policies.

	/**
	 * Gets a list of policy names.
	 * @return A list of policy names.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<String> getPolicies() throws IOException {
		return execute(service.getPolicies(null, null, null));
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
		return execute(service.getPolicies(owner, offset, limit));
	}

	/**
	 * Gets the content of a policy.
	 * @param policyName The name of the policy to get.
	 * @return The content of the policy as JSON.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getPolicy(String policyName) throws IOException {
		return execute(service.getPolicy(policyName, null));
	}

	/**
	 * Saves (or overwrites) the policy.
	 * @param name The name of the policy.
	 * @param json The body of the policy.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void savePolicy(String name, String json) throws IOException {
		executeVoid(service.savePolicy(name, null, json));
	}

	/**
	 * Deletes a policy.
	 * @param policyName The name of the policy to delete.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deletePolicy(String policyName) throws IOException {
		executeVoid(service.deletePolicy(policyName, null));
	}

	/**
	 * Gets the revision history for a policy.
	 * @param policyName The name of the policy.
	 * @return A list of {@link PolicyVersionSummary}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<PolicyVersionSummary> getPolicyVersions(String policyName) throws IOException {
		return execute(service.getPolicyVersions(policyName, null, null, null));
	}

	/**
	 * Gets a specific revision of a policy.
	 * @param policyName The name of the policy.
	 * @param revision The revision number.
	 * @return The policy content as JSON.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getPolicyVersion(String policyName, int revision) throws IOException {
		return execute(service.getPolicyVersion(policyName, revision, null));
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
		return execute(service.getPolicyDiff(policyName, from, to, null));
	}

	/**
	 * Rolls a policy back to a prior revision.
	 * @param policyName The name of the policy.
	 * @param revision The revision number to roll back to.
	 * @return The {@link PolicyRollbackResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public PolicyRollbackResponse rollbackPolicy(String policyName, int revision) throws IOException {
		return execute(service.rollbackPolicy(policyName, revision, null));
	}

	// Contexts.

	/**
	 * Gets the configured contexts.
	 * @return The contexts.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getContexts() throws IOException {
		return execute(service.getContexts(null, null, null));
	}

	/**
	 * Creates a context.
	 * @param name The name of the context.
	 * @param entityTypeDisambiguation Whether entity type disambiguation is enabled. May be {@code null}.
	 * @param ledger Whether the redaction ledger is enabled. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse createContext(String name, Boolean entityTypeDisambiguation, Boolean ledger) throws IOException {
		return execute(service.createContext(name, entityTypeDisambiguation, ledger));
	}

	/**
	 * Gets a context by name.
	 * @param name The name of the context.
	 * @return The context.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getContext(String name) throws IOException {
		return execute(service.getContext(name));
	}

	/**
	 * Updates a context.
	 * @param name The name of the context.
	 * @param entityTypeDisambiguation Whether entity type disambiguation is enabled. May be {@code null}.
	 * @param ledger Whether the redaction ledger is enabled. May be {@code null}.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse updateContext(String name, Boolean entityTypeDisambiguation, Boolean ledger) throws IOException {
		return execute(service.updateContext(name, entityTypeDisambiguation, ledger));
	}

	/**
	 * Deletes a context.
	 * @param name The name of the context.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteContext(String name) throws IOException {
		return execute(service.deleteContext(name));
	}

	/**
	 * Gets the entries for a context.
	 * @param name The name of the context.
	 * @return The context entries.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getContextEntries(String name) throws IOException {
		return execute(service.getContextEntries(name, null, null));
	}

	/**
	 * Deletes all entries for a context.
	 * @param name The name of the context.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteContextEntries(String name) throws IOException {
		return execute(service.deleteContextEntries(name));
	}

	/**
	 * Exports the entries for a context.
	 * @param name The name of the context.
	 * @param owner The owner. May be {@code null}.
	 * @return The exported entries.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String exportContextEntries(String name, String owner) throws IOException {
		return execute(service.exportContextEntries(name, owner));
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
		return execute(service.importContextEntries(name, onConflict, owner, json));
	}

	/**
	 * Deletes a single entry from a context.
	 * @param name The name of the context.
	 * @param entryId The entry ID.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse deleteContextEntry(String name, String entryId) throws IOException {
		return execute(service.deleteContextEntry(name, entryId));
	}

	// Documents.

	/**
	 * Gets the stored documents.
	 * @return The documents.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getDocuments() throws IOException {
		return execute(service.getDocuments(null, null, null));
	}

	/**
	 * Gets a stored document by ID.
	 * @param documentId The document ID.
	 * @return The document bytes.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public byte[] getDocument(String documentId) throws IOException {

		final Response<ResponseBody> response = service.getDocument(documentId, null).execute();

		if(response.isSuccessful()) {
			return response.body().bytes();
		} else {
			throw toException(response.code());
		}

	}

	/**
	 * Deletes a stored document.
	 * @param documentId The document ID.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteDocument(String documentId) throws IOException {
		executeVoid(service.deleteDocument(documentId, null));
	}

	/**
	 * Gets the processing status of a document.
	 * @param documentId The document ID.
	 * @return The document status.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getDocumentStatus(String documentId) throws IOException {
		return execute(service.getDocumentStatus(documentId, null));
	}

	// Legal holds.

	/**
	 * Gets the legal holds.
	 * @return A list of {@link LegalHoldResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public List<LegalHoldResponse> getHolds() throws IOException {
		return execute(service.getHolds(null, null, null));
	}

	/**
	 * Creates a legal hold.
	 * @param request The {@link LegalHoldRequest}.
	 * @return The created {@link LegalHoldResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LegalHoldResponse createHold(LegalHoldRequest request) throws IOException {
		return execute(service.createHold(null, request));
	}

	/**
	 * Gets a legal hold by reference.
	 * @param reference The legal hold reference.
	 * @return The {@link LegalHoldResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public LegalHoldResponse getHold(String reference) throws IOException {
		return execute(service.getHold(reference, null));
	}

	/**
	 * Deletes a legal hold.
	 * @param reference The legal hold reference.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteHold(String reference) throws IOException {
		executeVoid(service.deleteHold(reference, null));
	}

	// Redaction ledger.

	/**
	 * Queries the redaction ledger.
	 * @param query The query. May be {@code null}.
	 * @return The matching ledger entries.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getLedger(String query) throws IOException {
		return execute(service.getLedger(query, null, null, null));
	}

	/**
	 * Gets the ledger entry for a document.
	 * @param documentId The document ID.
	 * @return The ledger entry.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getLedgerEntry(String documentId) throws IOException {
		return execute(service.getLedgerEntry(documentId, null));
	}

	/**
	 * Exports the ledger entry for a document.
	 * @param documentId The document ID.
	 * @return The exported ledger entry.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String exportLedger(String documentId) throws IOException {
		return execute(service.exportLedger(documentId, null));
	}

	/**
	 * Checks whether the ledger for a document is valid.
	 * @param documentId The document ID.
	 * @return The validity result.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String isLedgerValid(String documentId) throws IOException {
		return execute(service.isLedgerValid(documentId, null));
	}

	// Custom lists.

	/**
	 * Gets the custom lists.
	 * @return The custom lists.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getLists() throws IOException {
		return execute(service.getLists(null));
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
		return execute(service.saveList(list, description, null, values));
	}

	/**
	 * Deletes a custom list.
	 * @param list The name of the list.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public void deleteList(String list) throws IOException {
		executeVoid(service.deleteList(list, null));
	}

	/**
	 * Gets the values of a custom list.
	 * @param name The name of the list.
	 * @return The {@link GetListsResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GetListsResponse getList(String name) throws IOException {
		return execute(service.getList(name, null));
	}

	// Redact lists.

	/**
	 * Gets the redact lists.
	 * @return The redact lists.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public String getRedactLists() throws IOException {
		return execute(service.getRedactLists(null));
	}

	/**
	 * Creates a redact list.
	 * @param json The redact list as JSON.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse createRedactList(String json) throws IOException {
		return execute(service.createRedactList(null, json));
	}

	/**
	 * Updates a redact list.
	 * @param json The redact list as JSON.
	 * @return A {@link GenericResponse}.
	 * @throws IOException Thrown if the call can not be executed.
	 */
	public GenericResponse updateRedactList(String json) throws IOException {
		return execute(service.updateRedactList(null, json));
	}

}
