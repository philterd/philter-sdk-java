# Philter SDK for Java

The **Philter SDK for Java** is an API client for [Philter](https://www.philterd.ai). Philter identifies and manipulates
sensitive information such as Protected Health Information (PHI) and personally identifiable information (PII) in
natural language text. Philter is built upon the open source PII/PHI detection
engine [Phileas](https://github.com/philterd/phileas).

Refer to the [Philter API](https://docs.philterd.ai/philter/latest/api-1-readme.html) documentation for details on the
methods available.

## Snapshots and Releases

As of version 1.6.0-SNAPSHOT, snapshots and releases are available from Maven Central. Previous versions were available
from our [Maven repositories](https://artifacts.philterd.ai/) so add the following to your Maven configuration:

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

* 1.6.0 (not yet released):
   * Updated dependencies. 
   * Now available from Maven central.
* 1.5.0:
   * Updated dependencies.
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

Copyright 2024 Philterd, LLC.
Philter is a registered trademark of Philterd, LLC.
