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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collections;

/**
 * Builds an {@link SSLContext} from keystore files with the JDK alone, as
 * {@link PhilterClient.PhilterClientBuilder#withClientCertificate(Path, char[])} and
 * {@link PhilterClient.PhilterClientBuilder#withTrustStore(Path, char[])} do.
 *
 * <p>Use it when supplying your own {@link java.net.http.HttpClient.Builder} with
 * {@link PhilterClient.PhilterClientBuilder#withHttpClientBuilder(java.net.http.HttpClient.Builder)}, which the
 * builder's TLS options cannot be combined with. Set the context on your builder:</p>
 *
 * <pre>{@code
 * SSLContext context = PhilterTls.sslContext(Path.of("client.p12"), keyStorePassword,
 *         Path.of("ca.p12"), trustStorePassword);
 *
 * HttpClient.Builder http = HttpClient.newBuilder()
 *         .connectTimeout(Duration.ofSeconds(30))
 *         .sslContext(context);
 *
 * PhilterClient client = new PhilterClient.PhilterClientBuilder()
 *         .withEndpoint("https://philter.internal.example.com:8080")
 *         .withHttpClientBuilder(http)
 *         .build();
 * }</pre>
 *
 * <p>A supplied builder gets none of the SDK's HTTP client defaults, so set what you need on it: the JDK's
 * builder has no connect timeout and follows no redirects unless told to. Without your own builder, use the
 * builder's TLS options instead, which keep those defaults.</p>
 */
public final class PhilterTls {

    private PhilterTls() {
    }

    /**
     * Builds an {@link SSLContext} that presents the client certificate in {@code keyStore}, when given, and trusts
     * the certificates in {@code trustStore}, when given, or the JVM's default trust store when not. Each file is
     * a PKCS12 or JKS store, its format detected from its contents, and is read now.
     *
     * <p>The client certificate's private key must use the keystore's password, as it does in a PKCS12 file.
     * Without a {@code keyStore}, no client certificate is presented; the {@code javax.net.ssl.keyStore} system
     * property is not used.</p>
     *
     * @param keyStore The keystore holding the client certificate and its private key. May be {@code null} for
     * no client certificate.
     * @param keyStorePassword The keystore's password. Required when {@code keyStore} is given.
     * @param trustStore The trust store holding the certificates to trust for the server. May be {@code null} for
     * the JVM's default trust store.
     * @param trustStorePassword The trust store's password. May be {@code null} for a store without one, in which
     * case its integrity is not checked.
     * @return The TLS context.
     * @throws IllegalArgumentException Thrown if neither store is given, if {@code keyStore} is given without a
     * password, or if a store is missing, cannot be read with its password, or holds nothing usable (no private
     * key in the keystore, no trusted certificates in the trust store). A message about a store names its file,
     * and no message contains a password.
     */
    public static SSLContext sslContext(final Path keyStore, final char[] keyStorePassword, final Path trustStore,
                                        final char[] trustStorePassword) {

        if (keyStore == null && trustStore == null) {
            throw new IllegalArgumentException("Give a client certificate keystore, a trust store, or both.");
        }

        if (keyStore != null && keyStorePassword == null) {
            throw new IllegalArgumentException("The client certificate keystore " + keyStore + " needs its password.");
        }

        KeyManager[] keyManagers = null;

        if (keyStore != null) {

            final KeyStore store = load(keyStore, keyStorePassword, "client certificate keystore");

            try {
                if (Collections.list(store.aliases()).stream().noneMatch(alias -> isKeyEntry(store, alias))) {
                    throw new IllegalArgumentException("The client certificate keystore " + keyStore
                            + " holds no private key.");
                }
                final KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                factory.init(store, keyStorePassword);
                keyManagers = factory.getKeyManagers();
            } catch (final GeneralSecurityException ex) {
                throw new IllegalArgumentException("The private key in the client certificate keystore " + keyStore
                        + " could not be read. Its password must be the keystore's password.", ex);
            }

        }

        TrustManager[] trustManagers = null;

        if (trustStore != null) {

            final KeyStore store = load(trustStore, trustStorePassword, "trust store");

            try {
                if (store.size() == 0) {
                    // Java skips certificates without its trusted-certificate attribute, which a PKCS12 file
                    // from openssl pkcs12 -nokeys lacks, so such a file reads as empty.
                    throw new IllegalArgumentException("The trust store " + trustStore + " holds no trusted "
                            + "certificates. A PKCS12 trust store made with openssl reads as empty in Java; build it "
                            + "with keytool -importcert instead.");
                }
                final TrustManagerFactory factory =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                factory.init(store);
                trustManagers = factory.getTrustManagers();
            } catch (final GeneralSecurityException ex) {
                throw new IllegalArgumentException("The trust store " + trustStore + " could not be used.", ex);
            }

        }

        try {
            final SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagers, trustManagers, null);
            return context;
        } catch (final GeneralSecurityException ex) {
            throw new IllegalStateException("The JVM could not create a TLS context.", ex);
        }

    }

    /** Loads a PKCS12 or JKS store, detecting the format from the file. */
    private static KeyStore load(final Path path, final char[] password, final String what) {
        try {
            return KeyStore.getInstance(path.toFile(), password);
        } catch (final IOException | GeneralSecurityException | IllegalArgumentException ex) {
            // The JDK's messages name neither the file nor the password, so name the file here.
            throw new IllegalArgumentException("The " + what + " " + path + " could not be read. Check that it "
                    + "exists, is a PKCS12 or JKS file, and that the password is correct: " + ex.getMessage(), ex);
        }
    }

    private static boolean isKeyEntry(final KeyStore store, final String alias) {
        try {
            return store.isKeyEntry(alias);
        } catch (final GeneralSecurityException ex) {
            return false;
        }
    }

}
