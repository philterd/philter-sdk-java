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
 * Builds the {@link SSLContext} for {@link PhilterClient.PhilterClientBuilder#withClientCertificate} and
 * {@link PhilterClient.PhilterClientBuilder#withTrustStore} with the JDK alone.
 */
final class TlsContexts {

    private TlsContexts() {
    }

    /**
     * An {@link SSLContext} presenting the key in {@code keyStore}, when given, and trusting the certificates in
     * {@code trustStore}, when given, or the JVM's default trust store when not. A store that cannot be used is
     * refused with an {@link IllegalArgumentException} naming the file, never the password.
     */
    static SSLContext create(final Path keyStore, final char[] keyStorePassword, final Path trustStore,
                             final char[] trustStorePassword) {

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
