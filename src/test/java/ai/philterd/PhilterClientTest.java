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
import ai.philterd.philter.model.StatusResponse;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
 * </ul>
 */
public class PhilterClientTest {

    private static final Logger LOGGER = LogManager.getLogger(PhilterClientTest.class);

    private static final String ENDPOINT = System.getenv("PHILTER_ENDPOINT");
    private static final String API_KEY = System.getenv("PHILTER_API_KEY");
    private static final boolean INSECURE = Boolean.parseBoolean(getEnv("PHILTER_INSECURE", "false"));

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
            builder.withOkHttpClientBuilder(getUnsafeOkHttpClientBuilder());
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
    public void status() throws Exception {
        final StatusResponse status = client().status();
        Assert.assertNotNull(status);
        Assert.assertNotNull(status.getStatus());
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

    private static String getEnv(final String name, final String defaultValue) {
        final String value = System.getenv(name);
        return value == null ? defaultValue : value;
    }

    // Trusts all certificates; used to test against Philter running with a self-signed certificate.
    private OkHttpClient.Builder getUnsafeOkHttpClientBuilder() throws NoSuchAlgorithmException, KeyManagementException {

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

        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
        builder.connectTimeout(PhilterClient.DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS);
        builder.writeTimeout(PhilterClient.DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS);
        builder.readTimeout(PhilterClient.DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS);
        builder.connectionPool(new ConnectionPool(PhilterClient.DEFAULT_MAX_IDLE_CONNECTIONS, PhilterClient.DEFAULT_KEEP_ALIVE_DURATION_MS, TimeUnit.MILLISECONDS));
        builder.hostnameVerifier((hostname, session) -> true);

        return builder;

    }

}
