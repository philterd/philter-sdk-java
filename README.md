# Philter SDK for Java

The **Philter SDK for Java** enables Java developers to easily work with Philter. [Philter](https://www.mtnfog.com/products/philter/) identifies and manipulates sensitive information like Protected Health Information (PHI) and personally identifiable information (PII) from natural language text.

Refer to [Philter API](https://philter.mtnfog.com/api/) documentation for details on the methods available.

[![Build Status](https://travis-ci.org/mtnfog/philter-sdk-java.svg?branch=master)](https://travis-ci.org/mtnfog/philter-sdk-java)
![Maven Central](https://img.shields.io/maven-central/v/com.mtnfog/philter-java-sdk)
[![javadoc](https://javadoc.io/badge2/com.mtnfog/philter-java-sdk/javadoc.svg)](https://javadoc.io/doc/com.mtnfog/philter-java-sdk)

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
  <groupId>com.mtnfog</groupId>
  <artifactId>philter-sdk-java</artifactId>
  <version>1.2.0</version>
</dependency>
```

Snapshot dependencies are available in the Maven Central Snapshot Repository by adding the repository to your `pom.xml`:

```
<repository>
  <id>snapshots</id>
  <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  <releases><enabled>false</enabled></releases>
  <snapshots><enabled>true</enabled></snapshots>
</repository>
```

## Release History

* 1.2.0:
  * Added option for API authentication support.
  * Added `salt` to `Span` for when the `HASH_SHA256_REPLACE` filter strategy is applied by Philter.
  * Changed artifact name to `philter-sdk-java`
  * Added alerts retrieval/deletion to client.
* 1.1.0:
  * Various changes/fixes.
  * Split SDKs into separate projects.
* 1.0.0:
  * Initial release.

## License

This project is licensed under the Apache License, version 2.0.

Copyright 2020 Mountain Fog, Inc.
Philter is a registered trademark of Mountain Fog, Inc.
