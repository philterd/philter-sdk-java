# Philter SDK for Java

The **Philter SDK for Java** enables Java developers to easily work with Philter. [Philter](https://www.mtnfog.com/products/philter/) identifies and manipulates sensitive information like Protected Health Information (PHI) and personally identifiable information (PII) from natural language text. 

[![Build Status](https://travis-ci.org/mtnfog/philter-sdk-java.svg?branch=master)](https://travis-ci.org/mtnfog/philter-sdk-java)

## Usage

Clone and build this project:

```
git clone https://github.com/mtnfog/philter-sdk-java.git
cd philter-sdk-java
mvn clean install
```

Add the dependency to your project:

```
<dependency>
  <groupId>com.mtnfog</groupId>
  <artifactId>philter-java-sdk</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

To filter text:

```
PhilterClient client = new PhilterClient("https://127.0.0.1:8080");
FilterResponse filterResponse = client.filter(text);
```

Snapshot dependencies are available in the Maven Central Snapshot Repository:

```
<repository>
  <id>snapshots</id>
  <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  <releases><enabled>false</enabled></releases>
  <snapshots><enabled>true</enabled></snapshots>
</repository>
```

## License

This project is licensed under the Apache Software License, version 2.0.

Copyright 2020 Mountain Fog, Inc.
