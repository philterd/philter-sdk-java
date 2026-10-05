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
package ai.philterd;

import ai.philterd.philter.PhilterClient;
import ai.philterd.philter.model.AdminSettings;
import ai.philterd.philter.model.AuditLogExport;
import ai.philterd.philter.model.BinaryFilterResponse;
import ai.philterd.philter.model.CreateApiKeyRequest;
import ai.philterd.philter.model.CreateUserRequest;
import ai.philterd.philter.model.CreatedApiKeyResponse;
import ai.philterd.philter.model.CreatedUserResponse;
import ai.philterd.philter.model.ExplainResponse;
import ai.philterd.philter.model.FilterResponse;
import ai.philterd.philter.model.GenericResponse;
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
import ai.philterd.philter.model.StatusResponse;
import ai.philterd.philter.model.UpdateAdminSettingsRequest;
import ai.philterd.philter.model.User;
import ai.philterd.philter.model.Webhook;
import ai.philterd.philter.model.exceptions.ClientException;
import ai.philterd.philter.model.exceptions.ServiceUnavailableException;
import ai.philterd.philter.model.exceptions.UnauthorizedException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Exercises {@link PhilterClient} against a local {@link HttpServer}, asserting both the request
 * that goes onto the wire and the response the client hands back.
 */
public class PhilterClientMockTest {

    private HttpServer server;
    private ExecutorService serverExecutor;

    /** The request the server last received. */
    private volatile String method;
    private volatile String path;
    private volatile String rawPath;
    private volatile String rawQuery;
    private volatile Map<String, String> queryParameters = new HashMap<>();
    private volatile Map<String, String> requestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private volatile byte[] requestBody = new byte[0];

    /** The response the server will next return. */
    private volatile int status = 200;
    private volatile byte[] responseBody = new byte[0];
    private volatile String documentIdHeader;
    private volatile String documentIdHeaderName = "x-document-id";
    private volatile String locationHeader;
    private volatile String contentTypeHeader;
    private final Map<String, String> responseHeaders = new HashMap<>();

    /** Held closed by a test that wants the server to stall; opened in teardown. */
    private final CountDownLatch released = new CountDownLatch(1);
    private volatile boolean holdResponse;

    @Before
    public void setUp() throws Exception {

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        serverExecutor = Executors.newFixedThreadPool(4);
        server.setExecutor(serverExecutor);

        server.createContext("/", (HttpExchange exchange) -> {

            method = exchange.getRequestMethod();
            path = exchange.getRequestURI().getPath();
            rawPath = exchange.getRequestURI().getRawPath();
            rawQuery = exchange.getRequestURI().getRawQuery();
            queryParameters = parseQuery(exchange.getRequestURI().getRawQuery());
            requestHeaders = copyHeaders(exchange.getRequestHeaders());
            requestBody = exchange.getRequestBody().readAllBytes();

            if (holdResponse) {
                try {
                    released.await(10, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                exchange.close();
                return;
            }

            if (documentIdHeader != null) {
                exchange.getResponseHeaders().add(documentIdHeaderName, documentIdHeader);
            }

            if (contentTypeHeader != null) {
                exchange.getResponseHeaders().add("Content-Type", contentTypeHeader);
            }

            if (locationHeader != null) {
                exchange.getResponseHeaders().add("Location", locationHeader);
            }

            responseHeaders.forEach(exchange.getResponseHeaders()::add);

            // A response with no body must be sent with a length of -1.
            if (responseBody.length == 0) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                exchange.sendResponseHeaders(status, responseBody.length);
                exchange.getResponseBody().write(responseBody);
            }

            exchange.close();

        });

        server.start();

    }

    @After
    public void tearDown() {
        released.countDown();
        server.stop(0);
        serverExecutor.shutdownNow();
    }

    private static Map<String, String> parseQuery(final String rawQuery) {

        final Map<String, String> parameters = new HashMap<>();

        if (rawQuery == null || rawQuery.isEmpty()) {
            return parameters;
        }

        for (final String pair : rawQuery.split("&")) {
            final int separator = pair.indexOf('=');
            final String name = separator < 0 ? pair : pair.substring(0, separator);
            final String value = separator < 0 ? "" : pair.substring(separator + 1);
            parameters.put(URLDecoder.decode(name, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }

        return parameters;

    }

    private static Map<String, String> copyHeaders(final Headers headers) {

        final Map<String, String> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        headers.forEach((name, values) -> {
            if (!values.isEmpty()) {
                copy.put(name, values.get(0));
            }
        });

        return copy;

    }

    private PhilterClient client() {
        return clientBuilder().build();
    }

    private PhilterClient.PhilterClientBuilder clientBuilder() {
        // Deliberately no trailing slash: Retrofit rejected this, the JDK client does not.
        return new PhilterClient.PhilterClientBuilder()
                .withEndpoint("http://localhost:" + server.getAddress().getPort());
    }

    private void respond(final int status, final String body) {
        respond(status, body.getBytes(StandardCharsets.UTF_8));
    }

    private void respond(final int status, final byte[] body) {
        this.status = status;
        this.responseBody = body;
    }

    private String queryParameter(final String name) {
        return queryParameters.get(name);
    }

    private String header(final String name) {
        return requestHeaders.get(name);
    }

    private String requestBodyAsString() {
        return new String(requestBody, StandardCharsets.UTF_8);
    }

    // Filtering.

    @Test
    public void filterText() throws Exception {

        respond(200, "My SSN is {{{REDACTED-ssn}}}.");
        documentIdHeader = "doc-123";

        final FilterResponse response = client().filter("ctx", "default", "My SSN is 123-45-6789.");

        Assert.assertEquals("My SSN is {{{REDACTED-ssn}}}.", response.getFilteredText());
        Assert.assertEquals("ctx", response.getContext());
        Assert.assertEquals("doc-123", response.getDocumentId());

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/filter", path);
        Assert.assertEquals("ctx", queryParameter("c"));
        Assert.assertEquals("default", queryParameter("p"));
        // Philter's text endpoint has no async parameter, so none is sent.
        Assert.assertNull(queryParameter("async"));
        Assert.assertNull(queryParameter("filename"));
        Assert.assertEquals("text/plain", header("Content-Type"));
        Assert.assertEquals("text/plain", header("Accept"));
        Assert.assertEquals("My SSN is 123-45-6789.", requestBodyAsString());
    }

    @Test
    public void filterPdf() throws Exception {

        final byte[] zipBytes = new byte[]{0x50, 0x4b, 0x03, 0x04, 0x01, 0x02};

        respond(200, zipBytes);
        documentIdHeader = "doc-pdf";

        final BinaryFilterResponse response = client().filter("ctx", "default", "test.pdf", pdfFile());

        Assert.assertArrayEquals(zipBytes, response.getContent());
        Assert.assertEquals("doc-pdf", response.getDocumentId());

        Assert.assertEquals("/api/filter", path);
        Assert.assertEquals("test.pdf", queryParameter("filename"));
        // The PDF endpoint defaults to asynchronous, so a waiting call must opt out explicitly.
        Assert.assertEquals("false", queryParameter("async"));
        Assert.assertEquals("application/pdf", header("Content-Type"));
        Assert.assertEquals("application/zip", header("Accept"));
        Assert.assertArrayEquals(new byte[]{1, 2, 3}, requestBody);
    }

    @Test
    public void filterPdfToPdf() throws Exception {

        respond(200, new byte[]{0x25, 0x50, 0x44, 0x46});

        final BinaryFilterResponse response = client().filterToPdf("ctx", "default", "test.pdf", pdfFile());

        Assert.assertArrayEquals(new byte[]{0x25, 0x50, 0x44, 0x46}, response.getContent());

        // The requested output format is what distinguishes this from the ZIP call.
        Assert.assertEquals("application/pdf", header("Accept"));
        Assert.assertEquals("false", queryParameter("async"));
    }

    @Test
    public void filterPdfAsynchronously() throws Exception {

        respond(202, "{\"documentId\":\"doc-async\"}");

        Assert.assertEquals("doc-async", client().filterAsync("ctx", "default", "test.pdf", pdfFile()));

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/filter", path);
        Assert.assertEquals("true", queryParameter("async"));
        Assert.assertEquals("application/zip", header("Accept"));
    }

    @Test
    public void filterPdfToPdfAsynchronously() throws Exception {

        respond(202, "{\"documentId\":\"doc-async-pdf\"}");

        Assert.assertEquals("doc-async-pdf", client().filterToPdfAsync("ctx", "default", "test.pdf", pdfFile()));

        Assert.assertEquals("true", queryParameter("async"));
        Assert.assertEquals("application/pdf", header("Accept"));
    }

    @Test
    public void filenameIsSentOnTextRequests() throws Exception {

        respond(200, "filtered");
        client().filter("ctx", "default", "notes.txt", "My SSN is 123-45-6789.");
        Assert.assertEquals("notes.txt", queryParameter("filename"));

        respond(200, "{}");
        client().explain("ctx", "default", "notes.txt", "My SSN is 123-45-6789.");
        Assert.assertEquals("/api/explain", path);
        Assert.assertEquals("notes.txt", queryParameter("filename"));
    }

    private File pdfFile() throws Exception {
        final File file = File.createTempFile("philter-test", ".pdf");
        Files.write(file.toPath(), new byte[]{1, 2, 3});
        file.deleteOnExit();
        return file;
    }

    @Test
    public void explain() throws Exception {

        respond(200, "{\"filteredText\":\"redacted\",\"context\":\"ctx\",\"documentId\":\"doc-1\"," +
                "\"explanation\":{\"appliedSpans\":[{\"filterType\":\"ssn\",\"characterStart\":0,\"characterEnd\":11}]," +
                "\"ignoredSpans\":[]}}");

        final ExplainResponse response = client().explain("ctx", "default", "My SSN is 123-45-6789.");

        Assert.assertEquals("redacted", response.getFilteredText());
        Assert.assertEquals("doc-1", response.getDocumentId());
        Assert.assertNotNull(response.getExplanation());
        Assert.assertEquals(1, response.getExplanation().getAppliedSpans().size());
        Assert.assertEquals("ssn", response.getExplanation().getAppliedSpans().get(0).getFilterType());

        Assert.assertEquals("/api/explain", path);
    }

    // Authentication.

    @Test
    public void authorizationHeaderIsSent() throws Exception {

        respond(200, "[]");

        clientBuilder().withApiKey("secret-key").build().getPolicies();

        Assert.assertEquals("secret-key", header("Authorization"));
    }

    @Test
    public void noAuthorizationHeaderWhenApiKeyAbsent() throws Exception {

        respond(200, "[]");

        client().getPolicies();

        Assert.assertNull(header("Authorization"));
    }

    @Test
    public void suppliedHttpClientBuilderIsUsedAndAuthIsLayeredOnTop() throws Exception {

        // A redirect the client is told not to follow proves the supplied builder is actually
        // used: the default client follows redirects, so it would not surface the 302. The API
        // key proves the Authorization header is still applied on top of the supplied builder.
        respond(302, "");
        locationHeader = "/api/policies-moved";

        final PhilterClient client = clientBuilder()
                .withHttpClientBuilder(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER))
                .withApiKey("secret-key")
                .build();

        try {
            client.getPolicies();
            Assert.fail("The redirect should not have been followed.");
        } catch (final ClientException expected) {
            Assert.assertEquals("Unknown error: HTTP 302", expected.getMessage());
        }

        Assert.assertEquals("secret-key", header("Authorization"));
    }

    // Status.

    @Test
    public void health() throws Exception {

        respond(200, "{\"applicationVersion\":\"4.0.0\",\"gitCommit\":\"abc123\"," +
                "\"redactionPolicySchemaVersion\":\"1\",\"status\":\"healthy\"}");

        final StatusResponse status = client().health();

        Assert.assertEquals("4.0.0", status.getApplicationVersion());
        Assert.assertEquals("healthy", status.getStatus());

        Assert.assertEquals("/api/health", path);
    }

    // Policies.

    @Test
    public void getPolicies() throws Exception {

        respond(200, "[\"default\",\"strict\"]");

        final List<String> policies = client().getPolicies();

        Assert.assertEquals(2, policies.size());
        Assert.assertTrue(policies.contains("default"));
        Assert.assertTrue(policies.contains("strict"));
    }

    @Test
    public void savePolicy() throws Exception {

        respond(201, "");

        client().savePolicy("default", "{\"name\":\"default\"}");

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/policies", path);
        Assert.assertEquals("default", queryParameter("name"));
        Assert.assertEquals("{\"name\":\"default\"}", requestBodyAsString());
        // Left out, so an existing policy keeps its description and notes.
        Assert.assertFalse(queryParameters.containsKey("description"));
        Assert.assertFalse(queryParameters.containsKey("notes"));
    }

    @Test
    public void savePolicyWithDescriptionAndNotes() throws Exception {

        respond(201, "");

        client().savePolicy("court", "{\"identifiers\":{}}", "Federal court filings", "Line one\nline two");

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/policies", path);
        Assert.assertEquals("court", queryParameter("name"));
        Assert.assertEquals("Federal court filings", queryParameter("description"));
        Assert.assertEquals("Line one\nline two", queryParameter("notes"));
        Assert.assertFalse(queryParameters.containsKey("owner"));
        Assert.assertEquals("application/json", header("Content-Type"));
        Assert.assertEquals("{\"identifiers\":{}}", requestBodyAsString());

        client().savePolicy("court", "{\"identifiers\":{}}", null, "Only the notes", OWNER);

        Assert.assertFalse(queryParameters.containsKey("description"));
        Assert.assertEquals("Only the notes", queryParameter("notes"));
        Assert.assertEquals(OWNER, queryParameter("owner"));
    }

    @Test
    public void getManagedPolicies() throws Exception {

        respond(200, "[\"managed_common_pii\",\"managed_hipaa\"]");

        Assert.assertEquals(List.of("managed_common_pii", "managed_hipaa"), client().getManagedPolicies(25, 50));

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/policies", path);
        Assert.assertEquals("true", queryParameter("managed"));
        Assert.assertEquals("25", queryParameter("offset"));
        Assert.assertEquals("50", queryParameter("limit"));
        Assert.assertFalse(queryParameters.containsKey("owner"));

        respond(200, "[]");
        client().getManagedPolicies();
        Assert.assertEquals(Map.of("managed", "true"), queryParameters);
    }

    @Test
    public void getPolicyDetailsMapsEveryField() throws Exception {

        respond(200, "{\"name\":\"court\",\"description\":\"Federal court filings\",\"notes\":\"Reviewed with the clerk.\","
                + "\"revision\":3,\"managed\":false,\"created\":\"2026-10-01T14:03:11.000Z\","
                + "\"lastUpdated\":\"2026-10-05T09:12:40.000Z\"}");

        final PolicyDetails details = client().getPolicyDetails("court");

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/policies/court/details", path);
        Assert.assertTrue(queryParameters.isEmpty());

        Assert.assertEquals("court", details.getName());
        Assert.assertEquals("Federal court filings", details.getDescription());
        Assert.assertEquals("Reviewed with the clerk.", details.getNotes());
        Assert.assertEquals(3, details.getRevision());
        Assert.assertFalse(details.isManaged());
        Assert.assertEquals("2026-10-01T14:03:11.000Z", details.getCreated());
        Assert.assertEquals("2026-10-05T09:12:40.000Z", details.getLastUpdated());
    }

    @Test
    public void setPolicyDetailsSendsOnlyTheGivenFields() throws Exception {

        respond(200, "{\"name\":\"court\",\"description\":\"Federal court filings\",\"notes\":\"Reviewed with the clerk.\","
                + "\"revision\":3,\"managed\":false,\"created\":\"2026-10-01T14:03:11.000Z\","
                + "\"lastUpdated\":\"2026-10-05T09:12:40.000Z\"}");

        final PolicyDetails details = client().setPolicyDetails("court", "Federal court filings", "");

        Assert.assertEquals("PUT", method);
        Assert.assertEquals("/api/policies/court/details", path);
        Assert.assertEquals("application/json", header("Content-Type"));
        // An empty string clears the notes, so it is sent.
        Assert.assertEquals("{\"description\":\"Federal court filings\",\"notes\":\"\"}", requestBodyAsString());
        Assert.assertEquals("court", details.getName());

        client().setPolicyDetails("court", null, "Reviewed.", OWNER);

        // A null field is left out, so Philter leaves it as it is.
        Assert.assertEquals("{\"notes\":\"Reviewed.\"}", requestBodyAsString());
        Assert.assertEquals(OWNER, queryParameter("owner"));
    }

    @Test
    public void copyPolicyFromAManagedPolicy() throws Exception {

        respond(201, "{\"name\":\"my-pii\",\"description\":\"Common PII\","
                + "\"notes\":\"Created from managed policy managed_common_pii\",\"revision\":1,\"managed\":false}");

        final PolicyDetails copy = client().copyPolicy("managed_common_pii", "my-pii");

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/policies/managed_common_pii/copy", path);
        Assert.assertEquals(Map.of("name", "my-pii"), queryParameters);
        Assert.assertEquals(0, requestBody.length);

        Assert.assertEquals("my-pii", copy.getName());
        Assert.assertFalse(copy.isManaged());
        Assert.assertEquals("Created from managed policy managed_common_pii", copy.getNotes());
    }

    @Test
    public void setPolicyDetailsOnAManagedPolicyIsAClientException() {

        respond(409, "{\"message\":\"Managed policies cannot be changed.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().setPolicyDetails("managed_common_pii", null, "n"));

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("409"));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("Managed policies cannot be changed."));
    }

    @Test
    public void deletePolicy() throws Exception {

        respond(200, "");

        client().deletePolicy("default");

        Assert.assertEquals("DELETE", method);
        Assert.assertEquals("/api/policies/default", path);
    }

    @Test
    public void getPolicyVersions() throws Exception {

        respond(200, "[{\"capturedTimestamp\":\"2026-01-01T00:00:00Z\",\"contentHash\":\"hash1\",\"revision\":1}]");

        final List<PolicyVersionSummary> versions = client().getPolicyVersions("default");

        Assert.assertEquals(1, versions.size());
        Assert.assertEquals(1, versions.get(0).getRevision());
        Assert.assertEquals("hash1", versions.get(0).getContentHash());
        Assert.assertEquals("/api/policies/default/versions", path);
    }

    @Test
    public void rollbackPolicy() throws Exception {

        respond(201, "{\"revision\":3}");

        final PolicyRollbackResponse response = client().rollbackPolicy("default", 2);

        Assert.assertEquals(3, response.getRevision());

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/policies/default/rollback", path);
        Assert.assertEquals("2", queryParameter("revision"));
    }

    // Contexts.

    @Test
    public void createContext() throws Exception {

        respond(200, "{\"message\":\"created\"}");

        final GenericResponse response = client().createContext("ctx", true, false);

        Assert.assertEquals("created", response.getMessage());

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/contexts", path);
        Assert.assertEquals("ctx", queryParameter("name"));
        Assert.assertEquals("true", queryParameter("entity_type_disambiguation"));
        Assert.assertEquals("false", queryParameter("ledger"));
    }

    // Legal holds.

    @Test
    public void createHold() throws Exception {

        respond(201, "{\"reference\":\"ref-1\",\"reason\":\"litigation\",\"scopeType\":\"document_chain\"," +
                "\"scopeValue\":\"doc-1\",\"setAt\":\"2026-01-01T00:00:00Z\"}");

        final LegalHoldRequest holdRequest = new LegalHoldRequest();
        holdRequest.setReference("ref-1");
        holdRequest.setReason("litigation");
        holdRequest.setScopeType("document_chain");
        holdRequest.setScopeValue("doc-1");

        final LegalHoldResponse response = client().createHold(holdRequest);

        Assert.assertEquals("ref-1", response.getReference());
        Assert.assertEquals("litigation", response.getReason());
        Assert.assertEquals("2026-01-01T00:00:00Z", response.getSetAt());

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/holds", path);
        Assert.assertTrue(requestBodyAsString().contains("\"reference\":\"ref-1\""));
    }

    // Custom lists.

    @Test
    public void getList() throws Exception {

        respond(200, "{\"lists\":[\"alpha\",\"beta\"]}");

        final GetListsResponse response = client().getList("my-list");

        Assert.assertEquals(2, response.getLists().size());
        Assert.assertTrue(response.getLists().contains("alpha"));

        Assert.assertEquals("/api/lists/my-list", path);
    }

    @Test
    public void saveList() throws Exception {

        respond(201, "{\"message\":\"saved\"}");

        final GenericResponse response = client().saveList("my-list", "a description", List.of("alpha", "beta"));

        Assert.assertEquals("saved", response.getMessage());

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/lists/my-list", path);
        Assert.assertEquals("a description", queryParameter("description"));
        Assert.assertEquals("[\"alpha\",\"beta\"]", requestBodyAsString());
    }

    // Users.

    @Test
    public void createUser() throws Exception {

        respond(201, "{\"username\":\"ci\",\"role\":\"user\"}");

        final CreatedUserResponse response = client().createUser("ci", "ci@example.com");

        Assert.assertEquals("ci", response.getUsername());
        Assert.assertEquals("user", response.getRole());

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/users", path);
        Assert.assertEquals("application/json", header("Content-Type"));
        Assert.assertEquals("application/json", header("Accept"));

        // No role means Philter's default, and Philter refuses any password, so neither is sent.
        Assert.assertEquals("{\"username\":\"ci\",\"email\":\"ci@example.com\"}", requestBodyAsString());
    }

    @Test
    public void createUserWithRole() throws Exception {

        respond(201, "{\"username\":\"ops\",\"role\":\"admin\"}");

        Assert.assertEquals("admin", client().createUser("ops", null, "admin").getRole());

        // An omitted email is left out of the body rather than sent as null.
        Assert.assertEquals("{\"username\":\"ops\",\"role\":\"admin\"}", requestBodyAsString());
    }

    @Test
    public void createUserWithRequest() throws Exception {

        respond(201, "{\"username\":\"ci\",\"role\":\"user\"}");

        final CreateUserRequest request = new CreateUserRequest();
        request.setUsername("ci");

        Assert.assertEquals("ci", client().createUser(request).getUsername());

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/users", path);
        Assert.assertEquals("{\"username\":\"ci\"}", requestBodyAsString());
    }

    @Test
    public void createUserConflict() {

        respond(409, "{\"message\":\"That username is taken.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().createUser("ci", null));

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("409"));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("That username is taken."));
    }

    @Test
    public void getUsersMapsThePageAndTotal() throws Exception {

        respond(200, "{\"users\":[{\"username\":\"alice\",\"email\":\"alice@example.com\",\"role\":\"admin\","
                + "\"active\":true,\"created\":\"2026-10-01T12:00:00.000+00:00\"},"
                + "{\"username\":\"bob\",\"role\":\"user\",\"active\":false,"
                + "\"created\":\"2026-10-02T12:00:00.000+00:00\",\"deactivatedAt\":\"2026-10-04T12:00:00.000+00:00\"}],"
                + "\"total\":30}");

        final GetUsersResponse response = client().getUsers(25, 50);

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/users", path);
        Assert.assertEquals("25", queryParameter("offset"));
        Assert.assertEquals("50", queryParameter("limit"));
        Assert.assertEquals("application/json", header("Accept"));

        Assert.assertEquals(30, response.getTotal());
        Assert.assertEquals(2, response.getUsers().size());

        final User alice = response.getUsers().get(0);
        Assert.assertEquals("alice", alice.getUsername());
        Assert.assertEquals("alice@example.com", alice.getEmail());
        Assert.assertEquals("admin", alice.getRole());
        Assert.assertTrue(alice.isActive());
        Assert.assertEquals("2026-10-01T12:00:00.000+00:00", alice.getCreated());
        Assert.assertNull(alice.getDeactivatedAt());

        final User bob = response.getUsers().get(1);
        Assert.assertNull(bob.getEmail());
        Assert.assertFalse(bob.isActive());
        Assert.assertEquals("2026-10-04T12:00:00.000+00:00", bob.getDeactivatedAt());
    }

    @Test
    public void getUsersShortFormSendsNoPaging() throws Exception {

        respond(200, "{\"users\":[],\"total\":0}");

        Assert.assertEquals(0, client().getUsers().getTotal());

        Assert.assertEquals("/api/users", path);
        Assert.assertTrue(queryParameters.isEmpty());
    }

    @Test
    public void getUserEncodesTheUsername() throws Exception {

        respond(200, "{\"username\":\"ci\",\"email\":\"ci@example.com\",\"role\":\"user\",\"active\":true,"
                + "\"created\":\"2026-10-05T12:00:00.000+00:00\"}");

        final User user = client().getUser("ci user");

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/users/ci%20user", rawPath);
        Assert.assertEquals("ci", user.getUsername());
        Assert.assertTrue(user.isActive());
    }

    @Test
    public void getCurrentUser() throws Exception {

        respond(200, "{\"username\":\"ci\",\"email\":\"ci@example.com\",\"role\":\"user\",\"active\":true,"
                + "\"created\":\"2026-10-05T12:00:00.000+00:00\"}");

        Assert.assertEquals("ci", client().getCurrentUser().getUsername());

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/users/me", path);
    }

    @Test
    public void setUserRole() throws Exception {

        respond(200, "{\"username\":\"ci\",\"role\":\"admin\",\"active\":true}");

        final User user = client().setUserRole("ci", "admin");

        Assert.assertEquals("PUT", method);
        Assert.assertEquals("/api/users/ci/role", path);
        Assert.assertEquals("application/json", header("Content-Type"));
        Assert.assertEquals("{\"role\":\"admin\"}", requestBodyAsString());
        Assert.assertEquals("admin", user.getRole());
    }

    @Test
    public void deactivateAndReactivateUser() throws Exception {

        respond(200, "{\"username\":\"ci\",\"role\":\"user\",\"active\":false,"
                + "\"deactivatedAt\":\"2026-10-05T12:00:00.000+00:00\"}");

        Assert.assertFalse(client().deactivateUser("ci").isActive());
        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/users/ci/deactivate", path);
        Assert.assertEquals(0, requestBody.length);

        respond(200, "{\"username\":\"ci\",\"role\":\"user\",\"active\":true}");

        Assert.assertTrue(client().reactivateUser("ci").isActive());
        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/users/ci/reactivate", path);
        Assert.assertEquals(0, requestBody.length);
    }

    // Philter answers both refusals with a 403, so the message is what tells them apart.
    @Test
    public void missingScopeAndNonAdministratorAreDistinguishable() {

        respond(403, "{\"error\": \"Forbidden\", \"message\": \"This API key does not have the 'users:read' scope.\"}");
        final ClientException scope = Assert.assertThrows(ClientException.class, () -> client().getUsers());

        respond(403, "{\"message\":\"Listing users requires an administrator.\"}");
        final ClientException admin = Assert.assertThrows(ClientException.class, () -> client().getUsers());

        Assert.assertTrue(scope.getMessage(), scope.getMessage().contains("does not have the 'users:read' scope"));
        Assert.assertTrue(admin.getMessage(), admin.getMessage().contains("requires an administrator"));
        Assert.assertNotEquals(scope.getMessage(), admin.getMessage());
    }

    @Test
    public void createApiKey() throws Exception {

        respond(201, "{\"username\":\"ci\",\"apiKey\":\"pk-secret\",\"scopes\":[\"redact\",\"policies:read\"]}");

        final CreatedApiKeyResponse response =
                client().createApiKey("ci", List.of("redact", "policies:read"));

        Assert.assertEquals("ci", response.getUsername());
        Assert.assertEquals("pk-secret", response.getApiKey());
        Assert.assertEquals(List.of("redact", "policies:read"), response.getScopes());

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/users/ci/api-keys", path);
        Assert.assertEquals("application/json", header("Content-Type"));
        Assert.assertEquals("{\"scopes\":[\"redact\",\"policies:read\"]}", requestBodyAsString());
    }

    @Test
    public void createApiKeyWithRequest() throws Exception {

        respond(201, "{\"username\":\"a b\",\"apiKey\":\"pk-secret\",\"scopes\":[\"redact\"]}");

        final CreateApiKeyRequest request = new CreateApiKeyRequest();
        request.setScopes(List.of("redact"));

        Assert.assertEquals("pk-secret", client().createApiKey("a b", request).getApiKey());

        // The username is a path segment, so a space in it goes on the wire percent-encoded, and
        // as %20 rather than the + that URLEncoder would produce for a form body.
        Assert.assertEquals("/api/users/a%20b/api-keys", rawPath);
        Assert.assertEquals("/api/users/a b/api-keys", path);
        Assert.assertEquals("POST", method);
    }

    @Test
    public void createApiKeyWithScopeNotHeld() {

        respond(403, "{\"message\":\"The calling API key does not hold: ledger:export.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().createApiKey("ci", List.of("ledger:export")));

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("403"));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("does not hold: ledger:export."));
    }

    // Wire contract: every remaining endpoint is invoked once and its method, path, and
    // key query parameters are asserted. This guards against typos in the request
    // construction (wrong path, misspelled query param, wrong HTTP method) that pattern
    // tests on a handful of endpoints would not catch.
    @Test
    public void wireContract() throws Exception {

        final PhilterClient c = client();

        // Filter and explain (compile, reidentify).
        verify("POST", "/api/policies/compile", Map.of(), "compiled", () -> c.compilePolicy("policy"));
        verify("POST", "/api/reidentify", Map.of("owner", "o1"), "result",
                () -> c.reidentify("o1", new ReidentifyRequest()));

        // Status.
        verify("GET", "/api/signing-key", Map.of(), "key", c::getSigningKey);
        verify("GET", "/api/signing-key/k1", Map.of(), "key", () -> c.getSigningKey("k1"));

        // Policies.
        verify("GET", "/api/policies/p1", Map.of(), "{}", () -> c.getPolicy("p1"));
        verify("GET", "/api/policies/p1/versions/2", Map.of(), "{}", () -> c.getPolicyVersion("p1", 2));
        verify("GET", "/api/policies/p1/diff", Map.of("from", "1", "to", "2"), "{}",
                () -> c.getPolicyDiff("p1", 1, 2));

        // Contexts.
        verify("GET", "/api/contexts", Map.of(), "[]", c::getContexts);
        verify("GET", "/api/contexts/c1", Map.of(), "{}", () -> c.getContext("c1"));
        verify("PUT", "/api/contexts/c1", Map.of("entity_type_disambiguation", "true", "ledger", "false"), "{}",
                () -> c.updateContext("c1", true, false));
        verify("DELETE", "/api/contexts/c1", Map.of(), "{}", () -> c.deleteContext("c1"));
        verify("GET", "/api/contexts/c1/entries", Map.of(), "[]", () -> c.getContextEntries("c1"));
        verify("DELETE", "/api/contexts/c1/entries", Map.of(), "{}", () -> c.deleteContextEntries("c1"));
        verify("GET", "/api/contexts/c1/entries/export", Map.of("owner", "o1"), "x",
                () -> c.exportContextEntries("c1", "o1"));
        verify("POST", "/api/contexts/c1/entries/import", Map.of("on_conflict", "skip", "owner", "o1"), "{}",
                () -> c.importContextEntries("c1", "skip", "o1", "[]"));
        verify("DELETE", "/api/contexts/c1/entries/e1", Map.of(), "{}", () -> c.deleteContextEntry("c1", "e1"));

        // Documents.
        verify("GET", "/api/documents", Map.of(), "[]", c::getDocuments);
        verify("DELETE", "/api/documents/d1", Map.of(), "", () -> c.deleteDocument("d1"));
        verify("GET", "/api/documents/d1/status", Map.of(), "{}", () -> c.getDocumentStatus("d1"));

        // Legal holds.
        verify("GET", "/api/holds", Map.of(), "[]", c::getHolds);
        verify("GET", "/api/holds/r1", Map.of(), "{}", () -> c.getHold("r1"));
        verify("DELETE", "/api/holds/r1", Map.of(), "", () -> c.deleteHold("r1"));

        // Redaction ledger.
        verify("GET", "/api/ledger", Map.of("q", "term"), "x", () -> c.getLedger("term"));
        verify("GET", "/api/ledger/d1", Map.of(), "x", () -> c.getLedgerEntry("d1"));
        verify("GET", "/api/ledger/d1/export", Map.of(), "x", () -> c.exportLedger("d1"));
        verify("GET", "/api/ledger/d1/valid", Map.of(), "true", () -> c.isLedgerValid("d1"));
        verify("DELETE", "/api/ledger/d1", Map.of(), "{}", () -> c.deleteLedgerEntry("d1"));
        verify("DELETE", "/api/ledger", Map.of("older_than_days", "30"), "{}", () -> c.purgeLedger(30));

        // Custom lists.
        verify("GET", "/api/lists", Map.of(), "[]", c::getLists);
        verify("DELETE", "/api/lists/l1", Map.of(), "", () -> c.deleteList("l1"));

        // Redact lists.
        verify("GET", "/api/redact-lists", Map.of(), "[]", c::getRedactLists);
        verify("POST", "/api/redact-lists", Map.of(), "{}", () -> c.createRedactList("{}"));
        verify("PUT", "/api/redact-lists", Map.of(), "{}", () -> c.updateRedactList("{}"));
    }

    /**
     * Queues a single response, runs the call, and asserts the recorded request's method,
     * path, and the given query parameters.
     */
    private void verify(String method, String path, Map<String, String> params,
                        String responseBody, Action call) throws Exception {

        respond(200, responseBody);

        call.run();

        Assert.assertEquals(method + " " + path, method, this.method);
        Assert.assertEquals(method + " " + path, path, this.path);

        for (final Map.Entry<String, String> param : params.entrySet()) {
            Assert.assertEquals("query param '" + param.getKey() + "' on " + path,
                    param.getValue(), queryParameter(param.getKey()));
        }

    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    @Test
    public void purgeLedger() throws Exception {

        respond(200, "{\"message\":\"Purged 3 chains.\"}");

        final GenericResponse response = client().purgeLedger(30);

        Assert.assertEquals("Purged 3 chains.", response.getMessage());

        Assert.assertEquals("DELETE", method);
        Assert.assertEquals("/api/ledger", path);
        Assert.assertEquals("30", queryParameter("older_than_days"));
    }

    @Test
    public void getContextReturnsFilterTypeCounts() throws Exception {

        final String body = "{\"size\":125,\"filterTypes\":{\"EMAIL_ADDRESS\":40,\"PERSON\":83},\"untyped\":2}";
        respond(200, body);

        Assert.assertEquals(body, client().getContext("c1"));

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/contexts/c1", path);
    }

    // Audit log export.

    /** The column row Philter writes at the top of every export page. */
    private static final String CSV_HEADER =
            "timestamp,event,request_id,api_key_id,associated_object,client_ip_address,details\n";

    @Test
    public void exportAuditLogSendsTheRangeAndReportsTruncation() throws Exception {

        responseHeaders.put("X-Philter-Export-Rows", "2");
        responseHeaders.put("X-Philter-Export-Truncated", "true");
        responseHeaders.put("X-Philter-Export-Next-Offset", "52");
        responseHeaders.put("X-Philter-Export-Time-Zone", "America/New_York");
        respond(200, CSV_HEADER
                + "2026-10-05T12:00:00Z,policy_saved,r2,,p1,,\n"
                + "2026-10-05T11:00:00Z,policy_saved,r1,,p1,,\n");

        final AuditLogExport export = client().exportAuditLog(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5),
                ZoneId.of("America/New_York"), 50, 2);

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/audit/export", path);
        Assert.assertEquals("2026-10-01", queryParameter("from"));
        Assert.assertEquals("2026-10-05", queryParameter("to"));
        Assert.assertEquals("America/New_York", queryParameter("zone"));
        Assert.assertEquals("50", queryParameter("offset"));
        Assert.assertEquals("2", queryParameter("limit"));

        Assert.assertTrue(export.getCsv().startsWith(CSV_HEADER));
        Assert.assertEquals(2, export.getRows());
        Assert.assertTrue(export.isTruncated());
        Assert.assertEquals(Integer.valueOf(52), export.getNextOffset());
        Assert.assertEquals("America/New_York", export.getTimeZone());
    }

    @Test
    public void exportAuditLogOmitsUnsetParametersAndReportsACompleteExport() throws Exception {

        responseHeaders.put("X-Philter-Export-Rows", "0");
        responseHeaders.put("X-Philter-Export-Truncated", "false");
        responseHeaders.put("X-Philter-Export-Time-Zone", "UTC");
        respond(200, CSV_HEADER);

        final AuditLogExport export = client().exportAuditLog(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        Assert.assertEquals("2026-09-01", queryParameter("from"));
        Assert.assertEquals("2026-09-30", queryParameter("to"));
        Assert.assertFalse(queryParameters.containsKey("zone"));
        Assert.assertFalse(queryParameters.containsKey("offset"));
        Assert.assertFalse(queryParameters.containsKey("limit"));

        Assert.assertEquals(0, export.getRows());
        Assert.assertFalse(export.isTruncated());
        Assert.assertNull(export.getNextOffset());
        Assert.assertEquals("UTC", export.getTimeZone());
    }

    @Test
    public void exportAuditLogSurfacesPhilterReasonForABadRange() throws Exception {

        respond(400, "The date range cannot exceed 30 days.");

        try {
            client().exportAuditLog(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1));
            Assert.fail("Expected a ClientException.");
        } catch (final ClientException ex) {
            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("HTTP 400"));
            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("The date range cannot exceed 30 days."));
        }
    }

    @Test(expected = ClientException.class)
    public void exportAuditLogRefusesAResponseWithoutTruncationHeaders() throws Exception {

        respond(200, CSV_HEADER);

        client().exportAuditLog(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
    }

    @Test(expected = ClientException.class)
    public void exportAuditLogRefusesAMalformedRowCount() throws Exception {

        responseHeaders.put("X-Philter-Export-Rows", "many");
        responseHeaders.put("X-Philter-Export-Truncated", "false");
        respond(200, CSV_HEADER);

        client().exportAuditLog(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
    }

    // Optional parameters: every overload that accepts `owner` must put it on the wire, and every
    // paged endpoint must send offset and limit. A missing parameter here is silent in production
    // (the server falls back to the caller's own data, or to the default page), so each overload is
    // exercised rather than a representative sample.
    @Test
    public void ownerIsSentOnEveryOverloadThatAcceptsIt() throws Exception {

        final PhilterClient c = client();
        final LegalHoldRequest hold = new LegalHoldRequest();

        // Policies.
        verifyOwner("/api/policies/p1", "{}", () -> c.getPolicy("p1", OWNER));
        verifyOwner("/api/policies", "", () -> c.savePolicy("p1", "{}", OWNER));
        verifyOwner("/api/policies", "", () -> c.savePolicy("p1", "{}", "d", "n", OWNER));
        verifyOwner("/api/policies/p1/details", "{}", () -> c.getPolicyDetails("p1", OWNER));
        verifyOwner("/api/policies/p1/details", "{}", () -> c.setPolicyDetails("p1", "d", "n", OWNER));
        verifyOwner("/api/policies/p1/copy", "{}", () -> c.copyPolicy("p1", "p2", OWNER));
        verifyOwner("/api/policies/p1", "", () -> c.deletePolicy("p1", OWNER));
        verifyOwner("/api/policies/p1/versions/2", "{}", () -> c.getPolicyVersion("p1", 2, OWNER));
        verifyOwner("/api/policies/p1/diff", "{}", () -> c.getPolicyDiff("p1", 1, 2, OWNER));
        verifyOwner("/api/policies/p1/rollback", "{}", () -> c.rollbackPolicy("p1", 2, OWNER));

        // Contexts.
        verifyOwner("/api/contexts", "{}", () -> c.createContext("c1", true, false, OWNER));
        verifyOwner("/api/contexts/c1", "{}", () -> c.getContext("c1", OWNER));
        verifyOwner("/api/contexts/c1", "{}", () -> c.updateContext("c1", true, false, OWNER));
        verifyOwner("/api/contexts/c1", "{}", () -> c.deleteContext("c1", OWNER));
        verifyOwner("/api/contexts/c1/entries", "{}", () -> c.deleteContextEntries("c1", OWNER));
        verifyOwner("/api/contexts/c1/entries/export", "x", () -> c.exportContextEntries("c1", OWNER));
        verifyOwner("/api/contexts/c1/entries/import", "{}", () -> c.importContextEntries("c1", "skip", OWNER, "[]"));
        verifyOwner("/api/contexts/c1/entries/e1", "{}", () -> c.deleteContextEntry("c1", "e1", OWNER));

        // Documents.
        verifyOwner("/api/documents/d1", "x", () -> c.getDocument("d1", OWNER));
        verifyOwner("/api/documents/d1", "", () -> c.deleteDocument("d1", OWNER));
        verifyOwner("/api/documents/d1/status", "{}", () -> c.getDocumentStatus("d1", OWNER));

        // Legal holds.
        verifyOwner("/api/holds", "{}", () -> c.createHold(hold, OWNER));
        verifyOwner("/api/holds/r1", "{}", () -> c.getHold("r1", OWNER));
        verifyOwner("/api/holds/r1", "", () -> c.deleteHold("r1", OWNER));

        // Redaction ledger.
        verifyOwner("/api/ledger/d1", "x", () -> c.getLedgerEntry("d1", OWNER));
        verifyOwner("/api/ledger/d1/export", "x", () -> c.exportLedger("d1", OWNER));
        verifyOwner("/api/ledger/d1/valid", "true", () -> c.isLedgerValid("d1", OWNER));
        verifyOwner("/api/ledger/d1", "{}", () -> c.deleteLedgerEntry("d1", OWNER));
        verifyOwner("/api/ledger", "{}", () -> c.purgeLedger(30, OWNER));

        // Custom lists.
        verifyOwner("/api/lists", "[]", () -> c.getLists(OWNER));
        verifyOwner("/api/lists/l1", "{}", () -> c.saveList("l1", "a description", List.of("alpha"), OWNER));
        verifyOwner("/api/lists/l1", "", () -> c.deleteList("l1", OWNER));
        verifyOwner("/api/lists/l1", "{}", () -> c.getList("l1", OWNER));

        // Redact lists.
        verifyOwner("/api/redact-lists", "[]", () -> c.getRedactLists(OWNER));
        verifyOwner("/api/redact-lists", "{}", () -> c.createRedactList("{}", OWNER));
        verifyOwner("/api/redact-lists", "{}", () -> c.updateRedactList("{}", OWNER));

        // Re-identification.
        verifyOwner("/api/reidentify", "{}", () -> c.reidentify(OWNER, new ReidentifyRequest()));

        // Webhook.
        verifyOwner("/api/webhook", "{}", () -> c.getWebhook(OWNER));
        verifyOwner("/api/webhook", "{}", () -> c.setWebhook("https://hooks.example.com/philter",
                "a-shared-secret-of-at-least-16-characters", OWNER));
        verifyOwner("/api/webhook", "", () -> c.removeWebhook(OWNER));
    }

    @Test
    public void paginationIsSentOnEveryPagedEndpoint() throws Exception {

        final PhilterClient c = client();

        verifyPaged("/api/policies", "[]", () -> c.getPolicies(OWNER, 25, 50));
        verifyPaged("/api/policies/p1/versions", "[]", () -> c.getPolicyVersions("p1", OWNER, 25, 50));
        verifyPaged("/api/contexts", "[]", () -> c.getContexts(OWNER, 25, 50));
        verifyPaged("/api/contexts/c1/entries", "[]", () -> c.getContextEntries("c1", OWNER, 25, 50));
        verifyPaged("/api/documents", "[]", () -> c.getDocuments(OWNER, 25, 50));
        verifyPaged("/api/holds", "[]", () -> c.getHolds(OWNER, 25, 50));
        verifyPaged("/api/ledger", "[]", () -> c.getLedger("term", OWNER, 25, 50));
    }

    private static final String OWNER = "acme";

    private void verifyOwner(String path, String responseBody, Action call) throws Exception {

        respond(200, responseBody);
        call.run();

        Assert.assertEquals(path, this.path);
        Assert.assertEquals("owner on " + path, OWNER, queryParameter("owner"));

    }

    private void verifyPaged(String path, String responseBody, Action call) throws Exception {

        verifyOwner(path, responseBody, call);

        Assert.assertEquals("offset on " + path, "25", queryParameter("offset"));
        Assert.assertEquals("limit on " + path, "50", queryParameter("limit"));

    }


    // Admin settings.

    @Test
    public void getAdminSettingsMapsEverySetting() throws Exception {

        respond(200, "{\"diffuseCountsEnabled\":false,\"signingEnabled\":true,"
                + "\"webhookAllowlist\":\"hooks.example.com, 10.4.0.0/16\",\"phieldEnabled\":true,"
                + "\"phieldUrl\":\"https://phield.example.com\",\"phieldSourceId\":\"philter\","
                + "\"phieldOrganization\":\"acme\",\"phieldApiKeySet\":true,\"warnings\":[]}");

        final AdminSettings settings = client().getAdminSettings();

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/settings", path);
        Assert.assertTrue(queryParameters.isEmpty());
        Assert.assertEquals("application/json", header("Accept"));

        Assert.assertFalse(settings.isDiffuseCountsEnabled());
        Assert.assertTrue(settings.isSigningEnabled());
        Assert.assertEquals("hooks.example.com, 10.4.0.0/16", settings.getWebhookAllowlist());
        Assert.assertTrue(settings.isPhieldEnabled());
        Assert.assertEquals("https://phield.example.com", settings.getPhieldUrl());
        Assert.assertEquals("philter", settings.getPhieldSourceId());
        Assert.assertEquals("acme", settings.getPhieldOrganization());
        Assert.assertTrue(settings.isPhieldApiKeySet());
        Assert.assertTrue(settings.getWarnings().isEmpty());
    }

    @Test
    public void updateAdminSettingsSendsOnlyTheChangedSettings() throws Exception {

        respond(200, "{\"diffuseCountsEnabled\":false,\"signingEnabled\":true,\"webhookAllowlist\":\"\","
                + "\"phieldEnabled\":true,\"phieldUrl\":\"http://phield.example.com\",\"phieldSourceId\":\"philter\","
                + "\"phieldOrganization\":\"philter\",\"phieldApiKeySet\":true,"
                + "\"warnings\":[\"The Phield URL is http, so the API key is sent in the clear. Use an https URL.\"]}");

        final UpdateAdminSettingsRequest request = new UpdateAdminSettingsRequest();
        request.setSigningEnabled(true);
        request.setPhieldEnabled(true);
        request.setPhieldUrl("http://phield.example.com");
        request.setPhieldApiKey("phield-key");

        final AdminSettings settings = client().updateAdminSettings(request);

        Assert.assertEquals("PATCH", method);
        Assert.assertEquals("/api/settings", path);
        Assert.assertTrue(queryParameters.isEmpty());
        Assert.assertEquals("application/json", header("Content-Type"));
        // A setting left unset is left out, so Philter leaves it as it is.
        Assert.assertEquals("{\"signingEnabled\":true,\"phieldEnabled\":true,"
                + "\"phieldUrl\":\"http://phield.example.com\",\"phieldApiKey\":\"phield-key\"}", requestBodyAsString());

        Assert.assertTrue(settings.isSigningEnabled());
        Assert.assertEquals(1, settings.getWarnings().size());
        Assert.assertTrue(settings.getWarnings().get(0).contains("sent in the clear"));
    }

    @Test
    public void updateAdminSettingsSendsAnEmptyPhieldApiKeyToRemoveIt() throws Exception {

        respond(200, "{\"phieldApiKeySet\":false,\"warnings\":[]}");

        final UpdateAdminSettingsRequest request = new UpdateAdminSettingsRequest();
        request.setPhieldApiKey("");

        Assert.assertFalse(client().updateAdminSettings(request).isPhieldApiKeySet());
        Assert.assertEquals("{\"phieldApiKey\":\"\"}", requestBodyAsString());
    }

    @Test
    public void updateAdminSettingsSurfacesPhilterReason() {

        respond(400, "webhookAllowlist entry 'not a host' is not a hostname, an IP address, or a CIDR range.");

        final UpdateAdminSettingsRequest request = new UpdateAdminSettingsRequest();
        request.setWebhookAllowlist("hooks.example.com,not a host");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().updateAdminSettings(request));

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("HTTP 400"));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("'not a host' is not a hostname"));
    }

    // Webhook.

    @Test
    public void getWebhookMapsTheUrlAndSecretFlag() throws Exception {

        respond(200, "{\"url\":\"https://hooks.example.com/philter\",\"secretSet\":true}");

        final Webhook webhook = client().getWebhook();

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/webhook", path);
        Assert.assertTrue(queryParameters.isEmpty());
        Assert.assertEquals("application/json", header("Accept"));
        Assert.assertEquals("https://hooks.example.com/philter", webhook.getUrl());
        Assert.assertTrue(webhook.isSecretSet());
    }

    @Test
    public void getWebhookWhenNoneIsSet() throws Exception {

        respond(200, "{\"url\":null,\"secretSet\":false}");

        final Webhook webhook = client().getWebhook(OWNER);

        Assert.assertEquals("/api/webhook", path);
        Assert.assertEquals(OWNER, queryParameter("owner"));
        Assert.assertNull(webhook.getUrl());
        Assert.assertFalse(webhook.isSecretSet());
    }

    @Test
    public void setWebhookSendsTheUrlAndSecret() throws Exception {

        respond(200, "{\"url\":\"https://hooks.example.com/philter\",\"secretSet\":true}");

        final Webhook webhook = client().setWebhook("https://hooks.example.com/philter",
                "a-shared-secret-of-at-least-16-characters");

        Assert.assertEquals("PUT", method);
        Assert.assertEquals("/api/webhook", path);
        Assert.assertTrue(queryParameters.isEmpty());
        Assert.assertEquals("application/json", header("Content-Type"));
        Assert.assertEquals("{\"url\":\"https://hooks.example.com/philter\","
                + "\"secret\":\"a-shared-secret-of-at-least-16-characters\"}", requestBodyAsString());

        Assert.assertEquals("https://hooks.example.com/philter", webhook.getUrl());
        Assert.assertTrue(webhook.isSecretSet());
    }

    @Test
    public void setWebhookForAnOwner() throws Exception {

        respond(200, "{\"url\":\"https://hooks.example.com/philter\",\"secretSet\":true}");

        client().setWebhook("https://hooks.example.com/philter", "a-shared-secret-of-at-least-16-characters", OWNER);

        Assert.assertEquals("PUT", method);
        Assert.assertEquals(OWNER, queryParameter("owner"));
    }

    @Test
    public void setWebhookSurfacesPhilterReason() {

        respond(400, "Secret must be at least 16 characters.");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().setWebhook("https://hooks.example.com/philter", "too-short"));

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("HTTP 400"));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("Secret must be at least 16 characters."));
    }

    @Test
    public void removeWebhook() throws Exception {

        respond(204, "");

        client().removeWebhook();

        Assert.assertEquals("DELETE", method);
        Assert.assertEquals("/api/webhook", path);
        Assert.assertTrue(queryParameters.isEmpty());

        client().removeWebhook(OWNER);

        Assert.assertEquals("DELETE", method);
        Assert.assertEquals(OWNER, queryParameter("owner"));
    }

    // Listings across all users.

    /** Asserts the last request was an all-users listing of the given path, paged at offset 25, limit 50. */
    private void assertAllUsersListing(String path) {
        Assert.assertEquals("GET", method);
        Assert.assertEquals(path, this.path);
        Assert.assertEquals("all_users on " + path, "true", queryParameter("all_users"));
        Assert.assertEquals("offset on " + path, "25", queryParameter("offset"));
        Assert.assertEquals("limit on " + path, "50", queryParameter("limit"));
        Assert.assertFalse("owner on " + path, queryParameters.containsKey("owner"));
    }

    @Test
    public void getPoliciesAcrossUsersMapsNameAndOwner() throws Exception {

        respond(200, "[{\"name\":\"default\",\"owner\":\"alice@example.com\"},"
                + "{\"name\":\"default\",\"owner\":\"bob@example.com\"}]");

        final List<OwnedName> policies = client().getPoliciesAcrossUsers(25, 50);

        assertAllUsersListing("/api/policies");
        Assert.assertEquals(2, policies.size());
        Assert.assertEquals("default", policies.get(0).getName());
        Assert.assertEquals("alice@example.com", policies.get(0).getOwner());
        Assert.assertEquals("bob@example.com", policies.get(1).getOwner());
    }

    @Test
    public void getHoldsAcrossUsersMapsTheHoldAndItsOwner() throws Exception {

        respond(200, "[{\"reference\":\"case-1\",\"scopeType\":\"document\",\"scopeValue\":\"doc-1\","
                + "\"reason\":\"Litigation\",\"setAt\":\"2026-10-05T12:00:00Z\",\"owner\":\"alice@example.com\"}]");

        final List<OwnedLegalHoldResponse> holds = client().getHoldsAcrossUsers(25, 50);

        assertAllUsersListing("/api/holds");
        Assert.assertEquals(1, holds.size());
        Assert.assertEquals("case-1", holds.get(0).getReference());
        Assert.assertEquals("document", holds.get(0).getScopeType());
        Assert.assertEquals("doc-1", holds.get(0).getScopeValue());
        Assert.assertEquals("Litigation", holds.get(0).getReason());
        Assert.assertEquals("2026-10-05T12:00:00Z", holds.get(0).getSetAt());
        Assert.assertEquals("alice@example.com", holds.get(0).getOwner());
    }

    @Test
    public void rawJsonListingsAcrossUsersPassTheOwnersThrough() throws Exception {

        final PhilterClient c = client();

        final String contexts = "{\"contexts\":[{\"name\":\"tenant-a\",\"owner\":\"alice@example.com\"}]}";
        respond(200, contexts);
        Assert.assertEquals(contexts, c.getContextsAcrossUsers(25, 50));
        assertAllUsersListing("/api/contexts");

        final String lists = "[{\"name\":\"names\",\"owner\":\"bob@example.com\"}]";
        respond(200, lists);
        Assert.assertEquals(lists, c.getListsAcrossUsers(25, 50));
        assertAllUsersListing("/api/lists");

        final String chains = "{\"chains\":[{\"documentId\":\"doc-1\",\"owner\":\"alice@example.com\"}],\"total\":1}";
        respond(200, chains);
        Assert.assertEquals(chains, c.getLedgerAcrossUsers(25, 50));
        assertAllUsersListing("/api/ledger");
        Assert.assertFalse("q on /api/ledger", queryParameters.containsKey("q"));
    }

    @Test
    public void shortFormsAcrossUsersSendOnlyAllUsers() throws Exception {

        final PhilterClient c = client();
        final Map<String, Action> calls = new java.util.LinkedHashMap<>();
        calls.put("/api/policies", c::getPoliciesAcrossUsers);
        calls.put("/api/contexts", c::getContextsAcrossUsers);
        calls.put("/api/lists", c::getListsAcrossUsers);
        calls.put("/api/ledger", c::getLedgerAcrossUsers);
        calls.put("/api/holds", c::getHoldsAcrossUsers);

        for (final Map.Entry<String, Action> call : calls.entrySet()) {
            respond(200, call.getKey().equals("/api/contexts") || call.getKey().equals("/api/ledger") ? "{}" : "[]");
            call.getValue().run();
            Assert.assertEquals(call.getKey(), path);
            Assert.assertEquals("query on " + call.getKey(), Map.of("all_users", "true"), queryParameters);
        }
    }

    @Test
    public void listingAcrossUsersWithoutCrossUserAccessIsAClientException() throws Exception {

        respond(404, "");

        try {
            client().getPoliciesAcrossUsers();
            Assert.fail("Expected a ClientException.");
        } catch (final ClientException ex) {
            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("HTTP 404"));
        }
    }

    // Targeted return-type, parsing, and parameter assertions.

    @Test
    public void getDocumentReturnsBytes() throws Exception {

        final byte[] bytes = new byte[]{10, 20, 30, 40};
        respond(200, bytes);

        final byte[] result = client().getDocument("d1");

        Assert.assertArrayEquals(bytes, result);

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/documents/d1", path);
    }

    @Test
    public void getPolicyReturnsRawJson() throws Exception {

        final String policyJson = "{\"name\":\"default\",\"identifiers\":{}}";
        respond(200, policyJson);

        // The raw policy JSON must be returned verbatim rather than parsed.
        Assert.assertEquals(policyJson, client().getPolicy("default"));
    }

    @Test
    public void importContextEntriesReturnsJsonBodyAsString() throws Exception {

        final String responseJson = "{\"imported\":3,\"skipped\":1}";
        respond(200, responseJson);

        // A JSON response body must still be returned as an unparsed String.
        final String result = client().importContextEntries("c1", "skip", "o1", "[]");
        Assert.assertEquals(responseJson, result);

        Assert.assertEquals("[]", requestBodyAsString());
    }

    @Test
    public void paginationParametersAreSent() throws Exception {

        respond(200, "[]");

        client().getPolicies("acme", 25, 50);

        Assert.assertEquals("acme", queryParameter("owner"));
        Assert.assertEquals("25", queryParameter("offset"));
        Assert.assertEquals("50", queryParameter("limit"));
    }

    @Test
    public void optionalParametersAreOmittedWhenNull() throws Exception {

        respond(200, "[]");

        client().getPolicies();

        Assert.assertNull(queryParameter("owner"));
        Assert.assertNull(queryParameter("offset"));
        Assert.assertNull(queryParameter("limit"));
    }

    @Test
    public void pathSegmentsAreEncoded() throws Exception {

        respond(200, "{}");

        client().getPolicy("my policy/v2");

        // The segment is percent-encoded, so the slash does not become a path separator.
        Assert.assertEquals("/api/policies/my policy/v2", path);
    }

    // Character encoding. Philter redacts names, and a name is very often not ASCII, so the
    // request body, the response body, and query values all have to survive the round trip.

    @Test
    public void nonAsciiTextIsSentAndReadAsUtf8() throws Exception {

        final String sent = "Patient is Zoë Müller, 北京 clinic, naïve café.";
        final String returned = "Patient is {{{REDACTED-person}}}, 北京 clinic, naïve café.";

        respond(200, returned);

        final FilterResponse response = client().filter("ctx", "default", sent);

        // The body must go onto the wire as UTF-8 bytes, not as the platform default.
        Assert.assertArrayEquals(sent.getBytes(StandardCharsets.UTF_8), requestBody);
        // And a response with no charset on its Content-Type must be read back as UTF-8.
        Assert.assertEquals(returned, response.getFilteredText());
    }

    @Test
    public void queryValuesAreEncoded() throws Exception {

        // Every character here changes meaning if it reaches the query string unescaped.
        final String tricky = "a b & c = d + e / ü";

        respond(200, "[]");
        client().getLedger(tricky);

        // The server must be able to recover the value exactly.
        Assert.assertEquals(tricky, queryParameter("q"));
        // A space must be %20: URLEncoder's "+" would decode back to a space only in a form body,
        // and would be a literal plus here.
        Assert.assertTrue("raw query was: " + rawQuery, rawQuery.contains("%20"));
        Assert.assertFalse("raw query was: " + rawQuery, rawQuery.contains("+"));
    }

    @Test
    public void documentIdHeaderIsReadRegardlessOfCase() throws Exception {

        // Philter sends "X-Document-Id"; the client looks for "x-document-id".
        documentIdHeaderName = "X-Document-Id";
        documentIdHeader = "doc-cased";
        respond(200, "filtered");

        Assert.assertEquals("doc-cased", client().filter("ctx", "default", "text").getDocumentId());
    }

    // Response shapes the endpoints actually produce.

    @Test
    public void noContentResponseIsAccepted() throws Exception {

        // The specification gives DELETE /api/lists/{name} a 204.
        respond(204, "");

        client().deleteList("my-list");

        Assert.assertEquals("DELETE", method);
        Assert.assertEquals("/api/lists/my-list", path);
    }

    @Test
    public void emptyBodyOnAJsonCallYieldsNull() throws Exception {

        // A 2xx with no body cannot be parsed into an object, so the call returns null rather
        // than throwing. Callers that chain off the result need to expect this.
        respond(200, "");

        Assert.assertNull(client().createContext("ctx", true, false));
    }

    @Test
    public void errorResponseBodyIsReportedInTheException() throws Exception {

        // Philter explains its 400s in the body. Losing that leaves the caller with only a status
        // code to debug from.
        respond(400, "{\"message\":\"'strategy' must be CRYPTO_REPLACE or FPE_ENCRYPT_REPLACE.\"}");

        try {
            client().reidentify(null, new ReidentifyRequest());
            Assert.fail("A 400 should have raised a ClientException.");
        } catch (final ClientException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("400"));
            Assert.assertTrue(expected.getMessage(),
                    expected.getMessage().contains("CRYPTO_REPLACE"));
        }
    }

    // Client configuration.

    @Test
    public void timeoutIsApplied() throws Exception {

        holdResponse = true;

        final PhilterClient client = clientBuilder().withTimeout(1).build();

        final long start = System.nanoTime();
        try {
            client.getPolicies();
            Assert.fail("The request should have timed out.");
        } catch (final IOException expected) {
            // HttpTimeoutException is an IOException, which is what the client contract promises.
        }

        final long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        Assert.assertTrue("gave up after " + elapsedMs + "ms", elapsedMs < 10_000);
    }

    @Test
    public void endpointWithTrailingSlashWorks() throws Exception {

        respond(200, "[]");

        new PhilterClient.PhilterClientBuilder()
                .withEndpoint("http://localhost:" + server.getAddress().getPort() + "/")
                .build()
                .getPolicies();

        Assert.assertEquals("/api/policies", path);
    }

    @Test
    public void endpointPathPrefixIsNotPreserved() throws Exception {

        respond(200, "[]");

        new PhilterClient.PhilterClientBuilder()
                .withEndpoint("http://localhost:" + server.getAddress().getPort() + "/philter")
                .build()
                .getPolicies();

        // Paths are absolute, so a prefix on the endpoint is replaced rather than kept. Retrofit
        // behaved the same way. Philter behind a path-prefixed reverse proxy is not supported.
        Assert.assertEquals("/api/policies", path);
    }

    @Test
    public void emptyApiKeySendsNoAuthorizationHeader() throws Exception {

        respond(200, "[]");

        clientBuilder().withApiKey("").build().getPolicies();

        Assert.assertNull(header("Authorization"));
    }

    @Test
    public void oneClientIsUsableFromManyThreads() throws Exception {

        // The documentation tells callers to build one client and share it.
        respond(200, "[\"default\"]");

        final PhilterClient client = client();
        final ExecutorService pool = Executors.newFixedThreadPool(8);

        try {

            final List<Callable<List<String>>> calls = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                calls.add(client::getPolicies);
            }

            for (final Future<List<String>> result : pool.invokeAll(calls)) {
                Assert.assertEquals(List.of("default"), result.get());
            }

        } finally {
            pool.shutdownNow();
        }
    }

    // Error handling.

    @Test(expected = UnauthorizedException.class)
    public void unauthorized() throws Exception {
        respond(401, "");
        client().getPolicies();
    }

    @Test(expected = ServiceUnavailableException.class)
    public void serviceUnavailable() throws Exception {
        respond(503, "");
        client().getPolicies();
    }

    @Test(expected = ClientException.class)
    public void unknownErrorBecomesClientException() throws Exception {
        respond(500, "");
        client().getPolicies();
    }

}
