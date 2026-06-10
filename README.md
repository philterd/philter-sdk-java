# Philter SDK for Java

The **Philter SDK for Java** is an API client for [Philter](https://www.philterd.ai). Philter identifies and manipulates
sensitive information such as Protected Health Information (PHI) and personally identifiable information (PII) in
natural language text. Philter is built upon the open source PII/PHI detection
engine [Phileas](https://github.com/philterd/phileas).

Refer to the [Philter API specification](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/openapi.json)
for details on the available endpoints.

## Installation

Releases are available from [Maven Central](https://central.sonatype.com/artifact/ai.philterd/philter-sdk-java). Add the
dependency to your Maven configuration:

```
<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>philter-sdk-java</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Compatibility

As of version 2.0.0, this client targets the **Philter 4.0.0** API. Earlier versions of the client are not
compatible with Philter 4.0.0 and later.

## Example Usage

With an available running instance of Philter, to filter text:

```
PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://127.0.0.1:8080")
        .withApiKey("your-api-key")
        .build();

FilterResponse filterResponse = client.filter("context", "default", text);
```

To filter text with explanation:

```
ExplainResponse explainResponse = client.explain("context", "default", text);
```

Philter 4.0.0 expects an `Authorization` header on nearly every endpoint. Provide its value with
`withApiKey(...)`; the value is sent verbatim, so include any scheme prefix (for example `"Bearer "`) if your
deployment requires it. The `health()` endpoint does not require authentication.

In addition to filtering, the client covers the full Philter 4.0.0 API: policies (including versions, diffs, and
rollbacks), contexts, documents, legal holds, the redaction ledger, custom lists, redact lists, and re-identification.

## Testing

Unit tests run against a mocked HTTP server and execute on every build with no external dependencies.

Live integration tests (`PhilterClientTest`) run against a real Philter instance and are skipped unless
`PHILTER_ENDPOINT` is set. To run them:

```
export PHILTER_ENDPOINT=https://localhost:8080/
export PHILTER_API_KEY=your-api-key   # optional
export PHILTER_INSECURE=true          # optional, to trust self-signed certificates
mvn test
```

Additional optional variables: `PHILTER_KEYSTORE`, `PHILTER_KEYSTORE_PASSWORD`, `PHILTER_TRUSTSTORE`,
`PHILTER_TRUSTSTORE_PASSWORD` (for mutual TLS) and `PHILTER_PDF_FILE` (to enable the PDF filtering test).

## Release History

See [RELEASE_NOTES.md](RELEASE_NOTES.md) for the release history.

## License

This project is licensed under the Apache License, version 2.0.

Copyright 2026 Philterd, LLC.
Philter is a registered trademark of Philterd, LLC.
