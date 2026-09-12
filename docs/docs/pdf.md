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

Pass `filterToPdf(...)` instead to receive a redacted PDF rather than a ZIP archive. The arguments are the same.

```java
BinaryFilterResponse response =
        client.filterToPdf("my-context", "default", "report.pdf", new File("report.pdf"));

Files.write(Path.of("report-redacted.pdf"), response.getContent());
```

## Submitting a document asynchronously

Both calls above wait for the redaction to finish, which ties up a connection for as long as the document takes. `filterAsync(...)` and `filterToPdfAsync(...)` submit the document and return the ID Philter assigned to it. Poll the document's status, then download the result.

```java
String documentId = client.filterAsync("my-context", "default", "report.pdf", new File("report.pdf"));

// JSON: {"status": "...", "documentId": "...", "effectiveConfigurationHash": "...", "error": "..."}
String status = client.getDocumentStatus(documentId);

byte[] zip = client.getDocument(documentId);            // once the status is COMPLETE
```

The status is `PENDING`, `PROCESSING`, `COMPLETE`, or `FAILED`. Downloading a document whose redaction failed returns an HTTP 410, which the client raises as a `ClientException`; the `error` field of the status response says why.

Use `getDocuments()` to list the stored documents and `deleteDocument(documentId)` to remove one.
