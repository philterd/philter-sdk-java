# Philter SDK for Java

The **Philter SDK for Java** is an API client for [Philter](https://www.philterd.ai). Philter identifies and manipulates sensitive information such as Protected Health Information (PHI) and personally identifiable information (PII) in natural language text. Philter is built upon the open source PII/PHI detection engine [Phileas](https://github.com/philterd/phileas).

Refer to the [Philter API](https://docs.philterd.ai/philter/latest/api-1-readme.html) documentation for details on the methods available.

## Requirements

Java 11 or later. The client uses the JDK's built-in HTTP client and has a single runtime dependency, Gson.

## Snapshots and Releases

Snapshots and releases are available in our [Maven repositories](https://artifacts.philterd.ai/) so add the following to your Maven configuration:

```
<repository>
    <id>philterd-repository-releases</id>
    <url>https://artifacts.philterd.ai/releases</url>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
</repository>
<repository>
    <id>philterd-repository-snapshots</id>
    <url>https://artifacts.philterd.ai/snapshots</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

## Example Usage

With an available running instance of Philter, to filter text:

```
PhilterClient client = new PhilterClient.PhilterClientBuilder().withEndpoint("https://127.0.0.1:8080").build();
FilterResponse filterResponse = client.filter(text);
```

To filter text with explanation:

```
PhilterClient client = new PhilterClient.PhilterClientBuilder().withEndpoint("https://127.0.0.1:8080").build();
ExplainResponse explainResponse = client.explain(text);
```

## Release History

* 1.6.0:
  * Replaced Retrofit and OkHttp with the JDK's `java.net.http.HttpClient`. The only remaining runtime dependency is Gson.
  * **Breaking:** requires Java 11 or later.
  * **Breaking:** `PhilterClientBuilder.withOkHttpClientBuilder(OkHttpClient.Builder)` is replaced by `withHttpClientBuilder(HttpClient.Builder)`.
  * **Deprecated:** `withMaxIdleConnections` and `withKeepAliveDurationMs` no longer have any effect. The JDK client is tuned with the `jdk.httpclient.connectionPoolSize` and `jdk.httpclient.keepalive.timeout` system properties.
  * Replaced the `sslcontext-kickstart`/`ayza`, `commons-lang3` and `commons-io` dependencies with the equivalent JDK APIs.
  * **Breaking:** removed the unused `FilteredSpan` model class. It was superseded by `Span`, which carries the same fields plus `id`, `text`, `salt` and `ignored`, and is what `explain()` returns.
  * **Breaking:** removed the `AbstractClient` base class; its constants and response handling now live on `PhilterClient`. The `UNAUTHORIZED` and `SERVICE_UNAVAILABLE` constants remain accessible as `PhilterClient.UNAUTHORIZED` and `PhilterClient.SERVICE_UNAVAILABLE`.
  * `withEndpoint` now accepts an endpoint with or without a trailing slash.
  * **Behaviour change:** the response body no longer has a read timeout. `withTimeout` now bounds the connect and the wait for response headers; OkHttp additionally aborted a response whose body stalled mid-transfer, and the JDK client has no equivalent setting.
  * **Behaviour change:** an HTTPS endpoint that redirects to HTTP is no longer followed.
  * Requests are pinned to HTTP/1.1. Pass a client configured for HTTP/2 to `withHttpClientBuilder` to opt in.
  * Fixed a `NullPointerException` when `withSslConfiguration` was given a keystore but no truststore; the JDK's default trust material is now used.
* 1.4.0:
  * Modified /api/status response.
  * Renamed profiles to policies.
* 1.3.1:
  * Changed from com.mtnfog to ai.philterd.
* 1.3.0:
  * Added support for SSL authentication.
  * Added support for filtering PDF documents.
  * Removed token-based API authentication.
  * Removed models client.
* 1.2.0:
  * Added option for API authentication support.
  * Added `salt` to `Span` for when the `HASH_SHA256_REPLACE` filter strategy is applied by Philter.
  * Changed artifact name to `philter-sdk-java`
  * Added alerts retrieval/deletion to client.
  * Added models client.
* 1.1.0:
  * Various changes/fixes.
  * Split SDKs into separate projects.
* 1.0.0:
  * Initial release.

## License

This project is licensed under the Apache License, version 2.0.

Copyright 2026 Philterd, LLC.
Philter is a registered trademark of Philterd, LLC.
