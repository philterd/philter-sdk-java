package com.mtnfog.test.philter;

import com.mtnfog.philter.FilterProfileRegistryClient;
import com.mtnfog.philter.PhilterClient;
import com.mtnfog.philter.model.StatusResponse;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Ignore
public class FilterProfileRegistryClientTest {

    private static final Logger LOGGER = LogManager.getLogger(FilterProfileRegistryClientTest.class);

    private static final String ENDPOINT = "https://localhost:8080/";
    private static final String TOKEN = "";

    @Test
    public void get() throws Exception {

        final FilterProfileRegistryClient client = new FilterProfileRegistryClient.FilterProfileRegistryClientBuilder()
                .withEndpoint(ENDPOINT)
                .withToken(TOKEN)
                .withOkHttpClientBuilder(getUnsafeOkHttpClientBuilder())
                .build();

        final List<String> filterProfileNames = client.get();

        Assert.assertTrue(filterProfileNames != null);
        Assert.assertFalse(filterProfileNames.isEmpty());

        for(final String name : filterProfileNames) {
            LOGGER.info("Filter profile: {}",  name);
        }

    }

    @Test
    public void getByName() throws Exception {

        final FilterProfileRegistryClient client = new FilterProfileRegistryClient.FilterProfileRegistryClientBuilder()
                .withEndpoint(ENDPOINT)
                .withToken(TOKEN)
                .withOkHttpClientBuilder(getUnsafeOkHttpClientBuilder())
                .build();

        final String filterProfile = client.get("default");

        Assert.assertTrue(filterProfile != null);
        Assert.assertTrue(filterProfile.length() > 0);

        LOGGER.info("Filter profile:\n{}", filterProfile);

    }

    @Test
    public void save() throws Exception {

        final FilterProfileRegistryClient client = new FilterProfileRegistryClient.FilterProfileRegistryClientBuilder()
                .withEndpoint(ENDPOINT)
                .withToken(TOKEN)
                .withOkHttpClientBuilder(getUnsafeOkHttpClientBuilder())
                .build();

        final String json = IOUtils.toString(this.getClass().getResource("/default2.json"), Charset.defaultCharset());

        client.save(json);

    }

    @Test
    public void status() throws Exception {

        final FilterProfileRegistryClient client = new FilterProfileRegistryClient.FilterProfileRegistryClientBuilder()
                .withEndpoint(ENDPOINT)
                .withToken(TOKEN)
                .withOkHttpClientBuilder(getUnsafeOkHttpClientBuilder())
                .build();

        final StatusResponse statusResponse = client.status();

        Assert.assertTrue(StringUtils.equals("Healthy", statusResponse.getStatus()));

    }

    // This is used to test against Philter running with a self-signed certificate.
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
        builder.connectTimeout(PhilterClient.TIMEOUT_SEC, TimeUnit.SECONDS);
        builder.writeTimeout(PhilterClient.TIMEOUT_SEC, TimeUnit.SECONDS);
        builder.readTimeout(PhilterClient.TIMEOUT_SEC, TimeUnit.SECONDS);
        builder.connectionPool(new ConnectionPool(PhilterClient.MAX_IDLE_CONNECTIONS, PhilterClient.KEEP_ALIVE_DURATION_MS, TimeUnit.MILLISECONDS));
        builder.hostnameVerifier((hostname, session) -> true);

        return builder;

    }

}
