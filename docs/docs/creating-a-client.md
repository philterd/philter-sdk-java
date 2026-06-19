# Creating a Client

Clients are created with the `PhilterClientBuilder`. Only the endpoint is required. Philter 4.0.0 expects an `Authorization` header on nearly every endpoint, so supply your API key with `withApiKey(...)`. The value is sent verbatim, so include any scheme prefix (for example `"Bearer "`) if your deployment requires it.

```java
import ai.philterd.philter.PhilterClient;

PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://localhost:8080")
        .withApiKey("your-api-key")
        .build();
```

A `PhilterClient` is thread-safe and reusable, so create one and share it for the lifetime of your application. Connection settings can be tuned on the builder:

```java
PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://localhost:8080")
        .withApiKey("your-api-key")
        .withTimeout(60)                 // connect/read/write timeout, seconds
        .withMaxIdleConnections(20)
        .withKeepAliveDurationMs(30000)
        .build();
```

For full control over the HTTP stack (proxies, custom TLS, logging interceptors, and so on) supply your own OkHttp builder. When you do, the timeout and pool settings above are not applied, so configure them on your builder:

```java
import okhttp3.OkHttpClient;

OkHttpClient.Builder http = new OkHttpClient.Builder();
// ...configure timeouts, interceptors, TLS, etc...

PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://localhost:8080")
        .withApiKey("your-api-key")
        .withOkHttpClientBuilder(http)
        .build();
```
