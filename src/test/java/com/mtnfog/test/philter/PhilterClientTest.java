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
import ai.philterd.philter.model.BinaryFilterResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Ignore
public class PhilterClientTest {

    private static final Logger LOGGER = LogManager.getLogger(PhilterClientTest.class);

    private static final String ENDPOINT = "https://10.0.2.227:8080/";

    @Test
    public void filterPdf() throws Exception {

        final PhilterClient client = new PhilterClient.PhilterClientBuilder()
                .withEndpoint(ENDPOINT)
                .withHttpClientBuilder(getUnsafeHttpClientBuilder())
                .withTimeout(300)
                .build();

        final File file = new File("pdf-file-name-here");
        final BinaryFilterResponse binaryFilterResponse = client.filter("context", "default", file);

        final File tempFile = File.createTempFile("philter", ".zip");
        Files.write(tempFile.toPath(), binaryFilterResponse.getContent());
        System.out.println("Response written to " + tempFile.getAbsolutePath());

    }

    @Test
    public void getPolicies() throws Exception {

        final PhilterClient client = new PhilterClient.PhilterClientBuilder()
                .withEndpoint(ENDPOINT)
                .withHttpClientBuilder(getUnsafeHttpClientBuilder())
                .build();

        final List<String> policyNames = client.getPolicies();

        Assert.assertNotNull(policyNames);
        Assert.assertFalse(policyNames.isEmpty());

        for(final String name : policyNames) {
            LOGGER.info("Policy: {}",  name);
        }

    }

    @Test(expected = SSLHandshakeException.class)
    public void getPoliciesNoCertificate() throws Exception {

        final PhilterClient client = new PhilterClient.PhilterClientBuilder()
                .withEndpoint(ENDPOINT)
                .build();

        client.getPolicies();

    }

    @Test
    public void get() throws Exception {

        final PhilterClient client = new PhilterClient.PhilterClientBuilder()
                .withEndpoint(ENDPOINT)
                .withSslConfiguration("/tmp/client-test.jks", "changeit",
                        "/tmp/keystore-server.jks", "changeit")
                .build();

        final List<String> policyNames = client.getPolicies();

        Assert.assertNotNull(policyNames);
        Assert.assertFalse(policyNames.isEmpty());

        for(final String name : policyNames) {
            LOGGER.info("Policy: {}",  name);
        }

    }

    @Test
    public void getByName() throws Exception {

        final PhilterClient client = new PhilterClient.PhilterClientBuilder()
                .withEndpoint(ENDPOINT)
                .withSslConfiguration("/tmp/client-test.jks", "changeit",
                        "/tmp/keystore-server.jks", "changeit")
                .build();

        final String filterProfile = client.Policy("default");

        Assert.assertNotNull(filterProfile);
        Assert.assertFalse(filterProfile.isEmpty());

        LOGGER.info("Policy:\n{}", filterProfile);

    }

    @Test
    public void save() throws Exception {

        final PhilterClient client = new PhilterClient.PhilterClientBuilder()
                .withEndpoint(ENDPOINT)
                .withSslConfiguration("/tmp/client-test.jks", "changeit",
                        "/tmp/keystore-server.jks", "changeit")
                .build();

        final byte[] bytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(this.getClass().getResource("/default2.json")).toURI()));
        final String json = new String(bytes, Charset.defaultCharset());

        client.savePolicy("default", json);

    }

    @Test
    public void status() throws Exception {

        final PhilterClient client = new PhilterClient.PhilterClientBuilder()
                .withEndpoint(ENDPOINT)
                .withSslConfiguration("/tmp/client-test.jks", "changeit",
                        "/tmp/keystore-server.jks", "changeit")
                .withHttpClientBuilder(getUnsafeHttpClientBuilder())
                .build();

        final String status = client.status();

        Assert.assertTrue(status.startsWith("Healhty:"));

    }

    // This is used to test against Philter running with a self-signed certificate.
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

        // The JDK client checks the hostname inside its TLS engine, so a trust-all manager alone is
        // not enough for a certificate whose name does not match; this also disables that check.
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(PhilterClient.DEFAULT_TIMEOUT_SEC));

    }

}
