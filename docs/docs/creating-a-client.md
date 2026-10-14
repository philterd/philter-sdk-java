# Creating a Client

Clients are created with the `PhilterClientBuilder`. Only the endpoint is required. Philter 4.0.0 expects an `Authorization` header on nearly every endpoint, so supply your API key with `withApiKey(...)`. The value is sent verbatim, so include any scheme prefix (for example `"Bearer "`) if your deployment requires it.

```java
import ai.philterd.philter.PhilterClient;

PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://localhost:8080")
        .withApiKey("your-api-key")
        .build();
```

A `PhilterClient` is thread-safe and reusable, so create one and share it for the lifetime of your application. The timeout can be set on the builder:

```java
PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://localhost:8080")
        .withApiKey("your-api-key")
        .withTimeout(60)                 // connect and per-request timeout, seconds
        .build();
```

Requests are made with the JDK's `java.net.http.HttpClient`, so the SDK adds no third-party HTTP dependency to your application.

For full control over the HTTP stack (proxies, a custom executor, and so on) supply your own `HttpClient.Builder`. The connect timeout is then yours to configure; the per-request timeout, the `Authorization` header, and the [client address](#requests-on-behalf-of-a-person) are still applied. The [TLS options](#tls-and-client-certificates) below configure the SDK's own HTTP client, so they cannot be combined with your own builder: configure TLS on it instead.

```java
import java.net.http.HttpClient;
import java.time.Duration;

HttpClient.Builder http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(60));
// ...configure TLS, proxy, executor, etc...

PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://localhost:8080")
        .withApiKey("your-api-key")
        .withHttpClientBuilder(http)
        .build();
```

Connection pooling is tuned with the JDK's own system properties (`jdk.httpclient.connectionPoolSize`, `jdk.httpclient.keepalive.timeout`) rather than on the builder. `withMaxIdleConnections(...)` and `withKeepAliveDurationMs(...)` remain on the builder for source compatibility but have no effect.

## TLS and client certificates

By default the client trusts the server certificates the JVM trusts and presents no client certificate. Three builder options change that, without giving up the SDK's other defaults such as the connect timeout:

- `withClientCertificate(Path keyStore, char[] password)` presents a client certificate for mutual TLS, from a PKCS12 or JKS keystore holding the certificate and its private key. Use it when Philter sits behind a load balancer, ingress, or service mesh that requires client certificates.
- `withTrustStore(Path trustStore, char[] password)` trusts the certificates in a PKCS12 or JKS trust store instead of the JVM's defaults. Use it when the server's certificate comes from a private certificate authority or is self-signed, such as the certificate the Philter Docker image generates.
- `withSslContext(SSLContext)` uses an `SSLContext` you build, for anything the other two do not cover, such as a key in a hardware module or certificates loaded from PEM files.

```java
import java.nio.file.Path;

PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://philter.internal.example.com:8080")
        .withApiKey("your-api-key")
        .withClientCertificate(Path.of("/etc/philter-client/client.p12"), keyStorePassword)
        .withTrustStore(Path.of("/etc/philter-client/ca.p12"), trustStorePassword)
        .build();
```

Either keystore option can be used alone: a client certificate with the JVM's default trust, or a trust store without a client certificate. The format of each file is detected from its contents. A client certificate's private key must use the keystore's password, as it does in a PKCS12 file. Passwords are copied, so you may clear your arrays once the builder has them.

The files are read when `build()` is called. A file that is missing, cannot be read with its password, or holds nothing usable (no private key in a client certificate keystore, no trusted certificates in a trust store) is refused with an `IllegalArgumentException` that names the file and never the password. Combining `withSslContext` with either keystore option, or any of the three with `withHttpClientBuilder`, is refused with an `IllegalStateException`.

Hostname verification stays on with every option: the server's certificate must name the host in the endpoint. If the server asks for a client certificate and the client has none it accepts, the call fails with an `IOException`.

To build a PKCS12 keystore from PEM files, for example ones issued by cert-manager or a service mesh:

```bash
openssl pkcs12 -export -in client.crt -inkey client.key -out client.p12
keytool -importcert -noprompt -alias ca -file ca.crt -keystore ca.p12 -storetype PKCS12 -storepass <password>
```

Build the trust store with `keytool -importcert` as shown. A PKCS12 trust store made with `openssl pkcs12 -export -nokeys` reads as empty in Java, because its certificates lack the attribute Java uses to mark trusted certificates, and `build()` refuses it.

### With your own HTTP client builder

The TLS options above cannot be combined with `withHttpClientBuilder`, since your builder carries its own TLS settings. To use the same keystores with your own builder, build the `SSLContext` with `PhilterTls.sslContext` and set it on your builder:

```java
import ai.philterd.philter.PhilterTls;
import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

SSLContext context = PhilterTls.sslContext(
        Path.of("/etc/philter-client/client.p12"), keyStorePassword,
        Path.of("/etc/philter-client/ca.p12"), trustStorePassword);

HttpClient.Builder http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(60))
        .sslContext(context);

PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://philter.internal.example.com:8080")
        .withApiKey("your-api-key")
        .withHttpClientBuilder(http)
        .build();
```

Your builder gets none of the SDK's HTTP client defaults, so set a connect timeout on it as shown: the JDK's builder has none, and follows no redirects unless told to.

`PhilterTls.sslContext` reads the files the same way `withClientCertificate` and `withTrustStore` do, and refuses an unusable file with the same `IllegalArgumentException`. Either store may be `null`, but not both: with no keystore no client certificate is presented, and with no trust store the JVM's default trust applies. The files are read when it is called, so build the context once and share it across clients.

## Requests on behalf of a person

An application that calls Philter for the people using it, such as a web front end, can pass each person's address with `withClientAddress`. It is sent as `X-Forwarded-For` on every request, so wherever Philter records a request's client address, such as on audit events and for the sign-in rate limit, it records the person's address rather than the application's:

```java
PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://localhost:8080")
        .withApiKey("Bearer " + sessionKey)
        .withClientAddress(() -> person.getAddress())
        .build();
```

The supplier is called for each request, on the thread making it, so an address that changes during a session is sent from the next request, and a client shared across people can return the address of the person whose request is being handled. A `null` result, an empty string, or only spaces sends no header. A result containing a comma, a control character such as a line break, a character outside ASCII, or a space within it is refused with an `IllegalArgumentException` before the request is sent.

Philter uses the header only when the request comes from an address in its `TRUSTED_PROXIES`, which by default is loopback only, so unless the application runs on the same host as Philter, list the application's address there; otherwise, and for a value that is not an IP address, it uses the address of the connection. A port is allowed and ignored. Return an address the application determined itself, such as the remote address of the person's connection to it, not one the person's browser supplied, which they can set to anything.
