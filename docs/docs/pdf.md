# Redacting PDF Documents

`filter(context, policyName, filename, file)` sends a PDF to Philter and returns the redacted result as a ZIP archive (the raw bytes are on the response).

```java
import ai.philterd.philter.model.BinaryFilterResponse;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

BinaryFilterResponse response =
        client.filter("my-context", "default", "report.pdf", new File("report.pdf"));

byte[] zip = response.getContent();
Files.write(Path.of("report-redacted.zip"), zip);
```
