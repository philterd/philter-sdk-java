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
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class PhilterClientMockTest {

    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    private PhilterClient client() throws Exception {
        return clientBuilder().build();
    }

    private PhilterClient.PhilterClientBuilder clientBuilder() {
        final HttpUrl url = server.url("/");
        return new PhilterClient.PhilterClientBuilder().withEndpoint(url.toString());
    }

    // Filtering.

    @Test
    public void filterText() throws Exception {

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/plain")
                .setHeader("x-document-id", "doc-123")
                .setBody("My SSN is {{{REDACTED-ssn}}}."));

        final PhilterClient client = client();
        final FilterResponse response = client.filter("ctx", "default", "My SSN is 123-45-6789.");

        Assert.assertEquals("My SSN is {{{REDACTED-ssn}}}.", response.getFilteredText());
        Assert.assertEquals("ctx", response.getContext());
        Assert.assertEquals("doc-123", response.getDocumentId());

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("POST", request.getMethod());
        final HttpUrl requested = request.getRequestUrl();
        Assert.assertEquals("/api/filter", requested.encodedPath());
        Assert.assertEquals("ctx", requested.queryParameter("c"));
        Assert.assertEquals("default", requested.queryParameter("p"));
        // The text filter must force synchronous processing.
        Assert.assertEquals("false", requested.queryParameter("async"));
        Assert.assertEquals("My SSN is 123-45-6789.", request.getBody().readUtf8());
    }

    @Test
    public void filterPdf() throws Exception {

        final byte[] zipBytes = new byte[]{0x50, 0x4b, 0x03, 0x04, 0x01, 0x02};

        final Buffer body = new Buffer();
        body.write(zipBytes);

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/zip")
                .setHeader("x-document-id", "doc-pdf")
                .setBody(body));

        final File file = File.createTempFile("philter-test", ".pdf");
        Files.write(file.toPath(), new byte[]{1, 2, 3});
        file.deleteOnExit();

        final PhilterClient client = client();
        final BinaryFilterResponse response = client.filter("ctx", "default", "test.pdf", file);

        Assert.assertArrayEquals(zipBytes, response.getContent());
        Assert.assertEquals("doc-pdf", response.getDocumentId());

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("/api/filter", request.getRequestUrl().encodedPath());
        Assert.assertEquals("test.pdf", request.getRequestUrl().queryParameter("filename"));
        Assert.assertEquals("application/pdf", request.getHeader("Content-Type"));
    }

    @Test
    public void explain() throws Exception {

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"filteredText\":\"redacted\",\"context\":\"ctx\",\"documentId\":\"doc-1\"," +
                        "\"explanation\":{\"appliedSpans\":[{\"filterType\":\"ssn\",\"characterStart\":0,\"characterEnd\":11}]," +
                        "\"ignoredSpans\":[]}}"));

        final PhilterClient client = client();
        final ExplainResponse response = client.explain("ctx", "default", "My SSN is 123-45-6789.");

        Assert.assertEquals("redacted", response.getFilteredText());
        Assert.assertEquals("doc-1", response.getDocumentId());
        Assert.assertNotNull(response.getExplanation());
        Assert.assertEquals(1, response.getExplanation().getAppliedSpans().size());
        Assert.assertEquals("ssn", response.getExplanation().getAppliedSpans().get(0).getFilterType());

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("/api/explain", request.getRequestUrl().encodedPath());
    }

    // Authentication.

    @Test
    public void authorizationHeaderIsSent() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));

        final PhilterClient client = clientBuilder().withApiKey("secret-key").build();
        client.getPolicies();

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("secret-key", request.getHeader("Authorization"));
    }

    @Test
    public void noAuthorizationHeaderWhenApiKeyAbsent() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));

        final PhilterClient client = client();
        client.getPolicies();

        final RecordedRequest request = server.takeRequest();
        Assert.assertNull(request.getHeader("Authorization"));
    }

    @Test
    public void suppliedOkHttpClientBuilderIsUsedAndAuthIsLayeredOnTop() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));

        // An interceptor on the supplied builder proves the builder is actually used; the API key
        // proves the Authorization interceptor is still layered on top of the supplied builder.
        final OkHttpClient.Builder supplied = new OkHttpClient.Builder()
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder().header("X-Custom", "custom-value").build()));

        final PhilterClient client = clientBuilder()
                .withOkHttpClientBuilder(supplied)
                .withApiKey("secret-key")
                .build();
        client.getPolicies();

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("custom-value", request.getHeader("X-Custom"));
        Assert.assertEquals("secret-key", request.getHeader("Authorization"));
    }

    // Status.

    @Test
    public void health() throws Exception {

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"applicationVersion\":\"4.0.0\",\"gitCommit\":\"abc123\"," +
                        "\"redactionPolicySchemaVersion\":\"1\",\"status\":\"healthy\"}"));

        final PhilterClient client = client();
        final StatusResponse status = client.health();

        Assert.assertEquals("4.0.0", status.getApplicationVersion());
        Assert.assertEquals("healthy", status.getStatus());

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("/api/health", request.getRequestUrl().encodedPath());
    }

    // Policies.

    @Test
    public void getPolicies() throws Exception {

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[\"default\",\"strict\"]"));

        final PhilterClient client = client();
        final List<String> policies = client.getPolicies();

        Assert.assertEquals(2, policies.size());
        Assert.assertTrue(policies.contains("default"));
        Assert.assertTrue(policies.contains("strict"));
    }

    @Test
    public void savePolicy() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(201));

        final PhilterClient client = client();
        client.savePolicy("default", "{\"name\":\"default\"}");

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("POST", request.getMethod());
        Assert.assertEquals("/api/policies", request.getRequestUrl().encodedPath());
        Assert.assertEquals("default", request.getRequestUrl().queryParameter("name"));
        Assert.assertEquals("{\"name\":\"default\"}", request.getBody().readUtf8());
    }

    @Test
    public void deletePolicy() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(200));

        final PhilterClient client = client();
        client.deletePolicy("default");

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("DELETE", request.getMethod());
        Assert.assertEquals("/api/policies/default", request.getRequestUrl().encodedPath());
    }

    @Test
    public void getPolicyVersions() throws Exception {

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"capturedTimestamp\":\"2026-01-01T00:00:00Z\",\"contentHash\":\"hash1\",\"revision\":1}]"));

        final PhilterClient client = client();
        final List<PolicyVersionSummary> versions = client.getPolicyVersions("default");

        Assert.assertEquals(1, versions.size());
        Assert.assertEquals(1, versions.get(0).getRevision());
        Assert.assertEquals("hash1", versions.get(0).getContentHash());
    }

    @Test
    public void rollbackPolicy() throws Exception {

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"revision\":3}"));

        final PhilterClient client = client();
        final PolicyRollbackResponse response = client.rollbackPolicy("default", 2);

        Assert.assertEquals(3, response.getRevision());

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("POST", request.getMethod());
        Assert.assertEquals("/api/policies/default/rollback", request.getRequestUrl().encodedPath());
        Assert.assertEquals("2", request.getRequestUrl().queryParameter("revision"));
    }

    // Contexts.

    @Test
    public void createContext() throws Exception {

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"created\"}"));

        final PhilterClient client = client();
        final GenericResponse response = client.createContext("ctx", true, false);

        Assert.assertEquals("created", response.getMessage());

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("POST", request.getMethod());
        Assert.assertEquals("/api/contexts", request.getRequestUrl().encodedPath());
        Assert.assertEquals("ctx", request.getRequestUrl().queryParameter("name"));
        Assert.assertEquals("true", request.getRequestUrl().queryParameter("entity_type_disambiguation"));
        Assert.assertEquals("false", request.getRequestUrl().queryParameter("ledger"));
    }

    // Legal holds.

    @Test
    public void createHold() throws Exception {

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"reference\":\"ref-1\",\"reason\":\"litigation\",\"scopeType\":\"context\"," +
                        "\"scopeValue\":\"ctx\",\"setAt\":\"2026-01-01T00:00:00Z\"}"));

        final LegalHoldRequest holdRequest = new LegalHoldRequest();
        holdRequest.setReference("ref-1");
        holdRequest.setReason("litigation");
        holdRequest.setScopeType("context");
        holdRequest.setScopeValue("ctx");

        final PhilterClient client = client();
        final LegalHoldResponse response = client.createHold(holdRequest);

        Assert.assertEquals("ref-1", response.getReference());
        Assert.assertEquals("litigation", response.getReason());
        Assert.assertEquals("2026-01-01T00:00:00Z", response.getSetAt());

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("POST", request.getMethod());
        Assert.assertEquals("/api/holds", request.getRequestUrl().encodedPath());
        Assert.assertTrue(request.getBody().readUtf8().contains("\"reference\":\"ref-1\""));
    }

    // Custom lists.

    @Test
    public void getList() throws Exception {

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"lists\":[\"alpha\",\"beta\"]}"));

        final PhilterClient client = client();
        final GetListsResponse response = client.getList("my-list");

        Assert.assertEquals(2, response.getLists().size());
        Assert.assertTrue(response.getLists().contains("alpha"));

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("/api/lists/my-list", request.getRequestUrl().encodedPath());
    }

    @Test
    public void saveList() throws Exception {

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"saved\"}"));

        final PhilterClient client = client();
        final GenericResponse response = client.saveList("my-list", "a description", List.of("alpha", "beta"));

        Assert.assertEquals("saved", response.getMessage());

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("POST", request.getMethod());
        Assert.assertEquals("/api/lists/my-list", request.getRequestUrl().encodedPath());
        Assert.assertEquals("a description", request.getRequestUrl().queryParameter("description"));
        Assert.assertEquals("[\"alpha\",\"beta\"]", request.getBody().readUtf8());
    }

    // Wire contract: every remaining endpoint is invoked once and its method, path, and
    // key query parameters are asserted. This guards against typos in the Retrofit
    // annotations (wrong path, misspelled query param, wrong HTTP method) that pattern
    // tests on a handful of endpoints would not catch.
    @Test
    public void wireContract() throws Exception {

        final PhilterClient c = client();

        // Filter and explain (compile, reidentify).
        verify(c, "POST", "/api/policies/compile", Map.of(), "compiled", () -> c.compilePolicy("policy"));
        verify(c, "POST", "/api/reidentify", Map.of("owner", "o1"), "result",
                () -> c.reidentify("o1", new ReidentifyRequest()));

        // Status.
        verify(c, "GET", "/api/status", Map.of(), "{}", c::status);
        verify(c, "GET", "/api/signing-key", Map.of(), "key", c::getSigningKey);

        // Policies.
        verify(c, "GET", "/api/policies/p1", Map.of(), "{}", () -> c.getPolicy("p1"));
        verify(c, "GET", "/api/policies/p1/versions/2", Map.of(), "{}", () -> c.getPolicyVersion("p1", 2));
        verify(c, "GET", "/api/policies/p1/diff", Map.of("from", "1", "to", "2"), "{}",
                () -> c.getPolicyDiff("p1", 1, 2));

        // Contexts.
        verify(c, "GET", "/api/contexts", Map.of(), "[]", c::getContexts);
        verify(c, "GET", "/api/contexts/c1", Map.of(), "{}", () -> c.getContext("c1"));
        verify(c, "PUT", "/api/contexts/c1", Map.of("entity_type_disambiguation", "true", "ledger", "false"), "{}",
                () -> c.updateContext("c1", true, false));
        verify(c, "DELETE", "/api/contexts/c1", Map.of(), "{}", () -> c.deleteContext("c1"));
        verify(c, "GET", "/api/contexts/c1/entries", Map.of(), "[]", () -> c.getContextEntries("c1"));
        verify(c, "DELETE", "/api/contexts/c1/entries", Map.of(), "{}", () -> c.deleteContextEntries("c1"));
        verify(c, "GET", "/api/contexts/c1/entries/export", Map.of("owner", "o1"), "x",
                () -> c.exportContextEntries("c1", "o1"));
        verify(c, "POST", "/api/contexts/c1/entries/import", Map.of("on_conflict", "skip", "owner", "o1"), "{}",
                () -> c.importContextEntries("c1", "skip", "o1", "[]"));
        verify(c, "DELETE", "/api/contexts/c1/entries/e1", Map.of(), "{}", () -> c.deleteContextEntry("c1", "e1"));

        // Documents.
        verify(c, "GET", "/api/documents", Map.of(), "[]", c::getDocuments);
        verify(c, "DELETE", "/api/documents/d1", Map.of(), "", () -> c.deleteDocument("d1"));
        verify(c, "GET", "/api/documents/d1/status", Map.of(), "{}", () -> c.getDocumentStatus("d1"));

        // Legal holds.
        verify(c, "GET", "/api/holds", Map.of(), "[]", c::getHolds);
        verify(c, "GET", "/api/holds/r1", Map.of(), "{}", () -> c.getHold("r1"));
        verify(c, "DELETE", "/api/holds/r1", Map.of(), "", () -> c.deleteHold("r1"));

        // Redaction ledger.
        verify(c, "GET", "/api/ledger", Map.of("q", "term"), "x", () -> c.getLedger("term"));
        verify(c, "GET", "/api/ledger/d1", Map.of(), "x", () -> c.getLedgerEntry("d1"));
        verify(c, "GET", "/api/ledger/d1/export", Map.of(), "x", () -> c.exportLedger("d1"));
        verify(c, "GET", "/api/ledger/d1/valid", Map.of(), "true", () -> c.isLedgerValid("d1"));

        // Custom lists.
        verify(c, "GET", "/api/lists", Map.of(), "[]", c::getLists);
        verify(c, "DELETE", "/api/lists/l1", Map.of(), "", () -> c.deleteList("l1"));

        // Redact lists.
        verify(c, "GET", "/api/redact-lists", Map.of(), "[]", c::getRedactLists);
        verify(c, "POST", "/api/redact-lists", Map.of(), "{}", () -> c.createRedactList("{}"));
        verify(c, "PUT", "/api/redact-lists", Map.of(), "{}", () -> c.updateRedactList("{}"));
    }

    /**
     * Enqueues a single response, runs the call, and asserts the recorded request's method,
     * path, and the given query parameters.
     */
    private void verify(PhilterClient client, String method, String path, Map<String, String> params,
                        String responseBody, Action call) throws Exception {

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        call.run();

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals(method + " " + path, method, request.getMethod());
        Assert.assertEquals(method + " " + path, path, request.getRequestUrl().encodedPath());

        for (final Map.Entry<String, String> param : params.entrySet()) {
            Assert.assertEquals("query param '" + param.getKey() + "' on " + path,
                    param.getValue(), request.getRequestUrl().queryParameter(param.getKey()));
        }

    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    // Targeted return-type, parsing, and parameter assertions.

    @Test
    public void getDocumentReturnsBytes() throws Exception {

        final byte[] bytes = new byte[]{10, 20, 30, 40};
        final Buffer body = new Buffer();
        body.write(bytes);

        server.enqueue(new MockResponse().setResponseCode(200).setBody(body));

        final byte[] result = client().getDocument("d1");

        Assert.assertArrayEquals(bytes, result);

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("GET", request.getMethod());
        Assert.assertEquals("/api/documents/d1", request.getRequestUrl().encodedPath());
    }

    @Test
    public void getPolicyReturnsRawJson() throws Exception {

        final String policyJson = "{\"name\":\"default\",\"identifiers\":{}}";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(policyJson));

        // The raw policy JSON must be returned verbatim rather than parsed.
        Assert.assertEquals(policyJson, client().getPolicy("default"));
    }

    @Test
    public void importContextEntriesReturnsJsonBodyAsString() throws Exception {

        final String responseJson = "{\"imported\":3,\"skipped\":1}";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson));

        // An application/json response body must still convert to String (scalars converter).
        final String result = client().importContextEntries("c1", "skip", "o1", "[]");
        Assert.assertEquals(responseJson, result);

        final RecordedRequest request = server.takeRequest();
        Assert.assertEquals("[]", request.getBody().readUtf8());
    }

    @Test
    public void paginationParametersAreSent() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json").setBody("[]"));

        client().getPolicies("acme", 25, 50);

        final HttpUrl url = server.takeRequest().getRequestUrl();
        Assert.assertEquals("acme", url.queryParameter("owner"));
        Assert.assertEquals("25", url.queryParameter("offset"));
        Assert.assertEquals("50", url.queryParameter("limit"));
    }

    @Test
    public void optionalParametersAreOmittedWhenNull() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json").setBody("[]"));

        client().getPolicies();

        final HttpUrl url = server.takeRequest().getRequestUrl();
        Assert.assertNull(url.queryParameter("owner"));
        Assert.assertNull(url.queryParameter("offset"));
        Assert.assertNull(url.queryParameter("limit"));
    }

    // Error handling.

    @Test(expected = UnauthorizedException.class)
    public void unauthorized() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401));
        client().getPolicies();
    }

    @Test(expected = ServiceUnavailableException.class)
    public void serviceUnavailable() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));
        client().getPolicies();
    }

    @Test(expected = ClientException.class)
    public void unknownErrorBecomesClientException() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));
        client().getPolicies();
    }

}
