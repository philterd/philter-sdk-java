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

For full control over the HTTP stack (proxies, custom TLS, a custom executor, and so on) supply your own `HttpClient.Builder`. The connect timeout is then yours to configure; the per-request timeout and the `Authorization` header are still applied:

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
