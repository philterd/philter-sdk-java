# Philter Java Client

This project is a Java client for Philter's REST API.

# Usage

Clone and build this project:

```
git clone https://github.com/mtnfog/philter-java-client.git
cd philter-java-client
mvn clean install
```

Add the dependency to your project:

```
<dependency>
  <groupId>com.mtnfog</groupId>
  <artifactId>philter-api-client</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

To filter text:

```
PhilterClient client = new PhilterClient("https://127.0.0.1:8080/api", false);
String filtered = client.filter(text);
```

# License

Licensed under the Apache License, version 2.0.
