# Philter SDK for Java

The **Philter SDK for Java** is an API client for [Philter](https://www.philterd.ai). Philter identifies and manipulates sensitive information such as Protected Health Information (PHI) and personally identifiable information (PII) in natural language text. Philter is built upon the open source PII/PHI detection engine [Phileas](https://github.com/philterd/phileas).

Refer to the [Philter API specification](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/openapi.json) for details on the available endpoints.

## Installation

Releases are available from [Maven Central](https://central.sonatype.com/artifact/ai.philterd/philter-sdk-java). Add the dependency to your Maven configuration:

```xml
<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>philter-sdk-java</artifactId>
    <version>2.0.0</version>
</dependency>
```

To track the latest development build, depend on the current `-SNAPSHOT` version and add the Maven Central snapshot repository to your build (snapshots are not served from the default Maven Central repository):

```xml
<repositories>
    <repository>
        <id>central-portal-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>philter-sdk-java</artifactId>
    <version>2.1.0-SNAPSHOT</version>
</dependency>
```

Snapshots are development builds: they are mutable and are periodically pruned, so pin a release version for anything you need to reproduce.

## Compatibility

As of version 2.0.0, this client targets the **Philter 4.0.0** API. Earlier versions of the client are not compatible with Philter 4.0.0 and later.

| philter-sdk-java | Philter API |
|------------------|-------------|
| 2.0.0 and later  | 4.0.0       |
| 1.x              | 1.x to 3.x  |

## Usage

With an available running instance of Philter, to filter text:

```java
PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://127.0.0.1:8080")
        .withApiKey("your-api-key")
        .build();

FilterResponse filterResponse = client.filter("context", "default", text);
```

To filter text with explanation:

```java
ExplainResponse explainResponse = client.explain("context", "default", text);
```

Philter 4.0.0 expects an `Authorization` header on nearly every endpoint. Provide its value with `withApiKey(...)`; the value is sent verbatim, so include any scheme prefix (for example `"Bearer "`) if your deployment requires it. The `health()` endpoint does not require authentication.

In addition to filtering, the client covers the full Philter 4.0.0 API: policies (including versions, diffs, and rollbacks), contexts, documents, legal holds, the redaction ledger, custom lists, redact lists, and re-identification.

## License

This project is licensed under the Apache License, version 2.0.

Copyright 2026 Philterd, LLC. Philter is a registered trademark of Philterd, LLC.
