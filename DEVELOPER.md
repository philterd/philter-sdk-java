# Developer Guide

This guide shows how to use the **Philter SDK for Java** to redact sensitive information (PII/PHI) from text
and documents with [Philter](https://www.philterd.ai). It targets the **Philter 4.0.0** API.

## Contents

- [Concepts](#concepts)
- [Adding the dependency](#adding-the-dependency)
- [Creating a client](#creating-a-client)
- [Redacting text](#redacting-text)
- [Inspecting what was redacted (explain)](#inspecting-what-was-redacted-explain)
- [Redacting PDF documents](#redacting-pdf-documents)
- [Policies: the redaction rules](#policies-the-redaction-rules)
- [Contexts](#contexts)
- [Re-identifying redacted values](#re-identifying-redacted-values)
- [Custom lists](#custom-lists)
- [Checking server health](#checking-server-health)
- [Error handling](#error-handling)
- [End-to-end example](#end-to-end-example)

## Concepts

- **Policy** — A named set of rules that tells Philter which entity types to find (names, SSNs, email
  addresses, dates, etc.) and what *filter strategy* to apply to each (redact, replace, mask, encrypt, hash, …).
  Every filter request names the policy to apply; Philter ships a policy named `default`. Policies are authored
  as JSON; this client treats the policy body as an opaque JSON string.
- **Context** — An arbitrary label used to group requests (for example, by tenant or job). It is passed on each
  filter request and is echoed back on the response.
- **Document ID** — Philter assigns an identifier to each filtered document and returns it in the
  `x-document-id` response header; the client exposes it on the response object.

## Adding the dependency

Releases are published to [Maven Central](https://central.sonatype.com/artifact/ai.philterd/philter-sdk-java):

```xml
<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>philter-sdk-java</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Creating a client

Clients are created with the `PhilterClientBuilder`. Only the endpoint is required. Philter 4.0.0 expects an
`Authorization` header on nearly every endpoint, so supply your API key with `withApiKey(...)`. The value is sent
verbatim, so include any scheme prefix (for example `"Bearer "`) if your deployment requires it.

```java
import ai.philterd.philter.PhilterClient;

PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://localhost:8080")
        .withApiKey("your-api-key")
        .build();
```

A `PhilterClient` is thread-safe and reusable — create one and share it for the lifetime of your application.
Connection settings can be tuned on the builder:

```java
PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://localhost:8080")
        .withApiKey("your-api-key")
        .withTimeout(60)                 // connect/read/write timeout, seconds
        .withMaxIdleConnections(20)
        .withKeepAliveDurationMs(30000)
        .build();
```

For full control over the HTTP stack (proxies, custom TLS, logging interceptors, etc.) supply your own
OkHttp builder. When you do, the timeout/pool settings above are not applied — configure them on your builder:

```java
import okhttp3.OkHttpClient;

OkHttpClient.Builder http = new OkHttpClient.Builder();
// ...configure timeouts, interceptors, TLS, etc...

PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://localhost:8080")
        .withApiKey("your-api-key")
        .withOkHttpClientBuilder(http)
        .build();
```

## Redacting text

`filter(context, policyName, text)` sends text to Philter and returns the redacted text. This is the core
redaction call.

```java
import ai.philterd.philter.model.FilterResponse;

FilterResponse response = client.filter("my-context", "default",
        "His name is George Washington and his SSN is 123-45-6789.");

String redacted = response.getFilteredText();
// e.g. "His name is {{{REDACTED-person}}} and his SSN is {{{REDACTED-ssn}}}."
// (the exact replacement depends on the policy's filter strategies)

String documentId = response.getDocumentId();  // assigned by Philter
String context = response.getContext();         // echoes "my-context"
```

The replacement text (redaction tokens, masking characters, encrypted values, etc.) is entirely determined by
the policy you name — see [Policies](#policies-the-redaction-rules).

## Inspecting what was redacted (explain)

When you need to know *what* Philter found and where, use `explain(...)`. It returns the redacted text plus a
structured explanation listing each span that was identified.

```java
import ai.philterd.philter.model.ExplainResponse;
import ai.philterd.philter.model.Span;

ExplainResponse response = client.explain("my-context", "default",
        "His name is George Washington and his SSN is 123-45-6789.");

System.out.println("Redacted: " + response.getFilteredText());

for (Span span : response.getExplanation().getAppliedSpans()) {
    System.out.printf("Found %s \"%s\" at [%d, %d) -> replaced with \"%s\" (confidence %.2f)%n",
            span.getFilterType(),
            span.getText(),
            span.getCharacterStart(),
            span.getCharacterEnd(),
            span.getReplacement(),
            span.getConfidence());
}

// Spans that matched but were ignored (e.g. by ignore lists) are available too:
response.getExplanation().getIgnoredSpans().forEach(s ->
        System.out.println("Ignored: " + s.getText()));
```

## Redacting PDF documents

`filter(context, policyName, filename, file)` sends a PDF to Philter and returns the redacted result as a ZIP
archive (the raw bytes are on the response).

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

## Policies: the redaction rules

A policy defines which entity types are detected and how each is redacted. Manage them with the policy methods.
Policy bodies are JSON strings whose schema is defined by Philter; refer to the
[Philter API specification](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/openapi.json)
for the policy schema.

```java
import java.util.List;

// List the available policy names.
List<String> policies = client.getPolicies();

// Retrieve a policy's JSON.
String policyJson = client.getPolicy("default");

// Create or overwrite a policy. The body is the policy JSON.
client.savePolicy("my-policy", policyJson);

// Apply your policy when filtering.
client.filter("my-context", "my-policy", "Some sensitive text...");

// Delete a policy.
client.deletePolicy("my-policy");
```

Philter keeps a revision history for each policy:

```java
import ai.philterd.philter.model.PolicyVersionSummary;
import ai.philterd.philter.model.PolicyRollbackResponse;

for (PolicyVersionSummary v : client.getPolicyVersions("my-policy")) {
    System.out.println("revision " + v.getRevision() + " @ " + v.getCapturedTimestamp());
}

String revisionJson = client.getPolicyVersion("my-policy", 2);   // a specific revision
String diff = client.getPolicyDiff("my-policy", 1, 2);            // diff between revisions

PolicyRollbackResponse rollback = client.rollbackPolicy("my-policy", 1);
System.out.println("rolled back to revision " + rollback.getRevision());
```

## Contexts

Contexts group related requests under an arbitrary label and can enable features such as entity-type
disambiguation and the redaction ledger.

```java
import ai.philterd.philter.model.GenericResponse;

// Create a context with entity-type disambiguation and the ledger enabled.
GenericResponse created = client.createContext("tenant-a", true, true);

// Then pass the context name on filter/explain calls.
client.filter("tenant-a", "default", "Some text...");

// Update or delete the context.
client.updateContext("tenant-a", false, true);
client.deleteContext("tenant-a");
```

## Re-identifying redacted values

When a policy uses a reversible strategy (such as encryption), previously redacted values can be re-identified.

```java
import ai.philterd.philter.model.ReidentifyRequest;
import java.util.List;

ReidentifyRequest request = new ReidentifyRequest();
request.setPolicyName("my-policy");
request.setReason("authorized support investigation");
request.setStrategy("encryption");
request.setValues(List.of("<redacted-or-tokenized-value>"));

String result = client.reidentify("owner-id", request);
```

## Custom lists

Custom lists let a policy match (or ignore) an explicit set of terms.

```java
import ai.philterd.philter.model.GetListsResponse;
import java.util.List;

// Save a list of terms.
client.saveList("blocked-terms", "Terms to always redact", List.of("Acme", "Project X"));

// Retrieve a list's values.
GetListsResponse list = client.getList("blocked-terms");
System.out.println(list.getLists());

// Delete a list.
client.deleteList("blocked-terms");
```

## Checking server health

`health()` does not require authentication and is useful for readiness checks.

```java
import ai.philterd.philter.model.StatusResponse;

StatusResponse status = client.health();
System.out.println(status.getStatus());             // e.g. "Healthy"
System.out.println(status.getApplicationVersion()); // e.g. "4.0.0"
```

## Error handling

Every operation declares a checked `IOException`, thrown when the request cannot be executed (network failure,
timeout, etc.). In addition, non-successful HTTP responses are mapped to unchecked exceptions:

| HTTP status | Exception |
|-------------|-----------|
| 401         | `UnauthorizedException` |
| 503         | `ServiceUnavailableException` |
| any other non-2xx | `ClientException` |

```java
import ai.philterd.philter.model.FilterResponse;
import ai.philterd.philter.model.exceptions.ServiceUnavailableException;
import ai.philterd.philter.model.exceptions.UnauthorizedException;
import ai.philterd.philter.model.exceptions.ClientException;
import java.io.IOException;

try {
    FilterResponse response = client.filter("my-context", "default", "Some text...");
    System.out.println(response.getFilteredText());
} catch (UnauthorizedException e) {
    // Bad or missing API key.
} catch (ServiceUnavailableException e) {
    // Philter is temporarily unavailable; consider retrying with backoff.
} catch (ClientException e) {
    // Other non-successful response.
} catch (IOException e) {
    // The request could not be completed.
}
```

## End-to-end example

```java
import ai.philterd.philter.PhilterClient;
import ai.philterd.philter.model.ExplainResponse;
import ai.philterd.philter.model.FilterResponse;
import ai.philterd.philter.model.Span;

public class RedactionExample {

    public static void main(String[] args) throws Exception {

        PhilterClient client = new PhilterClient.PhilterClientBuilder()
                .withEndpoint("https://localhost:8080")
                .withApiKey("your-api-key")
                .build();

        final String text = "Patient George Washington (SSN 123-45-6789) was seen on 03/14.";

        // 1. Redact.
        FilterResponse filtered = client.filter("clinic", "default", text);
        System.out.println("Redacted: " + filtered.getFilteredText());
        System.out.println("Document ID: " + filtered.getDocumentId());

        // 2. Explain what was found.
        ExplainResponse explained = client.explain("clinic", "default", text);
        for (Span span : explained.getExplanation().getAppliedSpans()) {
            System.out.printf("  %-12s %-20s -> %s%n",
                    span.getFilterType(), span.getText(), span.getReplacement());
        }
    }

}
```
