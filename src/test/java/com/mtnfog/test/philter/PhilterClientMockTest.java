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
package com.mtnfog.test.philter;

import ai.philterd.philter.PhilterClient;
import ai.philterd.philter.model.Alert;
import ai.philterd.philter.model.BinaryFilterResponse;
import ai.philterd.philter.model.ExplainResponse;
import ai.philterd.philter.model.FilterResponse;
import ai.philterd.philter.model.exceptions.ClientException;
import ai.philterd.philter.model.exceptions.ServiceUnavailableException;
import ai.philterd.philter.model.exceptions.UnauthorizedException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exercises {@link PhilterClient} against a local {@link HttpServer}, asserting both the request
 * that goes onto the wire and the response the client hands back.
 */
public class PhilterClientMockTest {

    private HttpServer server;
    private PhilterClient client;

    /** The request the server last received. */
    private String method;
    private String path;
    private String rawPath;
    private String query;
    private byte[] requestBody;
    private String contentType;
    private String accept;

    /** The response the server will next return. */
    private int status = 200;
    private byte[] responseBody = new byte[0];
    private String documentIdHeader;

    @Before
    public void setUp() throws Exception {

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        server.createContext("/", (HttpExchange exchange) -> {

            method = exchange.getRequestMethod();
            path = exchange.getRequestURI().getPath();
            rawPath = exchange.getRequestURI().getRawPath();
            query = exchange.getRequestURI().getRawQuery();
            requestBody = readAll(exchange);
            contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            accept = exchange.getRequestHeaders().getFirst("Accept");

            if (documentIdHeader != null) {
                exchange.getResponseHeaders().add("x-document-id", documentIdHeader);
            }

            // A 204 must not carry a body; anything else sends the queued bytes.
            if (responseBody.length == 0) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                exchange.sendResponseHeaders(status, responseBody.length);
                exchange.getResponseBody().write(responseBody);
            }

            exchange.close();

        });

        server.start();

        client = new PhilterClient.PhilterClientBuilder()
                // Deliberately no trailing slash - Retrofit rejected this, the JDK client does not.
                .withEndpoint("http://localhost:" + server.getAddress().getPort())
                .build();

    }

    @After
    public void tearDown() {
        server.stop(0);
    }

    private static byte[] readAll(final HttpExchange exchange) throws IOException {
        return exchange.getRequestBody().readAllBytes();
    }

    private void respond(final int status, final String body) {
        this.status = status;
        this.responseBody = body.getBytes(StandardCharsets.UTF_8);
    }

    private String requestBodyAsString() {
        return new String(requestBody, StandardCharsets.UTF_8);
    }

    // Filtering.

    @Test
    public void filterText() throws Exception {

        respond(200, "My name is {{{REDACTED-person}}}.");
        documentIdHeader = "doc-1";

        final FilterResponse response = client.filter("ctx", "ignored-doc-id", "default", "My name is John Smith.");

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/filter", path);
        Assert.assertEquals("c=ctx&d=ignored-doc-id&p=default", query);
        Assert.assertEquals("text/plain", contentType);
        Assert.assertEquals("text/plain", accept);
        Assert.assertEquals("My name is John Smith.", requestBodyAsString());

        Assert.assertEquals("My name is {{{REDACTED-person}}}.", response.getFilteredText());
        Assert.assertEquals("ctx", response.getContext());
        // The document ID always comes from the response header, not the argument.
        Assert.assertEquals("doc-1", response.getDocumentId());

    }

    @Test
    public void filterTextOmitsNullQueryParameters() throws Exception {

        respond(200, "filtered");

        client.filter(null, null, "default", "text");

        Assert.assertEquals("p=default", query);

    }

    @Test
    public void filterTextEncodesQueryParameters() throws Exception {

        respond(200, "filtered");

        client.filter("a context/with spaces&more", null, "default", "text");

        Assert.assertEquals("c=a%20context%2Fwith%20spaces%26more&p=default", query);

    }

    @Test
    public void filterTextSendsUtf8Body() throws Exception {

        respond(200, "filtered");

        client.filter("ctx", null, "default", "Zoë Ångström 日本語");

        Assert.assertEquals("Zoë Ångström 日本語", requestBodyAsString());

    }

    @Test
    public void filterPdf() throws Exception {

        final byte[] zip = new byte[]{0x50, 0x4b, 0x03, 0x04, 0x00, 0x01};
        this.status = 200;
        this.responseBody = zip;
        documentIdHeader = "doc-2";

        final Path pdf = Files.createTempFile("philter", ".pdf");
        final byte[] pdfBytes = "%PDF-1.4 fake".getBytes(StandardCharsets.UTF_8);
        Files.write(pdf, pdfBytes);

        final BinaryFilterResponse response = client.filter("ctx", null, "default", new File(pdf.toString()));

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/filter", path);
        Assert.assertEquals("application/pdf", contentType);
        Assert.assertEquals("application/zip", accept);
        Assert.assertArrayEquals(pdfBytes, requestBody);

        Assert.assertArrayEquals(zip, response.getContent());
        Assert.assertEquals("ctx", response.getContext());
        Assert.assertEquals("doc-2", response.getDocumentId());

        Files.deleteIfExists(pdf);

    }

    @Test
    public void explain() throws Exception {

        respond(200, "{\"filteredText\":\"redacted\",\"context\":\"ctx\",\"documentId\":\"doc-3\","
                + "\"explanation\":{\"appliedSpans\":[{\"id\":\"s1\",\"characterStart\":11,\"characterEnd\":21,"
                + "\"filterType\":\"person\",\"context\":\"ctx\"}],\"ignoredSpans\":[]}}");

        final ExplainResponse response = client.explain("ctx", null, "default", "My name is John Smith.");

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/explain", path);
        Assert.assertEquals("application/json", accept);
        Assert.assertEquals("text/plain", contentType);

        Assert.assertEquals("redacted", response.getFilteredText());
        Assert.assertEquals("doc-3", response.getDocumentId());
        Assert.assertNotNull(response.getExplanation());
        Assert.assertEquals(1, response.getExplanation().getAppliedSpans().size());
        Assert.assertEquals("person", response.getExplanation().getAppliedSpans().get(0).getFilterType());
        Assert.assertEquals(11, response.getExplanation().getAppliedSpans().get(0).getCharacterStart());
        Assert.assertTrue(response.getExplanation().getIgnoredSpans().isEmpty());

    }

    // Status.

    @Test
    public void status() throws Exception {

        respond(200, "Healthy");

        Assert.assertEquals("Healthy", client.status());
        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/status", path);
        Assert.assertNull(query);

    }

    // Policies.

    @Test
    public void getPolicies() throws Exception {

        respond(200, "[\"default\",\"strict\"]");

        final List<String> policies = client.getPolicies();

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/policies", path);
        Assert.assertEquals("application/json", accept);
        Assert.assertEquals(List.of("default", "strict"), policies);

    }

    @Test
    public void getPolicyByName() throws Exception {

        respond(200, "{\"name\":\"default\"}");

        Assert.assertEquals("{\"name\":\"default\"}", client.Policy("default"));
        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/policies/default", path);
        Assert.assertEquals("text/plain", accept);

    }

    @Test
    public void getPolicyEncodesNameInPath() throws Exception {

        respond(200, "{}");

        client.Policy("my policy/v2");

        // The name is one path segment, so the space and the slash must both be percent-encoded
        // rather than opening a new segment.
        Assert.assertEquals("/api/policies/my%20policy%2Fv2", rawPath);
        Assert.assertEquals("/api/policies/my policy/v2", path);

    }

    @Test
    public void savePolicy() throws Exception {

        this.status = 200;
        this.responseBody = new byte[0];

        client.savePolicy("{\"name\":\"default\"}");

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/api/policies", path);
        Assert.assertEquals("application/json", contentType);
        Assert.assertEquals("{\"name\":\"default\"}", requestBodyAsString());

    }

    @Test
    public void deletePolicy() throws Exception {

        this.status = 200;
        this.responseBody = new byte[0];

        client.deletePolicy("default");

        Assert.assertEquals("DELETE", method);
        Assert.assertEquals("/api/policies/default", path);

    }

    // Alerts.

    @Test
    public void getAlerts() throws Exception {

        respond(200, "[{\"id\":\"a1\",\"context\":\"ctx\",\"documentId\":\"d1\",\"filterType\":\"person\"}]");

        final List<Alert> alerts = client.getAlerts();

        Assert.assertEquals("GET", method);
        Assert.assertEquals("/api/alerts", path);
        Assert.assertEquals(1, alerts.size());
        Assert.assertEquals("a1", alerts.get(0).getId());
        Assert.assertEquals("person", alerts.get(0).getFilterType());

    }

    @Test
    public void deleteAlert() throws Exception {

        this.status = 200;
        this.responseBody = new byte[0];

        client.deleteAlert("a1");

        Assert.assertEquals("DELETE", method);
        Assert.assertEquals("/api/alerts/a1", path);

    }

    // Proxy support.

    @Test
    public void honoursSystemProxyProperties() throws Exception {

        respond(200, "Healthy");

        // Point the proxy at the test server and aim the client at a host that does not resolve,
        // so the call can only succeed if the proxy was actually consulted. OkHttp did this by
        // default; the JDK client only does it when a ProxySelector is set explicitly.
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", Integer.toString(server.getAddress().getPort()));

        try {

            final PhilterClient proxied = new PhilterClient.PhilterClientBuilder()
                    .withEndpoint("http://philter.example.invalid:8080")
                    .withTimeout(5)
                    .build();

            Assert.assertEquals("Healthy", proxied.status());
            Assert.assertEquals("/api/status", path);

        } finally {

            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");

        }

    }

    // Error mapping.

    @Test(expected = UnauthorizedException.class)
    public void unauthorizedMapsTo401() throws Exception {
        respond(401, "nope");
        client.getPolicies();
    }

    @Test(expected = ServiceUnavailableException.class)
    public void serviceUnavailableMapsTo503() throws Exception {
        respond(503, "nope");
        client.getPolicies();
    }

    @Test(expected = ClientException.class)
    public void otherErrorMapsToClientException() throws Exception {
        respond(500, "boom");
        client.getPolicies();
    }

    @Test
    public void statusReportsUnauthorizedAsClientException() throws Exception {

        respond(401, "nope");

        // Documents a pre-existing quirk: status() does not raise UnauthorizedException on a 401.
        try {
            client.status();
            Assert.fail("expected a ClientException");
        } catch (final ClientException ex) {
            Assert.assertEquals("Unknown error: HTTP 401", ex.getMessage());
        }

    }

}
