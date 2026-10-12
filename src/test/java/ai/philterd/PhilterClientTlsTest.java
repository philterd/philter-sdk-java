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
import ai.philterd.philter.PhilterTls;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Exercises the client's TLS options against a local HTTPS server, some of whose tests require a client
 * certificate. The keystores are generated with the JDK's {@code keytool} for each run, so no private key is
 * kept in the repository.
 */
public class PhilterClientTlsTest {

    private static final String PASSWORD = "sdk-test-store-password";

    @ClassRule
    public static final TemporaryFolder FOLDER = new TemporaryFolder();

    /** The server's key, for {@code localhost}. */
    private static Path serverKeyStore;
    /** A server key for another host, to show that hostname verification stays on. */
    private static Path otherHostKeyStore;
    /** The client certificate the server trusts, as PKCS12 and JKS. */
    private static Path clientKeyStore;
    private static Path clientJks;
    /** A client certificate the server does not trust. */
    private static Path strangerKeyStore;
    /** What the client trusts: both server certificates. */
    private static Path clientTrustStore;
    /** What the server trusts: the client certificate. */
    private static Path serverTrustStore;

    private HttpsServer server;

    @BeforeClass
    public static void generateKeyStores() throws Exception {

        final Path dir = FOLDER.getRoot().toPath();

        serverKeyStore = keyPair(dir, "server.p12", "CN=localhost", "SAN=dns:localhost,ip:127.0.0.1,ip:::1");
        otherHostKeyStore = keyPair(dir, "other-host.p12", "CN=philter.invalid", "SAN=dns:philter.invalid");
        clientKeyStore = keyPair(dir, "client.p12", "CN=philter-sdk-test-client", null);
        strangerKeyStore = keyPair(dir, "stranger.p12", "CN=stranger", null);

        clientTrustStore = dir.resolve("client-trust.p12");
        trust(clientTrustStore, "server", exportCertificate(serverKeyStore, dir.resolve("server.cer")));
        trust(clientTrustStore, "other-host", exportCertificate(otherHostKeyStore, dir.resolve("other-host.cer")));

        serverTrustStore = dir.resolve("server-trust.p12");
        trust(serverTrustStore, "client", exportCertificate(clientKeyStore, dir.resolve("client.cer")));

        clientJks = dir.resolve("client.jks");
        keytool("-importkeystore", "-srckeystore", clientKeyStore.toString(), "-srcstoretype", "PKCS12",
                "-srcstorepass", PASSWORD, "-destkeystore", clientJks.toString(), "-deststoretype", "JKS",
                "-deststorepass", PASSWORD, "-destkeypass", PASSWORD, "-noprompt");

    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    // Client certificates.

    @Test
    public void aClientCertificateReachesAServerThatRequiresOne() throws Exception {

        startServer(serverKeyStore, true);

        final PhilterClient client = builder()
                .withClientCertificate(clientKeyStore, PASSWORD.toCharArray())
                .withTrustStore(clientTrustStore, PASSWORD.toCharArray())
                .build();

        Assert.assertEquals("UP", client.health().getStatus());
    }

    @Test
    public void aJksClientCertificateLoadsToo() throws Exception {

        startServer(serverKeyStore, true);

        final PhilterClient client = builder()
                .withClientCertificate(clientJks, PASSWORD.toCharArray())
                .withTrustStore(clientTrustStore, PASSWORD.toCharArray())
                .build();

        Assert.assertEquals("UP", client.health().getStatus());
    }

    @Test
    public void withoutAClientCertificateTheServerRefuses() {

        startServer(serverKeyStore, true);

        final PhilterClient client = builder()
                .withTrustStore(clientTrustStore, PASSWORD.toCharArray())
                .build();

        Assert.assertThrows(IOException.class, client::health);
    }

    @Test
    public void aClientCertificateTheServerDoesNotTrustIsRefused() {

        startServer(serverKeyStore, true);

        final PhilterClient client = builder()
                .withClientCertificate(strangerKeyStore, PASSWORD.toCharArray())
                .withTrustStore(clientTrustStore, PASSWORD.toCharArray())
                .build();

        Assert.assertThrows(IOException.class, client::health);
    }

    @Test
    public void thePasswordIsCopied() throws Exception {

        startServer(serverKeyStore, true);

        final char[] password = PASSWORD.toCharArray();
        final PhilterClient.PhilterClientBuilder builder = builder()
                .withClientCertificate(clientKeyStore, password)
                .withTrustStore(clientTrustStore, password);
        Arrays.fill(password, '\0');

        Assert.assertEquals("UP", builder.build().health().getStatus());
    }

    // Trust stores.

    @Test
    public void aTrustStoreAloneReachesAServerWithAPrivateCertificate() throws Exception {

        startServer(serverKeyStore, false);

        final PhilterClient client = builder()
                .withTrustStore(clientTrustStore, PASSWORD.toCharArray())
                .build();

        Assert.assertEquals("UP", client.health().getStatus());
    }

    @Test
    public void withoutATrustStoreAPrivateCertificateIsRefused() {

        startServer(serverKeyStore, false);

        // The JVM's default trust store does not hold the test server's self-signed certificate.
        Assert.assertThrows(IOException.class, () -> builder().build().health());
    }

    @Test
    public void hostnameVerificationStaysOn() {

        // Trusted, but issued for philter.invalid rather than localhost.
        startServer(otherHostKeyStore, false);

        final PhilterClient client = builder()
                .withTrustStore(clientTrustStore, PASSWORD.toCharArray())
                .build();

        final IOException ex = Assert.assertThrows(IOException.class, client::health);
        Assert.assertTrue("Refused for its certificate: " + ex, causedBy(ex, CertificateException.class));
    }

    // An SSLContext.

    @Test
    public void anSslContextIsUsedAsGiven() throws Exception {

        startServer(serverKeyStore, true);

        final PhilterClient client = builder()
                .withSslContext(sslContext(clientKeyStore, clientTrustStore))
                .build();

        Assert.assertEquals("UP", client.health().getStatus());
    }

    @Test
    public void theSdkDefaultsStillApplyWithTls() throws Exception {

        final SSLContext context = sslContext(clientKeyStore, clientTrustStore);

        final HttpClient http = httpClientOf(new PhilterClient.PhilterClientBuilder()
                .withEndpoint("https://localhost:8080")
                .withTimeout(7)
                .withSslContext(context)
                .build());

        Assert.assertSame(context, http.sslContext());
        Assert.assertEquals(Duration.ofSeconds(7), http.connectTimeout().orElse(null));
        Assert.assertEquals(HttpClient.Redirect.NORMAL, http.followRedirects());
        Assert.assertEquals(HttpClient.Version.HTTP_1_1, http.version());
        if (ProxySelector.getDefault() != null) {
            Assert.assertSame(ProxySelector.getDefault(), http.proxy().orElse(null));
        }
    }

    // Combinations that are refused.

    @Test
    public void anSslContextCannotBeCombinedWithKeyStores() throws Exception {

        final SSLContext context = sslContext(clientKeyStore, clientTrustStore);

        Assert.assertThrows(IllegalStateException.class, () -> builder().withSslContext(context)
                .withTrustStore(clientTrustStore, PASSWORD.toCharArray()).build());
        Assert.assertThrows(IllegalStateException.class, () -> builder().withSslContext(context)
                .withClientCertificate(clientKeyStore, PASSWORD.toCharArray()).build());
    }

    @Test
    public void tlsOptionsCannotBeCombinedWithAnHttpClientBuilder() throws Exception {

        final SSLContext context = sslContext(clientKeyStore, clientTrustStore);

        final IllegalStateException ex = Assert.assertThrows(IllegalStateException.class,
                () -> builder().withHttpClientBuilder(HttpClient.newBuilder())
                        .withClientCertificate(clientKeyStore, PASSWORD.toCharArray()).build());
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("withHttpClientBuilder"));

        Assert.assertThrows(IllegalStateException.class, () -> builder().withHttpClientBuilder(HttpClient.newBuilder())
                .withTrustStore(clientTrustStore, PASSWORD.toCharArray()).build());
        Assert.assertThrows(IllegalStateException.class, () -> builder().withHttpClientBuilder(HttpClient.newBuilder())
                .withSslContext(context).build());
    }

    // Stores that cannot be used.

    @Test
    public void aMissingKeyStoreNamesTheFile() {

        final Path missing = FOLDER.getRoot().toPath().resolve("missing.p12");

        final IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                () -> builder().withClientCertificate(missing, PASSWORD.toCharArray()).build());

        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains(missing.toString()));
        assertNoPassword(ex, PASSWORD);
    }

    @Test
    public void aWrongPasswordNamesTheFileAndNotThePassword() {

        final String wrong = "not-the-store-password-42";

        final IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                () -> builder().withClientCertificate(clientKeyStore, wrong.toCharArray()).build());
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains(clientKeyStore.toString()));
        assertNoPassword(ex, wrong);

        final IllegalArgumentException trust = Assert.assertThrows(IllegalArgumentException.class,
                () -> builder().withTrustStore(clientTrustStore, wrong.toCharArray()).build());
        Assert.assertTrue(trust.getMessage(), trust.getMessage().contains(clientTrustStore.toString()));
        assertNoPassword(trust, wrong);
    }

    @Test
    public void aFileThatIsNotAKeyStoreIsRefused() throws Exception {

        final Path text = FOLDER.newFile("not-a-keystore.p12").toPath();
        Files.write(text, "not a keystore".getBytes(StandardCharsets.UTF_8));

        final IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                () -> builder().withTrustStore(text, PASSWORD.toCharArray()).build());
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains(text.toString()));
    }

    @Test
    public void aTrustStoreWithNoTrustedCertificatesIsRefused() throws Exception {

        // As a PKCS12 file from openssl pkcs12 -nokeys reads in Java.
        final Path empty = FOLDER.getRoot().toPath().resolve("empty-trust.p12");
        final KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        try (OutputStream out = Files.newOutputStream(empty)) {
            store.store(out, PASSWORD.toCharArray());
        }

        final IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                () -> builder().withTrustStore(empty, PASSWORD.toCharArray()).build());
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains(empty.toString()));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("keytool -importcert"));
    }

    @Test
    public void aClientCertificateKeyStoreWithoutAPrivateKeyIsRefused() {

        final IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                () -> builder().withClientCertificate(clientTrustStore, PASSWORD.toCharArray()).build());
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("holds no private key"));
    }

    // PhilterTls, for a caller's own HttpClient.Builder.

    @Test
    public void philterTlsWorksWithACallersOwnHttpClientBuilder() throws Exception {

        startServer(serverKeyStore, true);

        final SSLContext context = PhilterTls.sslContext(clientKeyStore, PASSWORD.toCharArray(),
                clientTrustStore, PASSWORD.toCharArray());

        final PhilterClient client = builder()
                .withHttpClientBuilder(ownBuilder(context))
                .build();

        Assert.assertEquals("UP", client.health().getStatus());
    }

    @Test
    public void philterTlsWithATrustStoreAlonePresentsNoClientCertificate() throws Exception {

        final SSLContext context = PhilterTls.sslContext(null, null, clientTrustStore, PASSWORD.toCharArray());

        startServer(serverKeyStore, false);
        Assert.assertEquals("UP", builder().withHttpClientBuilder(ownBuilder(context))
                .build().health().getStatus());

        server.stop(0);
        startServer(serverKeyStore, true);
        final PhilterClient refused = builder().withHttpClientBuilder(ownBuilder(context))
                .build();
        Assert.assertThrows(IOException.class, refused::health);
    }

    @Test
    public void philterTlsNeedsAtLeastOneStore() {
        Assert.assertThrows(IllegalArgumentException.class, () -> PhilterTls.sslContext(null, null, null, null));
    }

    @Test
    public void philterTlsNeedsTheKeyStorePassword() {

        final IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                () -> PhilterTls.sslContext(clientKeyStore, null, null, null));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains(clientKeyStore.toString()));
    }

    @Test
    public void philterTlsRefusesUnusableStoresAsTheBuilderDoes() throws Exception {

        final String wrong = "not-the-store-password-42";
        final Path missing = FOLDER.getRoot().toPath().resolve("missing-for-philter-tls.p12");

        final IllegalArgumentException wrongPassword = Assert.assertThrows(IllegalArgumentException.class,
                () -> PhilterTls.sslContext(clientKeyStore, wrong.toCharArray(), null, null));
        Assert.assertEquals(builderRefusal(() -> builder().withClientCertificate(clientKeyStore, wrong.toCharArray())),
                wrongPassword.getMessage());
        assertNoPassword(wrongPassword, wrong);

        final IllegalArgumentException missingFile = Assert.assertThrows(IllegalArgumentException.class,
                () -> PhilterTls.sslContext(null, null, missing, PASSWORD.toCharArray()));
        Assert.assertEquals(builderRefusal(() -> builder().withTrustStore(missing, PASSWORD.toCharArray())),
                missingFile.getMessage());

        final IllegalArgumentException noKey = Assert.assertThrows(IllegalArgumentException.class,
                () -> PhilterTls.sslContext(clientTrustStore, PASSWORD.toCharArray(), null, null));
        Assert.assertTrue(noKey.getMessage(), noKey.getMessage().contains("holds no private key"));
    }

    // Helpers.

    /** A caller's own HTTP client builder using {@code context}, with a connect timeout as a caller should set. */
    private static HttpClient.Builder ownBuilder(final SSLContext context) {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).sslContext(context);
    }

    /** The message of the {@link IllegalArgumentException} the builder's {@code build()} throws. */
    private static String builderRefusal(final Supplier<PhilterClient.PhilterClientBuilder> builder) {
        return Assert.assertThrows(IllegalArgumentException.class, () -> builder.get().build()).getMessage();
    }

    private PhilterClient.PhilterClientBuilder builder() {
        final int port = server == null ? 8080 : server.getAddress().getPort();
        return new PhilterClient.PhilterClientBuilder()
                .withEndpoint("https://localhost:" + port)
                .withTimeout(5);
    }

    /** Starts an HTTPS server answering {@code /api/health}, requiring a client certificate when asked. */
    private void startServer(final Path keyStore, final boolean requireClientCertificate) {

        try {

            server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext(keyStore, serverTrustStore)) {
                @Override
                public void configure(final HttpsParameters parameters) {
                    final SSLParameters ssl = getSSLContext().getDefaultSSLParameters();
                    ssl.setNeedClientAuth(requireClientCertificate);
                    parameters.setSSLParameters(ssl);
                }
            });

            server.createContext("/", exchange -> {
                final byte[] body = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });

            server.start();

        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }

    }

    private static SSLContext sslContext(final Path keyStore, final Path trustStore) throws Exception {

        final KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(KeyStore.getInstance(keyStore.toFile(), PASSWORD.toCharArray()), PASSWORD.toCharArray());

        final TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(KeyStore.getInstance(trustStore.toFile(), PASSWORD.toCharArray()));

        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys.getKeyManagers(), trust.getTrustManagers(), null);
        return context;

    }

    private static HttpClient httpClientOf(final PhilterClient client) throws Exception {
        final Field field = PhilterClient.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        return (HttpClient) field.get(client);
    }

    private static boolean causedBy(final Throwable ex, final Class<? extends Throwable> type) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private static void assertNoPassword(final Throwable ex, final String password) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            Assert.assertFalse("The password appears in " + t, String.valueOf(t.getMessage()).contains(password));
        }
    }

    private static Path keyPair(final Path dir, final String name, final String dname, final String san)
            throws Exception {

        final Path keyStore = dir.resolve(name);
        final List<String> args = new ArrayList<>(Arrays.asList("-genkeypair", "-alias", "key", "-keyalg", "RSA",
                "-keysize", "2048", "-validity", "2", "-dname", dname, "-keystore", keyStore.toString(),
                "-storetype", "PKCS12", "-storepass", PASSWORD, "-keypass", PASSWORD));
        if (san != null) {
            args.add("-ext");
            args.add(san);
        }
        keytool(args.toArray(new String[0]));
        return keyStore;

    }

    private static Path exportCertificate(final Path keyStore, final Path certificate) throws Exception {
        keytool("-exportcert", "-alias", "key", "-keystore", keyStore.toString(), "-storepass", PASSWORD,
                "-file", certificate.toString());
        return certificate;
    }

    private static void trust(final Path trustStore, final String alias, final Path certificate) throws Exception {
        keytool("-importcert", "-noprompt", "-alias", alias, "-file", certificate.toString(),
                "-keystore", trustStore.toString(), "-storetype", "PKCS12", "-storepass", PASSWORD);
    }

    private static void keytool(final String... args) throws Exception {

        final Path bin = Paths.get(System.getProperty("java.home"), "bin");
        final Path keytool = Files.exists(bin.resolve("keytool")) ? bin.resolve("keytool") : bin.resolve("keytool.exe");

        final List<String> command = new ArrayList<>();
        command.add(keytool.toString());
        command.addAll(Arrays.asList(args));

        final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException("keytool " + args[0] + " failed: " + output);
        }

    }

}
