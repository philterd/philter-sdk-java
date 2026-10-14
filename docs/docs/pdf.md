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

## Redacting a PDF held in memory

Each PDF method also takes the document as a `byte[]`, so a PDF that arrives as an upload can be redacted without first writing an unredacted copy of it to disk. The request is the same as with a `File`.

```java
// For example, the bytes of an uploaded file.
byte[] pdf = upload.getBytes();

BinaryFilterResponse response = client.filterToPdf("my-context", "default", "upload.pdf", pdf);

String documentId = client.filterAsync("my-context", "default", "upload.pdf", pdf);
```

## Submitting a document asynchronously

Both calls above wait for the redaction to finish, which ties up a connection for as long as the document takes. `filterAsync(...)` and `filterToPdfAsync(...)` submit the document and return the ID Philter assigned to it. Poll the document's status, then download the result.

```java
String documentId = client.filterAsync("my-context", "default", "report.pdf", new File("report.pdf"));

DocumentStatus status = client.getDocumentState(documentId);
String state = status.getStatus();

byte[] zip = client.getDocument(documentId);            // once the status is COMPLETE
```

The status is `PENDING`, `PROCESSING`, `COMPLETE`, or `FAILED`. Downloading a document whose redaction failed returns an HTTP 410, which the client raises as a `ClientException`; `status.getError()` says why.

The status also names the policy the document was submitted with, and so does each document from `listDocuments()`. Read that revision of the policy to see exactly what redacted it:

```java
String policy = client.getPolicyVersion(status.getPolicyName(), status.getPolicyVersion());
```

`getPolicyVersion()` is `null` only if Philter recorded no policy for the document. A policy's first revision is `0`.

To be notified when a redaction finishes instead of polling, set a [webhook](webhook.md).

## Sharing the queue fairly

Philter takes asynchronous documents from different accounts in turns, so one account's large batch does not hold up everyone else. An application that submits for many people under one API key can get the same behavior among those people by passing a fairness key as the last argument to `filterAsync(...)` or `filterToPdfAsync(...)`, with either a `File` or a `byte[]`. Documents queued with different keys take turns.

```java
// For example, an internal user or group ID.
String documentId = client.filterAsync("my-context", "default", "report.pdf", new File("report.pdf"), "group-42");
```

The key is opaque to Philter and is stored unencrypted with the queued document, so it must not contain personal data such as a name or email address. Use an internal ID instead. A key is 1 to 64 characters of letters, digits, `.`, `_`, `:` and `-`; Philter refuses any other value with an HTTP 400. Passing `null`, or calling the overloads without a key, sends no key; documents queued without a key take turns together, as if they shared one key. The synchronous `filter(...)` and `filterToPdf(...)` methods do not take one, since Philter refuses a key on a synchronous request.

Use `listDocuments()` to list the stored documents and `deleteDocument(documentId)` to remove one.
