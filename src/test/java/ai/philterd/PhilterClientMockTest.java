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
import ai.philterd.philter.model.GetApiKeyScopesResponse;
import ai.philterd.philter.model.ApiKeyScopeDescription;
import ai.philterd.philter.model.SigningKey;
import ai.philterd.philter.model.RedactListsRequest;
import ai.philterd.philter.model.RedactLists;
import ai.philterd.philter.model.LedgerExport;
import ai.philterd.philter.model.LedgerEntry;
import ai.philterd.philter.model.LedgerChain;
import ai.philterd.philter.model.GetLedgerResponse;
import ai.philterd.philter.model.GetDocumentsResponse;
import ai.philterd.philter.model.GetContextsAcrossUsersResponse;
import ai.philterd.philter.model.GetContextEntriesResponse;
import ai.philterd.philter.model.DocumentSummary;
import ai.philterd.philter.model.DocumentStatus;
import ai.philterd.philter.model.CustomListSummary;
import ai.philterd.philter.model.ContextEntry;
import ai.philterd.philter.model.ContextDetails;
import ai.philterd.philter.model.SignInResponse;
import ai.philterd.philter.model.MfaEnrollment;
import ai.philterd.philter.model.ApiKey;
import ai.philterd.philter.model.AuditEvent;
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
import ai.philterd.philter.model.GetApiKeysResponse;
import ai.philterd.philter.model.GetAuditLogResponse;
import ai.philterd.philter.model.GetListsResponse;
import ai.philterd.philter.model.GetUsersResponse;
import ai.philterd.philter.model.LegalHoldRequest;
import ai.philterd.philter.model.LegalHoldResponse;
import ai.philterd.philter.model.OwnedLegalHoldResponse;
import ai.philterd.philter.model.OwnedName;
import ai.philterd.philter.model.ManagedPolicySummary;
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
import ai.philterd.philter.model.exceptions.SignInLockedException;
import ai.philterd.philter.model.exceptions.SignInRateLimitedException;
import ai.philterd.philter.model.exceptions.SignInThrottledException;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
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

    /** Every request the server received, in order, for a call that makes more than one. */
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();

    /** The response the server will next return. */
    private volatile int status = 200;
    private volatile byte[] responseBody = new byte[0];
    private volatile String documentIdHeader;
    private volatile String documentIdHeaderName = "x-document-id";
    private volatile String locationHeader;
    private volatile String contentTypeHeader;
    private final Map<String, String> responseHeaders = new HashMap<>();

    /** Responses returned in order before falling back to {@link #status} and {@link #responseBody}. */
    private final Queue<Map.Entry<Integer, byte[]>> queuedResponses = new ConcurrentLinkedQueue<>();

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
            requests.add(new RecordedRequest(method, path, rawQuery, queryParameters,
                    new String(requestBody, StandardCharsets.UTF_8)));

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

            final Map.Entry<Integer, byte[]> queued = queuedResponses.poll();
            final int status = queued != null ? queued.getKey() : this.status;
            final byte[] responseBody = queued != null ? queued.getValue() : this.responseBody;

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

    /** Queues a response to be returned before those set with {@link #respond(int, String)}. */
    private void respondNext(final int status, final String body) {
        queuedResponses.add(Map.entry(status, body.getBytes(StandardCharsets.UTF_8)));
    }

    private static final class RecordedRequest {

        final String method;
        final String path;
        final String rawQuery;
        final Map<String, String> queryParameters;
        final String body;

        RecordedRequest(String method, String path, String rawQuery, Map<String, String> queryParameters, String body) {
            this.method = method;
            this.path = path;
            this.rawQuery = rawQuery;
            this.queryParameters = queryParameters;
            this.body = body;
        }

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

    /** The request the mock server last received, for comparing two calls. */
    private String lastRequest() {
        return method + " " + path + " " + new TreeMap<>(queryParameters) + " Content-Type=" + header("Content-Type")
                + " Accept=" + header("Accept") + " body=" + java.util.Arrays.toString(requestBody);
    }

    @Test
    public void synchronousPdfFromBytesSendsTheSameRequestAsFromAFile() throws Exception {

        final byte[] pdf = {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31};
        final File file = File.createTempFile("philter-test", ".pdf");
        Files.write(file.toPath(), pdf);
        file.deleteOnExit();
        final byte[] zip = {0x50, 0x4b, 0x03, 0x04};

        respond(200, zip);
        documentIdHeader = "doc-bytes";
        client().filter("ctx", "default", "upload.pdf", file);
        final String fromFile = lastRequest();

        final BinaryFilterResponse response = client().filter("ctx", "default", "upload.pdf", pdf);
        Assert.assertEquals(fromFile, lastRequest());
        Assert.assertArrayEquals(pdf, requestBody);
        Assert.assertEquals("application/pdf", header("Content-Type"));
        Assert.assertEquals("false", queryParameter("async"));
        Assert.assertEquals("upload.pdf", queryParameter("filename"));
        Assert.assertArrayEquals(zip, response.getContent());
        Assert.assertEquals("doc-bytes", response.getDocumentId());

        respond(200, pdf);
        client().filterToPdf("ctx", "default", "upload.pdf", file);
        final String toPdfFromFile = lastRequest();
        final BinaryFilterResponse toPdf = client().filterToPdf("ctx", "default", "upload.pdf", pdf);
        Assert.assertEquals(toPdfFromFile, lastRequest());
        Assert.assertEquals("application/pdf", header("Accept"));
        Assert.assertArrayEquals(pdf, toPdf.getContent());
    }

    @Test
    public void asynchronousPdfFromBytesSendsTheSameRequestAsFromAFile() throws Exception {

        final byte[] pdf = {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31};
        final File file = File.createTempFile("philter-test", ".pdf");
        Files.write(file.toPath(), pdf);
        file.deleteOnExit();

        respond(202, "{\"documentId\":\"doc-async-bytes\"}");
        client().filterAsync("ctx", "default", "upload.pdf", file);
        final String fromFile = lastRequest();

        Assert.assertEquals("doc-async-bytes", client().filterAsync("ctx", "default", "upload.pdf", pdf));
        Assert.assertEquals(fromFile, lastRequest());
        Assert.assertArrayEquals(pdf, requestBody);
        Assert.assertEquals("true", queryParameter("async"));
        Assert.assertEquals("application/zip", header("Accept"));

        client().filterToPdfAsync("ctx", "default", "upload.pdf", file);
        final String toPdfFromFile = lastRequest();
        Assert.assertEquals("doc-async-bytes", client().filterToPdfAsync("ctx", "default", "upload.pdf", pdf));
        Assert.assertEquals(toPdfFromFile, lastRequest());
        Assert.assertEquals("application/pdf", header("Accept"));
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

    // Signed responses.

    /** A compact JWT shaped like Philter's, whose bodyHash is the SHA-256 of the body's UTF-8 bytes. */
    private static String signatureFor(final String body) throws Exception {
        final byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8));
        final StringBuilder hex = new StringBuilder();
        for (final byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        final java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString("{\"alg\":\"ES256\",\"typ\":\"JWT\",\"kid\":\"0123456789abcdef\"}".getBytes(StandardCharsets.UTF_8))
                + "." + encoder.encodeToString(("{\"bodyHash\":\"" + hex + "\",\"policyName\":\"default\"}").getBytes(StandardCharsets.UTF_8))
                + ".c2lnbmF0dXJl";
    }

    /** The bodyHash claim of a compact JWT. */
    private static String bodyHashOf(final String jwt) {
        final String payload = new String(java.util.Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
        return payload.replaceAll(".*\"bodyHash\":\"([0-9a-f]+)\".*", "$1");
    }

    private static String sha256Hex(final String text) throws Exception {
        final byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        final StringBuilder hex = new StringBuilder();
        for (final byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /** The verification documented in redacting-text.md, step for step. */
    private static boolean[] verifyAsDocumented(final PhilterClient client, final String signature, final String body)
            throws Exception {

        final String[] parts = signature.split("\\.");
        final java.util.Base64.Decoder base64url = java.util.Base64.getUrlDecoder();

        final com.google.gson.JsonObject header = com.google.gson.JsonParser.parseString(
                new String(base64url.decode(parts[0]), StandardCharsets.UTF_8)).getAsJsonObject();
        final String pem = client.getSigningKeyDetails(header.get("kid").getAsString()).getPem()
                .replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
        final java.security.PublicKey publicKey = java.security.KeyFactory.getInstance("EC")
                .generatePublic(new java.security.spec.X509EncodedKeySpec(java.util.Base64.getDecoder().decode(pem)));

        final java.security.Signature verifier = java.security.Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(publicKey);
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        final boolean signatureValid = verifier.verify(base64url.decode(parts[2]));

        final com.google.gson.JsonObject payload = com.google.gson.JsonParser.parseString(
                new String(base64url.decode(parts[1]), StandardCharsets.UTF_8)).getAsJsonObject();
        final boolean bodyMatches = sha256Hex(body).equals(payload.get("bodyHash").getAsString());

        return new boolean[]{signatureValid, bodyMatches};
    }

    @Test
    public void theDocumentedVerificationAcceptsARealSignatureAndRejectsTampering() throws Exception {

        // Sign as Philter's SigningService does: ES256 over header.payload, P1363 signature encoding.
        final java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("EC");
        generator.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        final java.security.KeyPair keyPair = generator.generateKeyPair();

        final String body = "Zoë Ångström 测试, SSN {{{REDACTED-ssn}}}.";
        final java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        final String signingInput = encoder.encodeToString("{\"alg\":\"ES256\",\"typ\":\"JWT\",\"kid\":\"k-1\"}".getBytes(StandardCharsets.UTF_8))
                + "." + encoder.encodeToString(("{\"bodyHash\":\"" + sha256Hex(body) + "\",\"policyName\":\"default\","
                + "\"policyVersion\":1,\"documentId\":\"doc-1\",\"iat\":1759665600}").getBytes(StandardCharsets.UTF_8));
        final java.security.Signature signer = java.security.Signature.getInstance("SHA256withECDSAinP1363Format");
        signer.initSign(keyPair.getPrivate());
        signer.update(signingInput.getBytes(StandardCharsets.UTF_8));
        final String jwt = signingInput + "." + encoder.encodeToString(signer.sign());

        // Philter serves the PEM in JSON with its newlines escaped.
        final String pem = "-----BEGIN PUBLIC KEY-----\n"
                + java.util.Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
        final String keyJson = "{\"keyId\":\"k-1\",\"pem\":\"" + pem.replace("\n", "\\n") + "\",\"active\":true}";

        final PhilterClient c = client();

        responseHeaders.put("X-Philter-Signature", jwt);
        respond(200, body);
        final FilterResponse response = c.filter("ctx", "default", null, "Zoë Ångström 测试, SSN 123-45-6789.", true);
        responseHeaders.clear();

        respond(200, keyJson);
        final boolean[] genuine = verifyAsDocumented(c, response.getSignature(), response.getFilteredText());
        Assert.assertEquals("/api/signing-key/k-1", path);
        Assert.assertTrue("the signature verifies", genuine[0]);
        Assert.assertTrue("the body matches its hash", genuine[1]);

        respond(200, keyJson);
        final boolean[] tampered = verifyAsDocumented(c, response.getSignature(), response.getFilteredText() + " ");
        Assert.assertTrue(tampered[0]);
        Assert.assertFalse("a changed body no longer matches", tampered[1]);

        final java.security.KeyPair other = generator.generateKeyPair();
        respond(200, "{\"keyId\":\"k-1\",\"pem\":\"-----BEGIN PUBLIC KEY-----\\n"
                + java.util.Base64.getEncoder().encodeToString(other.getPublic().getEncoded())
                + "\\n-----END PUBLIC KEY-----\",\"active\":true}");
        Assert.assertFalse("another key does not verify it", verifyAsDocumented(c, response.getSignature(), response.getFilteredText())[0]);
    }

    @Test
    public void filterSendsNoSignByDefaultAndAnUnsignedResponseHasNoSignature() throws Exception {

        respond(200, "My SSN is {{{REDACTED-ssn}}}.");

        final FilterResponse response = client().filter("ctx", "default", null, "My SSN is 123-45-6789.");

        Assert.assertFalse(queryParameters.containsKey("sign"));
        Assert.assertNull(response.getSignature());
    }

    @Test
    public void filterAsksForASignatureAndReturnsIt() throws Exception {

        final String body = "Zoë Ångström 测试, SSN {{{REDACTED-ssn}}}.";
        final String jwt = signatureFor(body);
        responseHeaders.put("X-Philter-Signature", jwt);
        contentTypeHeader = "text/plain;charset=UTF-8";
        respond(200, body);

        final FilterResponse response = client().filter("ctx", "default", "notes.txt", "Zoë Ångström 测试, SSN 123-45-6789.", true);

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/filter", path);
        Assert.assertEquals("true", queryParameter("sign"));
        Assert.assertEquals("notes.txt", queryParameter("filename"));

        Assert.assertEquals(jwt, response.getSignature());
        // The signature covers the body exactly as returned, so a verifier can recompute it.
        Assert.assertEquals(bodyHashOf(jwt), sha256Hex(response.getFilteredText()));
    }

    @Test
    public void aSignatureFromTheAdminSettingIsReturnedWithoutAsking() throws Exception {

        final String body = "My SSN is {{{REDACTED-ssn}}}.";
        responseHeaders.put("X-Philter-Signature", signatureFor(body));
        respond(200, body);

        final FilterResponse response = client().filter("ctx", "default", "My SSN is 123-45-6789.");

        Assert.assertFalse(queryParameters.containsKey("sign"));
        Assert.assertNotNull(response.getSignature());
    }

    @Test
    public void explainAsksForASignatureAndKeepsTheExactBody() throws Exception {

        // Whitespace and key order a re-serialization would not reproduce.
        final String body = "{\"filteredText\":\"Zoë {{{REDACTED-ssn}}}\",  \"explanation\":{\"appliedSpans\":[],\"ignoredSpans\":[]},"
                + "\"policyName\":\"default\"}";
        final String jwt = signatureFor(body);
        responseHeaders.put("X-Philter-Signature", jwt);
        respond(200, body);

        final ExplainResponse response = client().explain("ctx", "default", null, "Zoë 123-45-6789", true);

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/explain", path);
        Assert.assertEquals("true", queryParameter("sign"));

        Assert.assertEquals("Zoë {{{REDACTED-ssn}}}", response.getFilteredText());
        Assert.assertEquals(jwt, response.getSignature());
        Assert.assertEquals(body, response.getResponseBody());
        Assert.assertEquals(bodyHashOf(jwt), sha256Hex(response.getResponseBody()));
    }

    @Test
    public void explainWithAnEmptyBodyReturnsNullAsBefore() throws Exception {

        respond(200, "");

        Assert.assertNull(client().explain("ctx", "default", "123-45-6789"));
    }

    @Test
    public void explainSendsNoSignByDefault() throws Exception {

        final String body = "{\"filteredText\":\"{{{REDACTED-ssn}}}\",\"explanation\":{\"appliedSpans\":[],\"ignoredSpans\":[]}}";
        respond(200, body);

        final ExplainResponse response = client().explain("ctx", "default", "123-45-6789");

        Assert.assertFalse(queryParameters.containsKey("sign"));
        Assert.assertNull(response.getSignature());
        Assert.assertEquals(body, response.getResponseBody());
    }

    // Status.

    @Test
    public void health() throws Exception {

        respond(200, "{\"applicationVersion\":\"4.0.0\",\"gitCommit\":\"abc123\"," +
                "\"redactionPolicySchemaVersion\":\"1\",\"status\":\"UP\"}");

        final StatusResponse status = client().health();

        Assert.assertEquals("4.0.0", status.getApplicationVersion());
        Assert.assertEquals("UP", status.getStatus());

        Assert.assertEquals("/api/health", path);
    }

    @Test
    public void regenerateSigningKeyReturnsTheActiveKeyId() throws Exception {

        respond(200, "{\"keyId\":\"k-2026-10-05\"}");

        Assert.assertEquals("k-2026-10-05", client().regenerateSigningKey());

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/signing-key/regenerate", path);
        Assert.assertTrue(queryParameters.isEmpty());
        Assert.assertEquals(0, requestBody.length);
    }

    @Test
    public void regenerateSigningKeyManagedByAFileIsAClientException() {

        respond(409, "{\"message\":\"The signing key is managed by PHILTER_SIGNING_KEY_PATH; "
                + "replace the file and restart all instances.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class, () -> client().regenerateSigningKey());

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("409"));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("PHILTER_SIGNING_KEY_PATH"));
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

        respondNext(201, "");
        respond(200, POLICY_DETAILS);

        client().savePolicy("court", "{\"identifiers\":{}}", "Federal court filings", "Line one\nline two");

        // Philter refuses description and notes on the create, so they follow in a details request.
        Assert.assertEquals(2, requests.size());
        final RecordedRequest create = requests.get(0);
        Assert.assertEquals("POST", create.method);
        Assert.assertEquals("/api/policies", create.path);
        Assert.assertEquals(Map.of("name", "court"), create.queryParameters);
        Assert.assertEquals("{\"identifiers\":{}}", create.body);
        final RecordedRequest details = requests.get(1);
        Assert.assertEquals("PUT", details.method);
        Assert.assertEquals("/api/policies/court/details", details.path);
        Assert.assertTrue(details.queryParameters.isEmpty());
        Assert.assertEquals("{\"description\":\"Federal court filings\",\"notes\":\"Line one\\nline two\"}", details.body);

        requests.clear();
        respondNext(201, "");
        client().savePolicy("court", "{\"identifiers\":{}}", null, "Only the notes", OWNER);

        Assert.assertEquals(Map.of("name", "court", "owner", OWNER), requests.get(0).queryParameters);
        Assert.assertEquals(Map.of("owner", OWNER), requests.get(1).queryParameters);
        // A null description is left out, so the stored one is kept.
        Assert.assertEquals("{\"notes\":\"Only the notes\"}", requests.get(1).body);

        // Neither given: only the create is sent.
        requests.clear();
        respond(201, "");
        client().savePolicy("court", "{\"identifiers\":{}}", null, null);
        Assert.assertEquals(1, requests.size());
    }

    /** 1000 three-byte characters: within the notes limit, but 9000 bytes once percent-encoded. */
    private static final String LONG_NOTES = "\u8a18".repeat(1000);

    private static final String POLICY_DETAILS = "{\"name\":\"court\",\"revision\":1,\"managed\":false}";

    @Test
    public void longNotesTravelInTheDetailsBodyWhenSavingOrReplacingAPolicy() throws Exception {

        respondNext(201, "");
        respond(200, POLICY_DETAILS);
        client().savePolicy("court", "{\"identifiers\":{}}", null, LONG_NOTES);

        respondNext(200, "");
        client().replacePolicy("court", "{\"identifiers\":{}}", null, LONG_NOTES, OWNER);

        Assert.assertEquals(4, requests.size());
        for (final int i : new int[]{1, 3}) {
            final RecordedRequest details = requests.get(i);
            Assert.assertEquals("/api/policies/court/details", details.path);
            Assert.assertEquals(LONG_NOTES, com.google.gson.JsonParser.parseString(details.body).getAsJsonObject()
                    .get("notes").getAsString());
        }
        for (final RecordedRequest request : requests) {
            Assert.assertFalse(request.queryParameters.containsKey("notes"));
            Assert.assertFalse(request.queryParameters.containsKey("description"));
            Assert.assertTrue(request.rawQuery == null || request.rawQuery.length() < 100);
        }
    }

    @Test
    public void aFailedDetailsRequestAfterSavingAPolicyIsAClientException() {

        respondNext(201, "");
        respond(400, "{\"message\":\"The notes cannot be longer than 1000 characters.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().savePolicy("court", "{\"identifiers\":{}}", null, "n"));

        Assert.assertEquals(400, ex.getStatusCode());
        Assert.assertEquals("The notes cannot be longer than 1000 characters.", ex.getErrorMessage());
        // The create was sent and succeeded before the details request failed.
        Assert.assertEquals("POST", requests.get(0).method);
        Assert.assertEquals("/api/policies/court/details", requests.get(1).path);
    }

    @Test
    public void aFailedReplaceDoesNotSendTheDetails() {

        respond(409, "{\"message\":\"The policy changed.\",\"reason\":\"policy_changed\"}");

        Assert.assertThrows(ClientException.class,
                () -> client().replacePolicy("court", "{\"identifiers\":{}}", "d", "n"));

        Assert.assertEquals(1, requests.size());
    }

    @Test
    public void listManagedPoliciesParsesTheRecordedResponse() throws Exception {

        respond(200, recorded("policies-managed.json"));

        final List<ManagedPolicySummary> managed = client().listManagedPolicies(25, 50);

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/policies", path);
        Assert.assertEquals(Map.of("managed", "true", "offset", "25", "limit", "50"), queryParameters);

        Assert.assertEquals(3, managed.size());
        Assert.assertEquals("managed_common_pii", managed.get(0).getName());
        Assert.assertEquals("Common PII including names, emails, phone numbers, and SSNs", managed.get(0).getDescription());
        Assert.assertEquals("managed_healthcare_phi", managed.get(2).getName());

        respond(200, "[]");
        Assert.assertTrue(client().listManagedPolicies().isEmpty());
        Assert.assertEquals(Map.of("managed", "true"), queryParameters);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void getManagedPoliciesStillReturnsTheNames() throws Exception {

        respond(200, recorded("policies-managed.json"));

        Assert.assertEquals(List.of("managed_common_pii", "managed_financial_pii", "managed_healthcare_phi"),
                client().getManagedPolicies(25, 50));
        Assert.assertEquals(Map.of("managed", "true", "offset", "25", "limit", "50"), queryParameters);

        Assert.assertEquals(3, client().getManagedPolicies().size());
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

    @Test
    public void copyPolicyToANameInUseIsA409WithItsReason() {

        respond(409, "{\"message\":\"A policy with this name already exists.\",\"reason\":\"policy_exists\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().copyPolicy("court", "court-copy"));

        Assert.assertEquals(409, ex.getStatusCode());
        Assert.assertEquals("policy_exists", ex.getReason());
        Assert.assertEquals("A policy with this name already exists.", ex.getErrorMessage());
    }

    @Test
    public void rollbackPolicy404SaysWhatWasNotFound() {

        respond(404, "{\"message\":\"Revision 99 does not exist.\"}");
        ClientException ex = Assert.assertThrows(ClientException.class, () -> client().rollbackPolicy("court", 99));
        Assert.assertEquals(404, ex.getStatusCode());
        Assert.assertEquals("Revision 99 does not exist.", ex.getErrorMessage());

        respond(404, "{\"message\":\"Policy does not exist.\"}");
        ex = Assert.assertThrows(ClientException.class, () -> client().rollbackPolicy("nope", 1));
        Assert.assertEquals("Policy does not exist.", ex.getErrorMessage());

        // An owner that does not exist or may not be reached: a 404 with no body.
        respond(404, "");
        ex = Assert.assertThrows(ClientException.class, () -> client().rollbackPolicy("court", 1, OWNER));
        Assert.assertEquals(404, ex.getStatusCode());
        Assert.assertNull(ex.getErrorMessage());
    }

    @Test
    public void rollbackPolicyThatChangedConcurrentlyIsA409WithItsReason() {

        respond(409, "{\"message\":\"Policy changed concurrently. Reload and retry.\",\"reason\":\"policy_changed\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class, () -> client().rollbackPolicy("court", 1));

        Assert.assertEquals(409, ex.getStatusCode());
        Assert.assertEquals("policy_changed", ex.getReason());
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

    // Sign-in.

    @Test
    public void signInReturnsASessionKey() throws Exception {

        respond(200, "{\"apiKey\":\"sk_AbCdEfGhIjKlMnOpQrStUvWxYz012345\",\"username\":\"jordan\",\"scopes\":[\"redact\",\"contexts:read\"],"
                + "\"expiresAt\":\"2026-10-06T02:03:11.000Z\",\"idleExpiresAt\":\"2026-10-05T14:33:11.000Z\","
                + "\"passwordChangeRequired\":false,\"mfaEnrollmentRequired\":false}");

        final SignInResponse response = client().signIn("jordan", "the-users-password");

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/sign-in", path);
        Assert.assertTrue(queryParameters.isEmpty());
        Assert.assertEquals("application/json", header("Content-Type"));
        Assert.assertEquals("{\"username\":\"jordan\",\"password\":\"the-users-password\"}", requestBodyAsString());

        Assert.assertFalse(response.isMfaRequired());
        Assert.assertEquals("sk_AbCdEfGhIjKlMnOpQrStUvWxYz012345", response.getApiKey());
        Assert.assertEquals("jordan", response.getUsername());
        Assert.assertEquals(List.of("redact", "contexts:read"), response.getScopes());
        Assert.assertEquals("2026-10-06T02:03:11.000Z", response.getExpiresAt());
        Assert.assertEquals("2026-10-05T14:33:11.000Z", response.getIdleExpiresAt());
        Assert.assertFalse(response.isPasswordChangeRequired());
        Assert.assertFalse(response.isMfaEnrollmentRequired());
        Assert.assertNull(response.getChallenge());
    }

    @Test
    public void signInReturnsARestrictedKeyWhenThePasswordMustChange() throws Exception {

        respond(200, "{\"apiKey\":\"sk_AbCdEfGhIjKlMnOpQrStUvWxYz012345\",\"username\":\"jordan\","
                + "\"passwordChangeRequired\":true,\"mfaEnrollmentRequired\":false}");

        final SignInResponse response = client().signIn("jordan", "the-users-password");

        Assert.assertTrue(response.isPasswordChangeRequired());
        Assert.assertNotNull(response.getApiKey());
    }

    @Test
    public void signInReturnsAnMfaChallengeAndCompleteSignInReturnsTheKey() throws Exception {

        respond(200, "{\"mfaRequired\":true,\"challenge\":\"y4pY0m3l8f2rVq7d\","
                + "\"challengeExpiresAt\":\"2026-10-05T14:08:11.000Z\"}");

        final SignInResponse challenge = client().signIn("jordan", "the-users-password");

        Assert.assertTrue(challenge.isMfaRequired());
        Assert.assertEquals("y4pY0m3l8f2rVq7d", challenge.getChallenge());
        Assert.assertEquals("2026-10-05T14:08:11.000Z", challenge.getChallengeExpiresAt());
        Assert.assertNull(challenge.getApiKey());

        respond(200, "{\"apiKey\":\"sk_AbCdEfGhIjKlMnOpQrStUvWxYz012345\",\"username\":\"jordan\",\"scopes\":[\"redact\",\"contexts:read\"],"
                + "\"expiresAt\":\"2026-10-06T02:03:11.000Z\",\"idleExpiresAt\":\"2026-10-05T14:33:11.000Z\","
                + "\"passwordChangeRequired\":false,\"mfaEnrollmentRequired\":false}");

        final SignInResponse response = client().completeSignIn("y4pY0m3l8f2rVq7d", "123456");

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/sign-in/mfa", path);
        Assert.assertEquals("{\"challenge\":\"y4pY0m3l8f2rVq7d\",\"code\":\"123456\"}", requestBodyAsString());
        Assert.assertEquals("sk_AbCdEfGhIjKlMnOpQrStUvWxYz012345", response.getApiKey());
    }

    @Test
    public void signInWithBadCredentialsIsUnauthorizedWithPhilterMessage() {

        respond(401, "{\"message\":\"Invalid username or password.\"}");

        final UnauthorizedException ex = Assert.assertThrows(UnauthorizedException.class,
                () -> client().signIn("jordan", "wrong-password-0123"));

        Assert.assertEquals("Invalid username or password.", ex.getMessage());
    }

    @Test
    public void aLockedUsernameIsASignInLockedException() {

        responseHeaders.put("Retry-After", "900");
        respond(429, "{\"message\":\"Too many failed sign-ins for this username. Try again later.\",\"reason\":\"locked\"}");

        final SignInLockedException ex = Assert.assertThrows(SignInLockedException.class,
                () -> client().signIn("jordan", "the-users-password"));

        Assert.assertEquals("locked", ex.getReason());
        Assert.assertEquals(Integer.valueOf(900), ex.getRetryAfterSeconds());
        Assert.assertEquals("Too many failed sign-ins for this username. Try again later.", ex.getMessage());
    }

    @Test
    public void anAddressOverTheRateLimitIsASignInRateLimitedException() {

        responseHeaders.put("Retry-After", "60");
        respond(429, "{\"message\":\"Too many sign-in requests. Try again later.\",\"reason\":\"rate_limited\"}");

        final SignInRateLimitedException ex = Assert.assertThrows(SignInRateLimitedException.class,
                () -> client().completeSignIn("y4pY0m3l8f2rVq7d", "123456"));

        Assert.assertEquals("rate_limited", ex.getReason());
        Assert.assertEquals(Integer.valueOf(60), ex.getRetryAfterSeconds());
        // A subclass of ClientException, so existing handlers still catch it.
        Assert.assertTrue(ex instanceof ClientException);
    }

    @Test
    public void a429WithoutAKnownReasonIsAClientException() {

        respond(429, "{\"message\":\"Slow down.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().signIn("jordan", "the-users-password"));

        Assert.assertFalse(ex instanceof SignInThrottledException);
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("HTTP 429"));
    }

    @Test
    public void aLockedMfaIsAClientExceptionWithPhilterMessage() {

        respond(403, "{\"message\":\"MFA is locked after repeated bad codes. An administrator must unlock it.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().completeSignIn("y4pY0m3l8f2rVq7d", "000000"));

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("HTTP 403"));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("MFA is locked"));
    }

    @Test
    public void signOutRevokesTheCallingKey() throws Exception {

        respond(204, "");

        client().signOut();

        Assert.assertEquals("DELETE", method);
        Assert.assertEquals("/api/api-keys/current", path);
        Assert.assertTrue(queryParameters.isEmpty());
    }

    @Test
    public void signingOutALongLivedKeyIsAClientException() {

        respond(409, "{\"message\":\"A long-lived key cannot revoke itself.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class, () -> client().signOut());

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("HTTP 409"));
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

        // No role means Philter's default, and no password a user who can only use API keys, so neither is sent.
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

        respond(201, "{\"id\":\"6a0f1c2e9b1d4e3f2a1b0c9d\",\"username\":\"ci\",\"apiKey\":\"pk-secret\","
                + "\"scopes\":[\"redact\",\"policies:read\"]}");

        final CreatedApiKeyResponse response =
                client().createApiKey("ci", List.of("redact", "policies:read"));

        Assert.assertEquals("6a0f1c2e9b1d4e3f2a1b0c9d", response.getId());
        Assert.assertEquals("ci", response.getUsername());
        Assert.assertEquals("pk-secret", response.getApiKey());
        Assert.assertEquals(List.of("redact", "policies:read"), response.getScopes());

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/users/ci/api-keys", path);
        Assert.assertEquals("application/json", header("Content-Type"));
        Assert.assertEquals("{\"scopes\":[\"redact\",\"policies:read\"]}", requestBodyAsString());
    }

    @Test
    public void listApiKeyScopesParsesTheRecordedResponse() throws Exception {

        respond(200, recorded("api-key-scopes.json"));

        final GetApiKeyScopesResponse response = client().listApiKeyScopes();

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/api-keys/scopes", path);
        Assert.assertTrue(queryParameters.isEmpty());
        Assert.assertEquals("application/json", header("Accept"));

        final List<ApiKeyScopeDescription> scopes = response.getScopes();
        Assert.assertEquals(25, scopes.size());
        // In the order Philter declares them.
        Assert.assertEquals("redact", scopes.get(0).getName());
        Assert.assertEquals("Redact text and documents, and explain redactions.", scopes.get(0).getDescription());
        Assert.assertEquals("reidentify", scopes.get(scopes.size() - 1).getName());
        for (final ApiKeyScopeDescription scope : scopes) {
            Assert.assertNotNull(scope.getName());
            Assert.assertFalse(scope.getName(), scope.getDescription() == null || scope.getDescription().isEmpty());
        }

        // The names are the ones createApiKey takes.
        final java.util.Set<String> names = new java.util.HashSet<>();
        scopes.forEach(scope -> names.add(scope.getName()));
        Assert.assertTrue(names.containsAll(List.of("redact", "policies:read", "api-keys:write", "ledger:export")));
    }

    @Test
    public void getApiKeysForTheCaller() throws Exception {

        respond(200, "{\"apiKeys\":[{\"id\":\"6a0f1c2e9b1d4e3f2a1b0c9d\",\"prefix\":\"sk_AbCdEfGhI...\",\"scopes\":[\"redact\"],"
                + "\"created\":\"2026-10-05T14:03:11.000Z\",\"bootstrap\":true}],\"total\":30}");

        final GetApiKeysResponse response = client().getApiKeys(null, 25, 50);

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/api-keys", path);
        Assert.assertEquals(Map.of("offset", "25", "limit", "50"), queryParameters);
        Assert.assertEquals("application/json", header("Accept"));

        Assert.assertEquals(30, response.getTotal());
        Assert.assertEquals(1, response.getApiKeys().size());

        final ApiKey key = response.getApiKeys().get(0);
        Assert.assertEquals("6a0f1c2e9b1d4e3f2a1b0c9d", key.getId());
        Assert.assertEquals("sk_AbCdEfGhI...", key.getPrefix());
        Assert.assertEquals(List.of("redact"), key.getScopes());
        Assert.assertEquals("2026-10-05T14:03:11.000Z", key.getCreated());
        Assert.assertTrue(key.isBootstrap());
    }

    @Test
    public void getApiKeysForAnOwnerUsesTheUserPath() throws Exception {

        respond(200, "{\"apiKeys\":[],\"total\":0}");

        client().getApiKeys("ci user", 25, 50);

        // Philter names the user in the path, not with an owner parameter.
        Assert.assertEquals("/api/users/ci%20user/api-keys", rawPath);
        Assert.assertEquals(Map.of("offset", "25", "limit", "50"), queryParameters);

        respond(200, "{\"apiKeys\":[],\"total\":0}");
        client().getApiKeys();
        Assert.assertEquals("/api/api-keys", path);
        Assert.assertTrue(queryParameters.isEmpty());
    }

    @Test
    public void createApiKeyForTheCaller() throws Exception {

        respond(201, "{\"id\":\"6a0f1c2e9b1d4e3f2a1b0c9e\",\"username\":\"ci\",\"apiKey\":\"pk-new\",\"scopes\":[\"redact\"]}");

        final CreatedApiKeyResponse response = client().createApiKey(List.of("redact"));

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/api-keys", path);
        Assert.assertEquals("application/json", header("Content-Type"));
        Assert.assertEquals("{\"scopes\":[\"redact\"]}", requestBodyAsString());

        Assert.assertEquals("6a0f1c2e9b1d4e3f2a1b0c9e", response.getId());
        Assert.assertEquals("pk-new", response.getApiKey());
    }

    @Test
    public void setApiKeyScopes() throws Exception {

        respond(200, "{\"id\":\"6a0f1c2e9b1d4e3f2a1b0c9d\",\"prefix\":\"sk_AbCdEfGhI...\","
                + "\"scopes\":[\"redact\",\"policies:read\"],\"bootstrap\":false}");

        final ApiKey key = client().setApiKeyScopes("6a0f1c2e9b1d4e3f2a1b0c9d", List.of("redact", "policies:read"));

        Assert.assertEquals("PUT", method);
        Assert.assertEquals("/api/api-keys/6a0f1c2e9b1d4e3f2a1b0c9d/scopes", path);
        Assert.assertTrue(queryParameters.isEmpty());
        Assert.assertEquals("application/json", header("Content-Type"));
        Assert.assertEquals("{\"scopes\":[\"redact\",\"policies:read\"]}", requestBodyAsString());
        Assert.assertEquals(List.of("redact", "policies:read"), key.getScopes());
    }

    @Test
    public void revokeApiKey() throws Exception {

        respond(204, "");

        client().revokeApiKey("6a0f1c2e9b1d4e3f2a1b0c9d");

        Assert.assertEquals("DELETE", method);
        Assert.assertEquals("/api/api-keys/6a0f1c2e9b1d4e3f2a1b0c9d", path);
        Assert.assertTrue(queryParameters.isEmpty());
    }

    @Test
    public void revokingTheCallingKeyIsAClientException() {

        respond(409, "{\"message\":\"This is the key making the request. Revoke it with another key.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().revokeApiKey("6a0f1c2e9b1d4e3f2a1b0c9d"));

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("409"));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("Revoke it with another key."));
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

    // Names that cannot be used in a request path. Philter refuses them now, but items created before
    // that may still have one, and a path containing one is refused before it reaches Philter.
    private static final List<String> PATH_UNSAFE_NAMES = List.of(
            "a/b", "a\\b", "a;b", "a%b", "a\u0007b", "tab\there", "del\u007f", "c1\u0085", ".", "..");

    // Allowed in a path once percent-encoded.
    private static final List<String> PATH_SAFE_NAMES = List.of(
            "c1", "tenant a", "v1.2", "...", "a..b", "café", "記録", "a+b&c=d?e#f");

    @Test
    public void deletesUseTheQueryRouteOnlyForNamesThatCannotBeUsedInAPath() throws Exception {

        final PhilterClient c = client();

        for (final String name : PATH_UNSAFE_NAMES) {

            verify("DELETE", "/api/contexts", Map.of("name", name), "{}", () -> c.deleteContext(name));
            Assert.assertEquals(Set.of("name"), queryParameters.keySet());
            verify("DELETE", "/api/contexts", Map.of("name", name, "owner", OWNER), "{}", () -> c.deleteContext(name, OWNER));

            verify("DELETE", "/api/lists", Map.of("name", name), "", () -> c.deleteList(name));
            Assert.assertEquals(Set.of("name"), queryParameters.keySet());
            verify("DELETE", "/api/lists", Map.of("name", name, "owner", OWNER), "", () -> c.deleteList(name, OWNER));

            verify("DELETE", "/api/holds", Map.of("reference", name), "", () -> c.deleteHold(name));
            Assert.assertEquals(Set.of("reference"), queryParameters.keySet());
            verify("DELETE", "/api/holds", Map.of("reference", name, "owner", OWNER), "", () -> c.deleteHold(name, OWNER));

        }

        for (final String name : PATH_SAFE_NAMES) {

            verify("DELETE", "/api/contexts/" + name, Map.of(), "{}", () -> c.deleteContext(name));
            Assert.assertTrue(queryParameters.isEmpty());
            verify("DELETE", "/api/contexts/" + name, Map.of("owner", OWNER), "{}", () -> c.deleteContext(name, OWNER));

            verify("DELETE", "/api/lists/" + name, Map.of(), "", () -> c.deleteList(name));
            Assert.assertTrue(queryParameters.isEmpty());
            verify("DELETE", "/api/lists/" + name, Map.of("owner", OWNER), "", () -> c.deleteList(name, OWNER));

            verify("DELETE", "/api/holds/" + name, Map.of(), "", () -> c.deleteHold(name));
            Assert.assertTrue(queryParameters.isEmpty());
            verify("DELETE", "/api/holds/" + name, Map.of("owner", OWNER), "", () -> c.deleteHold(name, OWNER));

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

    // Audit log.

    @Test
    public void getAuditLogSendsEveryFilterAndMapsTheEvents() throws Exception {

        respond(200, "{\"events\":[{\"timestamp\":\"2026-09-12T16:12:06.481+00:00\",\"event\":\"policy_deleted\","
                + "\"requestId\":\"b0e1f6c2-1d3a-4f88-9a7e-2c5d0a6f1b34\",\"apiKeyId\":\"6aa5792a403075186a843960\","
                + "\"associatedObject\":\"6aa57a01403075186a843971\",\"clientIpAddress\":\"192.0.2.10\","
                + "\"details\":\"policy: audit-probe-policy, source: api\"},"
                + "{\"timestamp\":\"2026-09-12T16:10:00.000+00:00\",\"event\":\"policy_deleted\"}],\"total\":42}");

        final GetAuditLogResponse response = client().getAuditLog("policy_deleted",
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-13T00:00:00Z"), OWNER, 25, 50);

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/audit", path);
        Assert.assertEquals(Map.of("event", "policy_deleted", "from", "2026-09-01T00:00:00Z",
                "to", "2026-09-13T00:00:00Z", "owner", OWNER, "offset", "25", "limit", "50"), queryParameters);
        Assert.assertEquals("application/json", header("Accept"));

        Assert.assertEquals(42, response.getTotal());
        Assert.assertEquals(2, response.getEvents().size());

        final AuditEvent first = response.getEvents().get(0);
        Assert.assertEquals("2026-09-12T16:12:06.481+00:00", first.getTimestamp());
        Assert.assertEquals("policy_deleted", first.getEvent());
        Assert.assertEquals("b0e1f6c2-1d3a-4f88-9a7e-2c5d0a6f1b34", first.getRequestId());
        Assert.assertEquals("6aa5792a403075186a843960", first.getApiKeyId());
        Assert.assertEquals("6aa57a01403075186a843971", first.getAssociatedObject());
        Assert.assertEquals("192.0.2.10", first.getClientIpAddress());
        Assert.assertEquals("policy: audit-probe-policy, source: api", first.getDetails());

        // Philter leaves out the fields an event did not record.
        final AuditEvent second = response.getEvents().get(1);
        Assert.assertNull(second.getRequestId());
        Assert.assertNull(second.getDetails());
    }

    @Test
    public void getAuditLogShortFormSendsNoFilters() throws Exception {

        respond(200, "{\"events\":[],\"total\":0}");

        Assert.assertEquals(0, client().getAuditLog().getTotal());

        Assert.assertEquals("/api/audit", path);
        Assert.assertTrue(queryParameters.isEmpty());
    }

    @Test
    public void getAuditLogSurfacesPhilterReasonForAnUnknownEvent() {

        respond(400, "The event parameter is not an audit event type.");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().getAuditLog("not_an_event", null, null, null, null, null));

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("HTTP 400"));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("not an audit event type"));
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
        // With a description or notes, both the create and the details request carry the owner.
        requests.clear();
        verifyOwner("/api/policies/p1/details", "{}", () -> c.savePolicy("p1", "{}", "d", "n", OWNER));
        Assert.assertEquals("/api/policies", requests.get(0).path);
        Assert.assertEquals(OWNER, requests.get(0).queryParameters.get("owner"));
        requests.clear();
        verifyOwner("/api/policies/p1/details", "{}", () -> c.replacePolicy("p1", "{}", "d", "n", OWNER));
        Assert.assertEquals("/api/policies/p1", requests.get(0).path);
        Assert.assertEquals(OWNER, requests.get(0).queryParameters.get("owner"));
        verifyOwner("/api/policies/p1", "", () -> c.replacePolicy("p1", "{}", OWNER));
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
        verifyPaged("/api/audit", "{}", () -> c.getAuditLog(null, null, null, OWNER, 25, 50));
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

    @Test
    public void createUserWithAPassword() throws Exception {

        respond(201, "{\"username\":\"jordan\",\"role\":\"user\"}");

        client().createUser("jordan", null, null, "a-password-of-16-or-more");

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/users", path);
        Assert.assertEquals("{\"username\":\"jordan\",\"password\":\"a-password-of-16-or-more\"}", requestBodyAsString());
    }

    @Test
    public void getUserMapsThePasswordAndMfaFields() throws Exception {

        respond(200, "{\"username\":\"jordan\",\"role\":\"user\",\"active\":true,\"passwordSet\":true,"
                + "\"passwordChangeRequired\":true,\"mfaEnabled\":true,\"mfaLocked\":true}");

        final User user = client().getUser("jordan");

        Assert.assertTrue(user.isPasswordSet());
        Assert.assertTrue(user.isPasswordChangeRequired());
        Assert.assertTrue(user.isMfaEnabled());
        Assert.assertTrue(user.isMfaLocked());
    }

    @Test
    public void changePassword() throws Exception {

        respond(204, "");

        client().changePassword("the-current-password", "a-new-password-of-16-or-more");

        Assert.assertEquals("PUT", method);
        Assert.assertEquals("/api/users/me/password", path);
        Assert.assertEquals("application/json", header("Content-Type"));
        Assert.assertEquals("{\"currentPassword\":\"the-current-password\","
                + "\"newPassword\":\"a-new-password-of-16-or-more\"}", requestBodyAsString());
    }

    @Test
    public void aWrongCurrentPasswordIsAClientException() {

        respond(403, "{\"message\":\"The current password is not correct.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().changePassword("wrong-password-0123", "a-new-password-of-16-or-more"));

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("HTTP 403"));
    }

    @Test
    public void setPassword() throws Exception {

        respond(204, "");

        client().setPassword("jordan user", "a-password-of-16-or-more");

        Assert.assertEquals("PUT", method);
        Assert.assertEquals("/api/users/jordan%20user/password", rawPath);
        Assert.assertEquals("{\"password\":\"a-password-of-16-or-more\"}", requestBodyAsString());
    }

    @Test
    public void startMfaEnrollment() throws Exception {

        respond(200, "{\"secret\":\"JBSWY3DPEHPK3PXP\","
                + "\"otpauthUri\":\"otpauth://totp/Philter:jordan?secret=JBSWY3DPEHPK3PXP&issuer=Philter\"}");

        final MfaEnrollment enrollment = client().startMfaEnrollment();

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/users/me/mfa", path);
        Assert.assertEquals(0, requestBody.length);
        Assert.assertEquals("JBSWY3DPEHPK3PXP", enrollment.getSecret());
        Assert.assertEquals("otpauth://totp/Philter:jordan?secret=JBSWY3DPEHPK3PXP&issuer=Philter", enrollment.getOtpauthUri());
    }

    @Test
    public void confirmAndRemoveMfaEnrollmentSendTheCode() throws Exception {

        respond(204, "");

        client().confirmMfaEnrollment("123456");
        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/users/me/mfa/confirm", path);
        Assert.assertEquals("{\"code\":\"123456\"}", requestBodyAsString());

        client().removeMfaEnrollment("654321");
        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/users/me/mfa/remove", path);
        Assert.assertEquals("{\"code\":\"654321\"}", requestBodyAsString());
    }

    @Test
    public void removeAndUnlockAnotherUsersMfa() throws Exception {

        respond(204, "");

        client().removeUserMfa("jordan");
        Assert.assertEquals("DELETE", method);
        Assert.assertEquals("/api/users/jordan/mfa", path);

        client().unlockUserMfa("jordan");
        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/users/jordan/mfa/unlock", path);
        Assert.assertEquals(0, requestBody.length);
    }

    @Test
    public void revokeSessionKeysReturnsTheCount() throws Exception {

        respond(200, "{\"revoked\":2}");

        Assert.assertEquals(2, client().revokeSessionKeys("jordan"));

        Assert.assertEquals("DELETE", method);
        Assert.assertEquals("/api/users/jordan/session-keys", path);
    }

    @Test
    public void getApiKeysMapsSessionKeyFields() throws Exception {

        respond(200, "{\"apiKeys\":[{\"id\":\"6a0f1c2e9b1d4e3f2a1b0c9d\",\"prefix\":\"sk_AbCdEfGhI...\","
                + "\"scopes\":[\"redact\"],\"bootstrap\":false,\"session\":true,"
                + "\"expiresAt\":\"2026-10-06T02:03:11.000Z\",\"idleExpiresAt\":\"2026-10-05T14:33:11.000Z\","
                + "\"lastUsedAt\":\"2026-10-05T14:03:11.000Z\"},"
                + "{\"id\":\"6a0f1c2e9b1d4e3f2a1b0c9e\",\"scopes\":[\"redact\"],\"session\":false}],\"total\":2}");

        final GetApiKeysResponse response = client().getApiKeys();

        final ApiKey session = response.getApiKeys().get(0);
        Assert.assertTrue(session.isSession());
        Assert.assertEquals("2026-10-06T02:03:11.000Z", session.getExpiresAt());
        Assert.assertEquals("2026-10-05T14:33:11.000Z", session.getIdleExpiresAt());
        Assert.assertEquals("2026-10-05T14:03:11.000Z", session.getLastUsedAt());

        final ApiKey longLived = response.getApiKeys().get(1);
        Assert.assertFalse(longLived.isSession());
        Assert.assertNull(longLived.getExpiresAt());
    }

    // Recorded from Philter: sign-in, then the user's keys with each session filter. The key values are
    // replaced with placeholders; the IDs are as recorded.
    @Test
    public void signInReturnsTheSessionKeyIdThatTheKeyListingUses() throws Exception {

        respond(200, recorded("sign-in.json"));
        final SignInResponse signedIn = client().signIn("rec-user", "the-users-password");
        Assert.assertEquals("6ac536512d9abffc9918d17a", signedIn.getId());

        respond(200, recorded("api-keys.json"));
        final GetApiKeysResponse all = client().getApiKeys();
        Assert.assertEquals(2, all.getTotal());
        Assert.assertTrue(all.getApiKeys().stream()
                .anyMatch(key -> key.getId().equals(signedIn.getId()) && key.isSession()));
    }

    @Test
    public void completeSignInReturnsTheSessionKeyId() throws Exception {

        respond(200, recorded("sign-in-mfa-challenge.json"));
        final SignInResponse challenge = client().signIn("rec-user", "the-users-password");
        Assert.assertTrue(challenge.isMfaRequired());
        Assert.assertNull(challenge.getId());

        respond(200, recorded("sign-in-mfa.json"));
        final SignInResponse signedIn = client().completeSignIn(challenge.getChallenge(), "123456");
        Assert.assertEquals("/api/sign-in/mfa", path);
        Assert.assertEquals("6ac536522d9abffc9918d192", signedIn.getId());
        Assert.assertNotNull(signedIn.getApiKey());
    }

    @Test
    public void getApiKeysFiltersBySession() throws Exception {

        respond(200, recorded("api-keys-session.json"));
        GetApiKeysResponse response = client().getApiKeys(null, 25, 50, true);
        Assert.assertEquals("/api/api-keys", path);
        Assert.assertEquals(Map.of("offset", "25", "limit", "50", "session", "true"), queryParameters);
        Assert.assertEquals(1, response.getTotal());
        Assert.assertTrue(response.getApiKeys().get(0).isSession());
        Assert.assertEquals("6ac536512d9abffc9918d17a", response.getApiKeys().get(0).getId());

        respond(200, recorded("api-keys-long-lived.json"));
        response = client().getApiKeys(null, null, null, false);
        Assert.assertEquals(Map.of("session", "false"), queryParameters);
        Assert.assertEquals(1, response.getTotal());
        Assert.assertFalse(response.getApiKeys().get(0).isSession());
        Assert.assertNull(response.getApiKeys().get(0).getExpiresAt());

        respond(200, recorded("api-keys.json"));
        response = client().getApiKeys(null, null, null, null);
        Assert.assertTrue(queryParameters.isEmpty());
        Assert.assertEquals(2, response.getTotal());

        // An administrator listing another user's keys.
        respond(200, "{\"apiKeys\":[],\"total\":0}");
        client().getApiKeys("ci user", null, null, true);
        Assert.assertEquals("/api/users/ci%20user/api-keys", rawPath);
        Assert.assertEquals(Map.of("session", "true"), queryParameters);
    }

    @Test
    public void changingTheCallingKeysOwnScopesIsA409() {

        // Recorded from Philter.
        respond(409, "{\"message\":\"This is the key making the request. Change its scopes with another key.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().setApiKeyScopes("6ac536512d9abffc9918d17a", List.of("redact")));

        Assert.assertEquals(409, ex.getStatusCode());
        Assert.assertEquals("This is the key making the request. Change its scopes with another key.",
                ex.getErrorMessage());
        Assert.assertNull(ex.getReason());
    }

    @Test
    public void adminSettingsCarryTheMfaSettings() throws Exception {

        respond(200, "{\"mfaAvailable\":true,\"mfaRequired\":false,\"warnings\":[]}");

        final UpdateAdminSettingsRequest request = new UpdateAdminSettingsRequest();
        request.setMfaAvailable(true);

        final AdminSettings settings = client().updateAdminSettings(request);

        Assert.assertEquals("{\"mfaAvailable\":true}", requestBodyAsString());
        Assert.assertTrue(settings.isMfaAvailable());
        Assert.assertFalse(settings.isMfaRequired());
    }

    @Test
    public void unauthorizedCarriesPhilterMessage() {

        respond(401, "{\"error\": \"Unauthorized\", \"message\": \"Invalid or missing credentials\"}");

        final UnauthorizedException ex = Assert.assertThrows(UnauthorizedException.class, () -> client().getPolicies());

        Assert.assertEquals("Invalid or missing credentials", ex.getMessage());
    }

    @Test
    public void unauthorizedWithoutABodyKeepsTheFixedMessage() {

        respond(401, "");

        final UnauthorizedException ex = Assert.assertThrows(UnauthorizedException.class, () -> client().getPolicies());

        Assert.assertEquals(PhilterClient.UNAUTHORIZED, ex.getMessage());
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

    // Typed models, parsed from responses recorded from a running Philter.

    private static String recorded(final String name) throws IOException {
        try (java.io.InputStream in = PhilterClientMockTest.class.getResourceAsStream("/recorded/" + name)) {
            Assert.assertNotNull("missing recording " + name, in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void listContextsParsesTheRecordedResponse() throws Exception {

        respond(200, recorded("contexts.json"));
        Assert.assertEquals(List.of("default", "ledgered"), client().listContexts(OWNER, 25, 50).getContexts());
        Assert.assertEquals("/api/contexts", path);
        Assert.assertEquals(Map.of("owner", OWNER, "offset", "25", "limit", "50"), queryParameters);

        respond(200, recorded("contexts-all-users.json"));
        final GetContextsAcrossUsersResponse all = client().listContextsAcrossUsers(0, 5);
        Assert.assertEquals("true", queryParameter("all_users"));
        Assert.assertEquals(3, all.getContexts().size());
        Assert.assertEquals("ledgered", all.getContexts().get(2).getName());
        Assert.assertTrue(all.getContexts().get(2).getOwner().endsWith("@example.com"));
    }

    @Test
    public void getContextDetailsAndEntriesParseTheRecordedResponses() throws Exception {

        respond(200, recorded("context.json"));
        final ContextDetails details = client().getContextDetails("ledgered");
        Assert.assertEquals("/api/contexts/ledgered", path);
        Assert.assertEquals(1, details.getSize());
        Assert.assertEquals(Map.of("PERSON", 1L), details.getFilterTypes());
        Assert.assertEquals(0, details.getUntyped());
        Assert.assertTrue(details.isLedger());
        Assert.assertFalse(details.isEntityTypeDisambiguation());

        respond(200, recorded("context-entries.json"));
        final GetContextEntriesResponse entries = client().listContextEntries("ledgered", null, 0, 25);
        Assert.assertEquals("/api/contexts/ledgered/entries", path);
        Assert.assertEquals(1, entries.getTotal());
        final ContextEntry entry = entries.getEntries().get(0);
        Assert.assertEquals("52c4ba942abd91b9e64b9634", entry.getId());
        Assert.assertEquals("{{{REDACTED-person}}}", entry.getReplacement());
        Assert.assertEquals("PERSON", entry.getFilterType());
        Assert.assertEquals(0, entry.getReads());
        Assert.assertEquals("2026-10-06T09:31:01.591-04:00", entry.getTimestamp());
    }

    @Test
    public void customListsParseTheRecordedResponses() throws Exception {

        respond(200, recorded("lists.json"));
        final List<CustomListSummary> lists = client().listCustomLists();
        Assert.assertEquals("/api/lists", path);
        Assert.assertEquals(1, lists.size());
        Assert.assertEquals("customer-names", lists.get(0).getName());
        Assert.assertEquals("Customer names", lists.get(0).getDescription());
        Assert.assertEquals(2, lists.get(0).getSize());
        Assert.assertNull(lists.get(0).getOwner());

        respond(200, recorded("lists-all-users.json"));
        final List<CustomListSummary> all = client().listCustomListsAcrossUsers(0, 5);
        Assert.assertEquals("true", queryParameter("all_users"));
        Assert.assertTrue(all.get(0).getOwner().endsWith("@example.com"));

        respond(200, recorded("list.json"));
        final GetListsResponse list = client().getList("customer-names");
        Assert.assertEquals("Customer names", list.getDescription());
        Assert.assertEquals(List.of("Jordan Example", "Avery Example"), list.getLists());
    }

    @Test
    public void redactListsParseAndTakeATypedRequest() throws Exception {

        respond(200, recorded("redact-lists.json"));
        final RedactLists lists = client().listRedactLists();
        Assert.assertEquals("/api/redact-lists", path);
        Assert.assertEquals(List.of("Project Falcon"), lists.getAlwaysRedact());
        Assert.assertEquals(List.of("Philter"), lists.getNeverRedact());

        respond(200, "{\"message\":\"Saved.\"}");
        client().createRedactList(new RedactListsRequest(List.of("Project Falcon"), List.of("Philter")));
        Assert.assertEquals("POST", method);
        Assert.assertEquals("{\"alwaysRedact\":[\"Project Falcon\"],\"neverRedact\":[\"Philter\"]}", requestBodyAsString());

        client().updateRedactList(new RedactListsRequest(List.of("Project Osprey"), null), OWNER);
        Assert.assertEquals("PUT", method);
        Assert.assertEquals(OWNER, queryParameter("owner"));
        // A null list is left out, so an append adds nothing to it.
        Assert.assertEquals("{\"alwaysRedact\":[\"Project Osprey\"]}", requestBodyAsString());
    }

    @Test
    public void ledgerResponsesParseTheRecordedResponses() throws Exception {

        respond(200, recorded("ledger.json"));
        final GetLedgerResponse ledger = client().listLedgerChains(null);
        Assert.assertEquals("/api/ledger", path);
        Assert.assertEquals(1, ledger.getTotal());
        final LedgerEntry head = ledger.getChains().get(0);
        Assert.assertEquals("note.txt", head.getFilename());
        Assert.assertEquals("[genesis]", head.getPreviousHash());
        Assert.assertEquals("ssn-only", head.getPolicyName());
        Assert.assertEquals("221ec36b04081bcc", head.getSigningKeyId());
        Assert.assertNotNull(head.getSignature());
        Assert.assertNull("reading a chain does not return the original value", head.getToken());

        respond(200, recorded("ledger-all-users.json"));
        Assert.assertTrue(client().listLedgerChainsAcrossUsers(0, 5).getChains().get(0).getOwner().endsWith("@example.com"));
        Assert.assertEquals("true", queryParameter("all_users"));

        respond(200, recorded("ledger-chain.json"));
        final LedgerChain chain = client().getLedgerChain("50f6010d-6f44-44a9-85a8-830b93d13c17");
        Assert.assertEquals("/api/ledger/50f6010d-6f44-44a9-85a8-830b93d13c17", path);
        Assert.assertTrue(chain.isValid());
        Assert.assertEquals(Boolean.TRUE, chain.getHashChainValid());
        Assert.assertEquals(Boolean.TRUE, chain.getSignaturesValid());
        Assert.assertEquals(Integer.valueOf(2), chain.getSignedEntries());
        Assert.assertEquals(Integer.valueOf(0), chain.getUnsignedEntries());
        Assert.assertNull(chain.getValidationError());
        Assert.assertEquals(2, chain.getEntries().size());
        Assert.assertEquals("ssn", chain.getEntries().get(1).getType());
        Assert.assertNull(chain.getEntries().get(1).getToken());

        respond(200, recorded("ledger-export.json"));
        final LedgerExport export = client().getLedgerExport("50f6010d-6f44-44a9-85a8-830b93d13c17");
        Assert.assertEquals("/api/ledger/50f6010d-6f44-44a9-85a8-830b93d13c17/export", path);
        Assert.assertEquals(3, export.getVersion());
        Assert.assertEquals(2, export.getCount());
        Assert.assertTrue(export.getSigningKeys().get("221ec36b04081bcc").startsWith("-----BEGIN PUBLIC KEY-----"));
        Assert.assertEquals("an export carries the original value", "123-45-6789", export.getEntries().get(1).getToken());

        respond(200, recorded("ledger-valid.json"));
        final LedgerChain verified = client().verifyLedgerChain("50f6010d-6f44-44a9-85a8-830b93d13c17");
        Assert.assertEquals("/api/ledger/50f6010d-6f44-44a9-85a8-830b93d13c17/valid", path);
        Assert.assertTrue(verified.isValid());
        Assert.assertNull(verified.getEntries());
        Assert.assertNull(verified.getValidationError());
    }

    // Recorded from Philter with a genesis entry's document hash rewritten in the database.
    @Test
    public void aTamperedLedgerChainFailsItsHashCheck() throws Exception {

        for (final String recording : List.of("ledger-chain-tampered.json", "ledger-valid-tampered.json")) {

            respond(200, recorded(recording));
            final LedgerChain chain = recording.startsWith("ledger-chain")
                    ? client().getLedgerChain("doc-tampered")
                    : client().verifyLedgerChain("doc-tampered");

            Assert.assertFalse(recording, chain.isValid());
            Assert.assertNull(recording, chain.getValidationError());
            Assert.assertEquals(recording, Boolean.FALSE, chain.getHashChainValid());
            Assert.assertEquals(recording, Boolean.TRUE, chain.getSignaturesValid());
            Assert.assertEquals(recording, Integer.valueOf(3), chain.getSignedEntries());
            Assert.assertEquals(recording, Integer.valueOf(0), chain.getUnsignedEntries());
        }

        Assert.assertNull(client().verifyLedgerChain("doc-tampered").getEntries());
        respond(200, recorded("ledger-chain-tampered.json"));
        Assert.assertEquals(3, client().getLedgerChain("doc-tampered").getEntries().size());
    }

    // Recorded from Philter with an entry's encrypted token replaced, so it no longer decrypts.
    @Test
    public void aLedgerChainThatCouldNotBeCheckedHasAValidationErrorAndNoCheckResults() throws Exception {

        for (final String recording : List.of("ledger-chain-unverifiable.json", "ledger-valid-unverifiable.json")) {

            respond(200, recorded(recording));
            final LedgerChain chain = recording.startsWith("ledger-chain")
                    ? client().getLedgerChain("doc-unverifiable")
                    : client().verifyLedgerChain("doc-unverifiable");

            Assert.assertEquals(recording, "doc-unverifiable", chain.getDocumentId());
            Assert.assertFalse(recording, chain.isValid());
            Assert.assertEquals(recording, "The chain could not be validated, so it is not reported as valid. "
                    + "An entry could not be read or checked.", chain.getValidationError());
            // Not false: the checks did not complete, which is not evidence of tampering.
            Assert.assertNull(recording, chain.getHashChainValid());
            Assert.assertNull(recording, chain.getSignaturesValid());
            Assert.assertNull(recording, chain.getSignedEntries());
            Assert.assertNull(recording, chain.getUnsignedEntries());
            Assert.assertNull(recording, chain.getEntries());
        }
    }

    @Test
    public void documentsParseTheRecordedResponses() throws Exception {

        respond(200, recorded("documents.json"));
        final GetDocumentsResponse documents = client().listDocuments();
        Assert.assertEquals("/api/documents", path);
        final DocumentSummary document = documents.getDocuments().get(0);
        Assert.assertEquals("bb00a978-5ac9-4189-896a-bd1e281645ee", document.getDocumentId());
        Assert.assertEquals("scan.pdf", document.getFileName());
        Assert.assertEquals("PENDING", document.getStatus());
        Assert.assertNotNull(document.getTimestamp());

        respond(200, recorded("document-status.json"));
        final DocumentStatus status = client().getDocumentState("bb00a978-5ac9-4189-896a-bd1e281645ee");
        Assert.assertEquals("/api/documents/bb00a978-5ac9-4189-896a-bd1e281645ee/status", path);
        Assert.assertEquals("PENDING", status.getStatus());
        Assert.assertEquals(64, status.getEffectiveConfigurationHash().length());
        Assert.assertNull(status.getError());
    }

    @Test
    public void signingKeysParseTheRecordedResponses() throws Exception {

        respond(200, recorded("signing-key.json"));
        final SigningKey active = client().getSigningKeyDetails();
        Assert.assertEquals("/api/signing-key", path);
        Assert.assertEquals("221ec36b04081bcc", active.getKeyId());
        Assert.assertTrue(active.getPem().startsWith("-----BEGIN PUBLIC KEY-----"));
        Assert.assertEquals("EC", active.getJwk().get("kty"));
        Assert.assertEquals("P-256", active.getJwk().get("crv"));
        Assert.assertTrue(active.getFingerprint().startsWith("22:1e:"));
        Assert.assertNull(active.getActive());

        respond(200, recorded("signing-key-by-id.json"));
        final SigningKey byId = client().getSigningKeyDetails("221ec36b04081bcc");
        Assert.assertEquals("/api/signing-key/221ec36b04081bcc", path);
        Assert.assertEquals(Boolean.TRUE, byId.getActive());
        Assert.assertNull(byId.getJwk());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void theDeprecatedStringMethodsStillReturnTheBodyUnchanged() throws Exception {

        final String body = recorded("ledger-chain.json");
        respond(200, body);
        Assert.assertEquals(body, client().getLedgerEntry("50f6010d-6f44-44a9-85a8-830b93d13c17"));

        final String lists = recorded("lists.json");
        respond(200, lists);
        Assert.assertEquals(lists, client().getLists());
    }

    // Create only creates; replace with PUT.

    @Test
    public void savePolicyWithAnExistingNameIsA409WithItsReason() {

        respond(409, "{\"message\":\"A policy with this name already exists.\",\"reason\":\"policy_exists\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().savePolicy("court", "{\"identifiers\":{}}"));

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/policies", path);
        Assert.assertEquals(409, ex.getStatusCode());
        Assert.assertEquals("policy_exists", ex.getReason());
    }

    @Test
    public void replacePolicySendsAPutWithOnlyTheGivenFields() throws Exception {

        respond(200, "");

        client().replacePolicy("court", "{\"identifiers\":{}}");
        Assert.assertEquals("PUT", method);
        Assert.assertEquals("/api/policies/court", path);
        Assert.assertEquals("application/json", header("Content-Type"));
        Assert.assertEquals("{\"identifiers\":{}}", requestBodyAsString());
        // Left out, so Philter keeps the current description and notes.
        Assert.assertTrue(queryParameters.isEmpty());

        requests.clear();
        respond(200, POLICY_DETAILS);
        client().replacePolicy("court", "{\"identifiers\":{}}", "Federal court filings", null, OWNER);
        Assert.assertEquals(2, requests.size());
        Assert.assertEquals("PUT", requests.get(0).method);
        Assert.assertEquals("/api/policies/court", requests.get(0).path);
        Assert.assertEquals(Map.of("owner", OWNER), requests.get(0).queryParameters);
        Assert.assertEquals("{\"identifiers\":{}}", requests.get(0).body);
        Assert.assertEquals("PUT", requests.get(1).method);
        Assert.assertEquals("/api/policies/court/details", requests.get(1).path);
        Assert.assertEquals(Map.of("owner", OWNER), requests.get(1).queryParameters);
        Assert.assertEquals("{\"description\":\"Federal court filings\"}", requests.get(1).body);
    }

    @Test
    public void replacingAPolicyThatDoesNotExistIsA404() {

        respond(404, "{\"message\":\"Policy does not exist.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().replacePolicy("nope", "{\"identifiers\":{}}"));

        Assert.assertEquals(404, ex.getStatusCode());
        Assert.assertEquals("Policy does not exist.", ex.getErrorMessage());
    }

    @Test
    public void aConcurrentPolicyChangeIsA409WithItsReason() {

        respond(409, "{\"message\":\"Policy changed concurrently. Reload and retry.\",\"reason\":\"policy_changed\"}");

        Assert.assertEquals("policy_changed", Assert.assertThrows(ClientException.class,
                () -> client().replacePolicy("court", "{\"identifiers\":{}}")).getReason());
    }

    @Test
    public void saveListWithAnExistingNameIsA409WithItsReason() {

        respond(409, "{\"message\":\"A list with this name already exists.\",\"reason\":\"list_exists\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().saveList("customer-names", "Customer names", List.of("Jordan Example")));

        Assert.assertEquals("POST", method);
        Assert.assertEquals(409, ex.getStatusCode());
        Assert.assertEquals("list_exists", ex.getReason());
    }

    @Test
    public void replaceListSendsAPutAndPassesTheDescriptionThrough() throws Exception {

        respond(200, "{\"message\":\"List saved.\"}");

        client().replaceList("customer-names", null, List.of("Jordan Example", "Avery Example"));
        Assert.assertEquals("PUT", method);
        Assert.assertEquals("/api/lists/customer-names", path);
        Assert.assertEquals("[\"Jordan Example\",\"Avery Example\"]", requestBodyAsString());
        // A null description is left out, so Philter keeps the current one.
        Assert.assertFalse(queryParameters.containsKey("description"));

        client().replaceList("customer-names", "", List.of("Jordan Example"), OWNER);
        // An empty description is sent, which clears it.
        Assert.assertEquals("", queryParameter("description"));
        Assert.assertEquals(OWNER, queryParameter("owner"));
    }

    @Test
    public void replacingAListThatDoesNotExistIsA404() {

        respond(404, "{\"message\":\"List does not exist.\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().replaceList("nope", null, List.of("x")));
        Assert.assertEquals(404, ex.getStatusCode());
        Assert.assertEquals("List does not exist.", ex.getErrorMessage());
    }

    @Test
    public void deletePolicyReportsAMissingPolicyAndTheDefaultPolicy() {

        respond(404, "{\"message\":\"Policy does not exist.\"}");
        Assert.assertEquals(404, Assert.assertThrows(ClientException.class,
                () -> client().deletePolicy("nope")).getStatusCode());

        respond(409, "{\"message\":\"Cannot delete the default policy.\",\"reason\":\"policy_default\"}");
        final ClientException ex = Assert.assertThrows(ClientException.class, () -> client().deletePolicy("default"));
        Assert.assertEquals(409, ex.getStatusCode());
        Assert.assertEquals("policy_default", ex.getReason());
    }

    @Test
    public void createHoldConflictsCarryTheirReason() {

        respond(409, "{\"message\":\"A hold with reference 'case-1' already exists.\",\"reason\":\"hold_exists\"}");

        final LegalHoldRequest hold = new LegalHoldRequest();
        Assert.assertEquals("hold_exists", Assert.assertThrows(ClientException.class,
                () -> client().createHold(hold)).getReason());
    }

    @Test
    public void adminSettingsCarryTheReadOnlyDeploymentFlags() throws Exception {

        respond(200, "{\"signingEnabled\":true,\"crossUserAccessEnabled\":true,\"ledgerDeletionEnabled\":false,"
                + "\"signingKeyExternallyManaged\":true,\"warnings\":[]}");

        final AdminSettings settings = client().getAdminSettings();

        Assert.assertTrue(settings.isCrossUserAccessEnabled());
        Assert.assertFalse(settings.isLedgerDeletionEnabled());
        Assert.assertTrue(settings.isSigningKeyExternallyManaged());

        // Read-only, so an update cannot carry them.
        for (final String flag : List.of("crossUserAccessEnabled", "ledgerDeletionEnabled", "signingKeyExternallyManaged")) {
            Assert.assertThrows(flag, NoSuchFieldException.class, () -> UpdateAdminSettingsRequest.class.getDeclaredField(flag));
        }
    }

    @Test
    public void getCurrentUserCarriesTheMfaSettings() throws Exception {

        respond(200, "{\"username\":\"jordan\",\"role\":\"user\",\"active\":true,\"mfaEnabled\":false,"
                + "\"mfaAvailable\":true,\"mfaRequired\":true}");

        final CurrentUser me = client().getCurrentUser();

        Assert.assertEquals("/api/users/me", path);
        Assert.assertEquals("jordan", me.getUsername());
        Assert.assertTrue(me.isActive());
        Assert.assertTrue(me.isMfaAvailable());
        Assert.assertTrue(me.isMfaRequired());
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

    // ClientException status and error message.

    @Test
    public void clientExceptionCarriesTheStatusAndErrorMessage() {

        final int[] statuses = {400, 403, 404, 409};

        for (final int status : statuses) {

            final String body = "{\"message\":\"Refused with " + status + ".\"}";
            respond(status, body);

            final ClientException ex = Assert.assertThrows(ClientException.class, () -> client().getPolicy("p1"));

            Assert.assertEquals(status, ex.getStatusCode());
            Assert.assertEquals("Refused with " + status + ".", ex.getErrorMessage());
            // The message is unchanged, so anything that logs it sees the same text.
            Assert.assertEquals("Unknown error: HTTP " + status + ": " + body, ex.getMessage());
        }
    }

    @Test
    public void clientExceptionWithoutAJsonBodyHasTheStatusAndNoErrorMessage() {

        final int[] statuses = {400, 403, 404, 409};

        for (final int status : statuses) {

            respond(status, "");
            final ClientException empty = Assert.assertThrows(ClientException.class, () -> client().getPolicy("p1"));
            Assert.assertEquals(status, empty.getStatusCode());
            Assert.assertNull(empty.getErrorMessage());
            Assert.assertEquals("Unknown error: HTTP " + status, empty.getMessage());

            respond(status, "Bad request: the policy name is missing.");
            final ClientException text = Assert.assertThrows(ClientException.class, () -> client().getPolicy("p1"));
            Assert.assertEquals(status, text.getStatusCode());
            Assert.assertNull("a plain-text body has no message field", text.getErrorMessage());
            Assert.assertEquals("Unknown error: HTTP " + status + ": Bad request: the policy name is missing.", text.getMessage());
        }
    }

    @Test
    public void theErrorMessageIsReadFromTheWholeBodyNotTheTruncatedMessage() {

        final String longMessage = "x".repeat(600);
        respond(400, "{\"message\":\"" + longMessage + "\"}");

        final ClientException ex = Assert.assertThrows(ClientException.class, () -> client().getPolicy("p1"));

        Assert.assertTrue("the exception message is still truncated", ex.getMessage().endsWith("..."));
        Assert.assertEquals(longMessage, ex.getErrorMessage());
    }

    @Test
    public void createContextConflictsCarryTheirReason() {

        final String[][] cases = {
                {"context_exists", "Context already exists."},
                {"context_limit_reached", "Maximum number of contexts reached."}};

        for (final String[] c : cases) {

            respond(409, "{\"message\":\"" + c[1] + "\",\"reason\":\"" + c[0] + "\"}");

            final ClientException ex = Assert.assertThrows(ClientException.class,
                    () -> client().createContext("tenant-a", false, false));

            Assert.assertEquals(409, ex.getStatusCode());
            Assert.assertEquals(c[0], ex.getReason());
            Assert.assertEquals(c[1], ex.getErrorMessage());
        }
    }

    @Test
    public void aBodyWithoutAReasonHasANullReason() {

        respond(409, "{\"message\":\"Policy changed concurrently. Reload and retry.\"}");
        final ClientException noReason = Assert.assertThrows(ClientException.class, () -> client().getPolicy("p1"));
        Assert.assertNull(noReason.getReason());
        Assert.assertEquals("Policy changed concurrently. Reload and retry.", noReason.getErrorMessage());

        respond(409, "Conflict.");
        Assert.assertNull("a non-JSON body has no reason",
                Assert.assertThrows(ClientException.class, () -> client().getPolicy("p1")).getReason());

        respond(409, "");
        Assert.assertNull("an empty body has no reason",
                Assert.assertThrows(ClientException.class, () -> client().getPolicy("p1")).getReason());

        respond(409, "{\"message\":\"m\",\"reason\":{\"code\":1}}");
        Assert.assertNull("a reason that is not a string is ignored",
                Assert.assertThrows(ClientException.class, () -> client().getPolicy("p1")).getReason());
    }

    @Test
    public void signInRefusalsExposeTheirReasonOnClientException() {

        responseHeaders.put("Retry-After", "900");
        respond(429, "{\"message\":\"Too many failed sign-ins for this username. Try again later.\",\"reason\":\"locked\"}");

        // Caught as the base type: the reason is available without knowing the subclass.
        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().signIn("jordan", "the-users-password"));

        Assert.assertEquals("locked", ex.getReason());
        Assert.assertEquals(429, ex.getStatusCode());
    }

    @Test
    public void signInThrottledExceptionsCarryStatus429() {

        responseHeaders.put("Retry-After", "60");
        respond(429, "{\"message\":\"Too many sign-in requests. Try again later.\",\"reason\":\"rate_limited\"}");

        final SignInRateLimitedException limited = Assert.assertThrows(SignInRateLimitedException.class,
                () -> client().signIn("jordan", "the-users-password"));
        Assert.assertEquals(429, limited.getStatusCode());
        Assert.assertEquals("Too many sign-in requests. Try again later.", limited.getErrorMessage());
        Assert.assertEquals("Too many sign-in requests. Try again later.", limited.getMessage());

        responseHeaders.put("Retry-After", "900");
        respond(429, "{\"message\":\"Too many failed sign-ins for this username. Try again later.\",\"reason\":\"locked\"}");

        final SignInLockedException locked = Assert.assertThrows(SignInLockedException.class,
                () -> client().signIn("jordan", "the-users-password"));
        Assert.assertEquals(429, locked.getStatusCode());
    }

    @Test
    public void aClientExceptionNotCausedByAnHttpStatusHasStatusZero() {

        // A 200 that lacks the headers the export needs is refused by the client, not by Philter.
        respond(200, "timestamp,event\n");

        final ClientException ex = Assert.assertThrows(ClientException.class,
                () -> client().exportAuditLog(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)));

        Assert.assertEquals(0, ex.getStatusCode());
        Assert.assertNull(ex.getErrorMessage());
    }

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
