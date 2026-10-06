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

For full control over the HTTP stack (proxies, custom TLS, a custom executor, and so on) supply your own `HttpClient.Builder`. The connect timeout is then yours to configure; the per-request timeout, the `Authorization` header, and the [client address](#requests-on-behalf-of-a-person) are still applied:

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

Philter uses the header only when the request comes from an address in its `TRUSTED_PROXIES`, which by default are the loopback, private, link-local, and IPv6 unique-local ranges; otherwise, and for a value that is not an IP address, it uses the address of the connection. A port is allowed and ignored. Return an address the application determined itself, such as the remote address of the person's connection to it, not one the person's browser supplied, which they can set to anything.
