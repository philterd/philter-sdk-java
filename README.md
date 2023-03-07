# Philter SDK for Java

The **Philter SDK for Java** is an API client for [Philter](https://www.mtnfog.com/products/philter/). Philter identifies and manipulates sensitive information such as Protected Health Information (PHI) and personally identifiable information (PII) in natural language text. Philter is built upon the open source PII/PHI detection engine [Phileas](https://github.com/philterd/phileas).

Refer to the [Philter API](https://docs.mtnfog.com/philter/api/api-1/api) documentation for details on the methods available.

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

## Dependency

Release dependencies are available in Maven Central.

```
<dependency>
  <groupId>io.philterd</groupId>
  <artifactId>philter-sdk-java</artifactId>
  <version>1.3.0</version>
</dependency>
```

Snapshot dependencies are available in the Maven Central Snapshot Repository by adding the repository to your `pom.xml`:

```
<repository>
  <id>snapshots</id>
  <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
  <releases><enabled>false</enabled></releases>
  <snapshots><enabled>true</enabled></snapshots>
</repository>
```

## Release History

* 1.3.1:
  * Changed from com.mtnfog to io.philterd.
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

Copyright 2019-2023 Mountain Fog, Inc.
Philter is a registered trademark of Mountain Fog, Inc.
