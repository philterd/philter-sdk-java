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
import ai.philterd.philter.model.CreatedApiKeyResponse;
import ai.philterd.philter.model.CreatedUserResponse;
import ai.philterd.philter.model.ExplainResponse;
import ai.philterd.philter.model.FilterResponse;
import ai.philterd.philter.model.GenericResponse;
import ai.philterd.philter.model.GetListsResponse;
import ai.philterd.philter.model.StatusResponse;
import ai.philterd.philter.model.exceptions.ClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Live integration tests that run against a real Philter 4.0.0 instance.
 *
 * <p>These tests are skipped (not failed) unless {@code PHILTER_ENDPOINT} is set. Configure them with
 * the following environment variables:</p>
 *
 * <ul>
 *   <li>{@code PHILTER_ENDPOINT} (required) - e.g. {@code https://localhost:8080/}</li>
 *   <li>{@code PHILTER_API_KEY} (optional) - value sent in the Authorization header</li>
 *   <li>{@code PHILTER_INSECURE} (optional) - {@code true} to trust self-signed certificates</li>
 *   <li>{@code PHILTER_PDF_FILE} (optional) - path to a PDF used by the PDF filtering test</li>
 *   <li>{@code PHILTER_PROVISIONING} (optional) - {@code true} when the instance runs with
 *       {@code PROVISIONING_API_ENABLED=true} and {@code PHILTER_API_KEY} is an administrator's key, to run
 *       the provisioning tests. They create a user, which nothing in the API removes, so they are opt-in.</li>
 * </ul>
 */
public class PhilterClientTest {

    private static final Logger LOGGER = LogManager.getLogger(PhilterClientTest.class);

    private static final String ENDPOINT = System.getenv("PHILTER_ENDPOINT");
    private static final String API_KEY = System.getenv("PHILTER_API_KEY");
    private static final boolean INSECURE = Boolean.parseBoolean(getEnv("PHILTER_INSECURE", "false"));
    private static final boolean PROVISIONING = Boolean.parseBoolean(getEnv("PHILTER_PROVISIONING", "false"));

    static {
        if (INSECURE) {
            // The JDK HTTP client has no hostname verifier hook. This system property is the
            // supported way to accept a certificate whose name does not match the host, and it
            // must be set before the client is first created.
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
        }
    }

    private static final String SENSITIVE_TEXT = "His SSN is 123-45-6789.";
    private static final String SSN = "123-45-6789";

    @Before
    public void requireServer() {
        Assume.assumeTrue("Set PHILTER_ENDPOINT to run integration tests against a live Philter instance.",
                ENDPOINT != null && !ENDPOINT.isBlank());
    }

    private PhilterClient client() throws Exception {

        final PhilterClient.PhilterClientBuilder builder = new PhilterClient.PhilterClientBuilder()
                .withEndpoint(ENDPOINT)
                .withTimeout(300);

        if (API_KEY != null && !API_KEY.isBlank()) {
            builder.withApiKey(API_KEY);
        }

        if (INSECURE) {
            builder.withHttpClientBuilder(getUnsafeHttpClientBuilder());
        }

        return builder.build();
    }

    // Status.

    @Test
    public void health() throws Exception {
        final StatusResponse status = client().health();
        Assert.assertNotNull(status);
        Assert.assertNotNull(status.getStatus());
        LOGGER.info("Health: {} (version {})", status.getStatus(), status.getApplicationVersion());
    }

    @Test
    public void signingKey() throws Exception {
        // The signing key endpoint is unauthenticated, like health.
        Assert.assertNotNull(client().getSigningKey());
    }

    // Filtering and explanation.

    @Test
    public void filterText() throws Exception {

        final FilterResponse response = client().filter("sdk-it", "default", SENSITIVE_TEXT);

        Assert.assertNotNull(response.getFilteredText());
        Assert.assertEquals("sdk-it", response.getContext());
        // The default policy should redact the SSN, so the raw value must not survive.
        Assert.assertFalse("The SSN should have been filtered from the text.",
                response.getFilteredText().contains(SSN));
        // Verifies that Philter returns the assigned document ID via the x-document-id header.
        Assert.assertNotNull("Philter should return a document ID.", response.getDocumentId());

        LOGGER.info("Filtered text: {} (document {})", response.getFilteredText(), response.getDocumentId());
    }

    @Test
    public void explain() throws Exception {

        final ExplainResponse response = client().explain("sdk-it", "default", SENSITIVE_TEXT);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getFilteredText());
        // Verifies that explain returns a structured explanation object rather than a bare string.
        Assert.assertNotNull("Explain should return a structured explanation.", response.getExplanation());
        Assert.assertNotNull(response.getExplanation().getAppliedSpans());
    }

    @Test
    public void filterPdf() throws Exception {

        final String pdfPath = System.getenv("PHILTER_PDF_FILE");
        Assume.assumeTrue("Set PHILTER_PDF_FILE to run the PDF filtering test.",
                pdfPath != null && !pdfPath.isBlank());

        final BinaryFilterResponse response = client().filter("sdk-it", "default", "test.pdf", new File(pdfPath));

        Assert.assertNotNull(response.getContent());
        Assert.assertTrue(response.getContent().length > 0);
    }

    // Policies.

    @Test
    public void getPolicies() throws Exception {
        final List<String> policies = client().getPolicies();
        Assert.assertNotNull(policies);
        LOGGER.info("Policies: {}", policies);
    }

    @Test
    public void policyRoundTrip() throws Exception {

        final PhilterClient client = client();

        // Round-trip the server's own policy JSON so we do not depend on a hand-written schema.
        final String defaultPolicy = client.getPolicy("default");
        Assume.assumeTrue("Requires a 'default' policy on the server.",
                defaultPolicy != null && !defaultPolicy.isBlank());

        final String name = "sdk-it-policy-" + System.currentTimeMillis();

        try {
            client.savePolicy(name, defaultPolicy);

            final String fetched = client.getPolicy(name);
            Assert.assertNotNull(fetched);
            Assert.assertTrue("Saved policy should appear in the policy list.",
                    client.getPolicies().contains(name));
        } finally {
            client.deletePolicy(name);
        }
    }

    // Contexts.

    @Test
    public void contextLifecycle() throws Exception {

        final PhilterClient client = client();
        final String name = "sdk-it-ctx-" + System.currentTimeMillis();

        try {
            final GenericResponse created = client.createContext(name, false, false);
            Assert.assertNotNull(created);
            Assert.assertNotNull(client.getContexts());
        } finally {
            client.deleteContext(name);
        }
    }

    // Custom lists.

    @Test
    public void listLifecycle() throws Exception {

        final PhilterClient client = client();
        final String name = "sdk-it-list-" + System.currentTimeMillis();

        try {
            client.saveList(name, "integration test list", List.of("alpha", "beta"));

            final GetListsResponse list = client.getList(name);
            Assert.assertNotNull(list);
            Assert.assertNotNull(list.getLists());
            Assert.assertTrue("Saved list values should be returned.", list.getLists().contains("alpha"));
        } finally {
            client.deleteList(name);
        }
    }

    // Provisioning.

    @Test
    public void provisionUserAndApiKey() throws Exception {

        Assume.assumeTrue("Set PHILTER_PROVISIONING=true against an instance with PROVISIONING_API_ENABLED=true.",
                PROVISIONING);

        final PhilterClient client = client();
        final String username = "sdk-it-user-" + System.currentTimeMillis();

        final CreatedUserResponse user = client.createUser(username, username + "@example.com",
                "sdk-it-password-that-is-long-enough");

        Assert.assertEquals(username, user.getUsername());
        Assert.assertEquals("Provisioning only ever creates a non-administrator.", "user", user.getRole());

        final CreatedApiKeyResponse key = client.createApiKey(username, List.of("redact"));

        Assert.assertEquals(username, key.getUsername());
        Assert.assertNotNull("The key value is returned only here.", key.getApiKey());
        Assert.assertFalse(key.getApiKey().isBlank());
        Assert.assertEquals(List.of("redact"), key.getScopes());

        LOGGER.info("Provisioned user {} with a {} key.", username, key.getScopes());

        // The minted key works, and carries only the scope it was given. Philter expects the Bearer
        // scheme, and the client sends the Authorization value verbatim, so the prefix goes on here.
        final PhilterClient scoped = new PhilterClient.PhilterClientBuilder()
                .withEndpoint(ENDPOINT)
                .withApiKey("Bearer " + key.getApiKey())
                .withHttpClientBuilder(INSECURE ? getUnsafeHttpClientBuilder() : HttpClient.newBuilder())
                .build();

        // The new user comes with a "default" policy and context, so the key can redact straight away.
        Assert.assertNotNull(scoped.filter("default", "default", SENSITIVE_TEXT).getFilteredText());
        Assert.assertThrows("A redact-only key cannot read policies.", ClientException.class, scoped::getPolicies);
    }

    private static String getEnv(final String name, final String defaultValue) {
        final String value = System.getenv(name);
        return value == null ? defaultValue : value;
    }

    // Trusts all certificates; used to test against Philter running with a self-signed certificate.
    private HttpClient.Builder getUnsafeHttpClientBuilder() throws NoSuchAlgorithmException, KeyManagementException {

        final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

        } };

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(java.time.Duration.ofSeconds(PhilterClient.DEFAULT_TIMEOUT_SEC));

    }

}
