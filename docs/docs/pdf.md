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

## Different policies for different pages

A PDF that bundles different documents, such as a cover letter followed by medical records, can be redacted with a different policy for each set of pages in one request. Pass a list of `PagePolicy` assignments as the last argument to `filter(...)` or `filterToPdf(...)`, with either a `File` or a `byte[]`. Requires Philter 4.1.0 or later.

```java
import ai.philterd.philter.model.PagePolicy;
import java.util.List;

List<PagePolicy> pagePolicies = List.of(
        new PagePolicy("2-5,8", "records"),
        new PagePolicy("12-", "exhibits"));

BinaryFilterResponse response =
        client.filterToPdf("my-context", "default", "bundle.pdf", new File("bundle.pdf"), pagePolicies);

// For example, "2-5,8=records@3; 12-=exhibits@1"
String applied = response.getPagePolicies();
```

Pages are numbered from 1. A page set is single pages (`3`), ranges (`2-5`), and open-ended ranges (`12-`, page 12 to the end), separated by commas. A request can name up to 50 page sets. Each assignment is sent as its own `page_policy` parameter.

* **Unassigned pages** are redacted with the request's policy (`default` above).
* **Rendering.** The `config.pdf` settings of the request's policy, such as `dpi` and `redactionColor`, render the whole document.
* **Context.** One context spans the document, so where two policies both use the `CONTEXT` replacement scope, a value gets the same replacement on pages under either policy.
* **Evidence.** `getPagePolicies()` returns Philter's `X-Philter-Page-Policies` header: the policy and version that redacted each page set, as `pages=policy@version` separated by `; `. It is `null` on a response to a request without page policies. Each ledger entry is stamped with the policy of the page it was found on.
* **Validation.** A malformed page set, page sets that overlap, or a page set beyond the document's last page is refused with an HTTP 400, and a policy that does not exist with an HTTP 404, raised as a `ClientException` carrying Philter's message.

The asynchronous methods take page policies after the fairness key, which may be `null`, and return an `AsyncFilterResponse` with the document ID and the policy and version pinned for each page set when Philter accepted the document.

```java
import ai.philterd.philter.model.AsyncFilterResponse;

AsyncFilterResponse accepted =
        client.filterAsync("my-context", "default", "bundle.pdf", new File("bundle.pdf"), null, pagePolicies);

String documentId = accepted.getDocumentId();
String applied = accepted.getPagePolicies();
```

Philter validates the assignments before it queues the document, so an invalid one fails the submission rather than the redaction.

## Submitting a document asynchronously

Both calls above wait for the redaction to finish, which ties up a connection for as long as the document takes. `filterAsync(...)` and `filterToPdfAsync(...)` submit the document and return the ID Philter assigned to it. Poll the document's status, then download the result.

```java
String documentId = client.filterAsync("my-context", "default", "report.pdf", new File("report.pdf"));

DocumentStatus status = client.getDocumentState(documentId);
String state = status.getStatus();

byte[] zip = client.getDocument(documentId);            // once the status is COMPLETE
```

The status is `PENDING`, `PROCESSING`, `COMPLETE`, or `FAILED`. Downloading a document whose redaction failed returns an HTTP 410, which the client raises as a `ClientException`; `status.getError()` says why.

`status.getType()` says what the job produces, as does `getType()` on each document from `listDocuments()`. `redaction`, a redacted document, is the only type Philter 4.0 returns. It is a string, so a type a later Philter adds is still read.

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

## Leftover check

To request the policy's [leftover check](redacting-text.md#checking-what-is-left-leftover-check), pass `FilterOptions` as the last argument of `filter(...)` or `filterToPdf(...)` with a `byte[]` and page policies, or of `filterAsync(...)` or `filterToPdfAsync(...)` with a `byte[]`, a fairness key, and page policies. Pass `null` for page policies or a fairness key to send none. A synchronous response reports the result on `getLeftoverCheck()` and `isLeftoversFlagged()`.

An asynchronous document is checked when it is redacted, after Philter accepts it, so its result is on the status and download responses, not on the submission's. `getDocument(documentId)` returns only the bytes; `downloadDocument(documentId)` returns them with the result:

```java
FilterOptions options = FilterOptions.defaults().withLeftoverCheck(true);

String documentId = client.filterAsync("my-context", "default", "report.pdf", pdf, null, null, options)
        .getDocumentId();

DocumentStatus status = client.getDocumentState(documentId);   // the result, once the status is COMPLETE

DocumentDownload download = client.downloadDocument(documentId);
byte[] zip = download.getContent();
download.getLeftoverCheck().ifPresent(check -> System.out.println(check.getFindingCount()));
```

A PDF's redacted pages are flattened to images, so only pages kept as text can be checked. When the output has no readable text, the check is reported as not performed.

## Requests selected for review

`BinaryFilterResponse.isReviewSampled()` is `true` when the policy's review sampling rate selected a synchronous request for human spot review. See [Requests selected for review](redacting-text.md#requests-selected-for-review).

An asynchronous document is selected when Philter accepts it, so only the submission's response reports it; the status and download responses do not. `filterAsync(...)` and `filterToPdfAsync(...)` with a fairness key and page policies return an `AsyncFilterResponse` whose `isReviewSampled()` reports it. Pass `null` for either to send none:

```java
AsyncFilterResponse accepted =
        client.filterAsync("my-context", "default", "report.pdf", new File("report.pdf"), null, null);

if (accepted.isReviewSampled()) {
    // Keep accepted.getDocumentId() for a reviewer.
}
```
