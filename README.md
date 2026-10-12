# Philter SDK for Java

The **Philter SDK for Java** is an API client for [Philter](https://www.philterd.ai). Philter identifies and manipulates
sensitive information such as Protected Health Information (PHI) and personally identifiable information (PII) in
natural language text. Philter is built upon the open source PII/PHI detection
engine [Phileas](https://github.com/philterd/phileas).

Refer to the [Philter API specification](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/openapi.json)
for details on the available endpoints.

## Compatibility

As of version 2.0.0, this client targets the **Philter 4.0.0** API. Earlier versions of the client are not
compatible with Philter 4.0.0 and later.

| philter-sdk-java | Philter API | Branch |
|------------------|-------------|--------|
| 2.0.0 and later  | 4.x         | `main` |
| 1.x              | 1.x – 3.x   | `1.x`  |

Development for Philter 4.x happens on the `main` branch. The `1.x` branch supports Philter 3 and earlier.

The client requires Java 11 or later. Requests are made with the JDK's `java.net.http.HttpClient`, so the SDK adds
no third-party HTTP dependency to your application.

## Installation

Releases are available from [Maven Central](https://central.sonatype.com/artifact/ai.philterd/philter-sdk-java). Add the
dependency to your Maven configuration:

```
<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>philter-sdk-java</artifactId>
    <version>2.0.0</version>
</dependency>
```

To track the latest development build, depend on the current `-SNAPSHOT` version and add the Maven Central snapshot repository to your build (snapshots are not served from the default Maven Central repository):

```
<repositories>
    <repository>
        <id>central-portal-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>philter-sdk-java</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

Snapshots are development builds: they are mutable and are periodically pruned, so pin a release version for anything you need to reproduce.

## Example Usage

See the [documentation site](https://philterd.github.io/philter-sdk-java/) for a full, example-driven walkthrough of
redacting text and documents, managing policies, and the rest of the API.

With an available running instance of Philter, to filter text:

```
PhilterClient client = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://127.0.0.1:8080")
        .withApiKey("your-api-key")
        .build();

FilterResponse filterResponse = client.filter("context", "default", text);
```

To filter text with explanation:

```
ExplainResponse explainResponse = client.explain("context", "default", text);
```

Every Philter response carries a request id, which Philter also records on the audit events the request caused.
A failed call carries it on the exception (`getRequestId()`, and at the end of the message), and
`client.getLastRequestId()` returns it for the last call on the current thread. Log it, and use it to find the request
in the audit log:

```
try {
    client.replacePolicy("claims", policyJson);
} catch (ClientException e) {
    System.err.println(e.getErrorMessage() + " (request id " + e.getRequestId() + ")");
}
```

Philter 4.0.0 expects an `Authorization` header on nearly every endpoint. Provide its value with
`withApiKey(...)`; the value is sent verbatim, so include any scheme prefix (for example `"Bearer "`) if your
deployment requires it. The `health()` endpoint does not require authentication.

In addition to filtering, the client covers the full Philter 4.0.0 API: policies (including templates, versions and
their authors, diffs, rollbacks, deleted policies, descriptions and notes, managed policies, and copies), contexts,
documents, legal holds, the redaction ledger, the audit log and its CSV export, custom lists, redact lists,
re-identification, user management (including roles and email addresses), sign-in with passwords, session keys, and
MFA, webhooks, managing API keys, rotating the output signing key, admin settings, and administrator listings of
policies, contexts, custom lists, ledger chains, and legal holds across all users.

To create a policy, start from one of Philter's templates, which matches the schema Philter runs, and save it:

```
String template = client.getPolicyTemplate("default");
client.savePolicy("claims", template);
```

Deleting a policy keeps its revision history. `getDeletedPolicies` lists the deleted policies, and a deleted policy's
history is read by its name like a live one's. To restore one, save the revision you want under its name:

```
for (DeletedPolicy deleted : client.getDeletedPolicies().getPolicies()) {
    System.out.println(deleted.getName() + " (revision " + deleted.getLatestRevision() + ")");
}

client.savePolicy("claims-2025", client.getPolicyVersion("claims-2025", 4));
```

Read Philter's limits and rules instead of hard-coding them. `getLimits()` returns them with what the caller may do,
and `getSignInOptions()` returns the password rules before anyone signs in, empty when password sign-in is off:

```
Limits limits = client.getLimits();
int maxDescription = limits.getPolicies().getDescriptionMaxLength();

Optional<SignInOptions> options = client.getSignInOptions();
```

Replace one redact list without losing someone else's change: pass the revision you read, and a list changed since is
refused with a `ClientException` whose reason is `ErrorReasons.REDACT_LIST_CHANGED`, so you can read it again and
retry:

```
RedactListTerms current = client.getRedactList(RedactList.ALWAYS);
List<String> terms = new ArrayList<>(current.getTerms());   // the list is replaced, so start from what is there
terms.add("Project X");
client.replaceRedactList(RedactList.ALWAYS, terms, current.getRevision());
```

Listings return a page of items and the total, and take a `ListOptions` to page, sort, search, and filter:

```
Page<String> page = client.listPolicies(null, new ListOptions().limit(50).sort("created").descending().search("claims"));
System.out.println(page.getItems() + " of " + page.getTotal());
```

This client reads the listing shape Philter returns since philterd/philter#160: an object with the items and `total`.
`GET /api/policies`, `/api/holds`, `/api/lists`, and policy versions used to return bare arrays, so their listing methods
need a Philter that includes that change. The older methods that return a plain `List` still work and are deprecated.

Edit a policy without overwriting someone else's change: read it with its entity tag, and pass the tag back. If the
policy changed since, the write is refused with `ErrorReasons.POLICY_CHANGED`:

```
PolicyWithETag read = client.getPolicyWithETag("claims");
String newTag = client.replacePolicy("claims", edited, "Claims", null, read.getETag(), null);
```

A description or notes passed to `savePolicy` or `replacePolicy` is written with the policy in one request.

Every refusal carries a stable reason. Act on it with the `ErrorReasons` constants rather than on the message or the
status alone; `getField()` names the invalid field when Philter reports one:

```
try {
    client.savePolicy("claims", json);
} catch (ClientException e) {
    if (ErrorReasons.POLICY_EXISTS.equals(e.getReason())) {
        client.replacePolicy("claims", json);
    } else {
        throw e;
    }
}
```

Responses are typed. Compile PhiSQL with `compilePhiSql`, compare policy revisions with `diffPolicy`, re-identify
values with `reidentifyValues`, and copy a context's mappings with `getContextEntriesExport` and
`importContextEntriesExport`, without handling JSON:

```
CompilePolicyResponse compiled = client.compilePhiSql("POLICY ssn_only; REDACT SSN WITH MASK;");
client.savePolicy(compiled.getName(), compiled.getPolicyJson());

PolicyDiff diff = client.diffPolicy("ssn_only");
ImportContextEntriesResponse imported =
        client.importContextEntriesExport("tenant-b", client.getContextEntriesExport("tenant-a"));
```

The older `compilePolicy`, `getPolicyDiff`, `reidentify`, `exportContextEntries`, and `importContextEntries`, which
return JSON strings, still work and are deprecated.

Export the audit log as CSV a page at a time with `exportAuditLogAfter`, following each page's cursor, which returns
each event once even when the range includes today:

```
String cursor = null;
do {
    AuditLogExport page = client.exportAuditLogAfter(from, to, null, cursor, 1000);
    cursor = page.isTruncated() ? page.getNextCursor() : null;
} while (cursor != null);
```

A webhook delivers the result of each asynchronous redaction. `testWebhook` sends it a test event now and reports how
the receiver answered, and `getWebhookDeliveries` shows each delivery's status and last error:

```
WebhookTestResult test = client.testWebhook();
System.out.println(test.isDelivered() ? "delivered" : test.getStatusCode() + ": " + test.getError());

for (WebhookDelivery delivery : client.getWebhookDeliveries().getDeliveries()) {
    System.out.println(delivery.getEvent() + " " + delivery.getStatus() + " " + delivery.getLastError());
}
```

A person signed in with a session key can see their sessions, with the address and user agent each signed in from,
and sign out the others:

```
for (ApiKey key : client.getApiKeys(null, null, null, true).getApiKeys()) {
    String marker = key.isCurrent() ? " (this one)" : "";
    System.out.println(key.getUserAgent() + " from " + key.getClientAddress() + marker);
}
long revoked = client.signOutOtherSessions();
```

An application that signs people in on their behalf passes each person's address and user agent, so each session
shows the person's own browser rather than the application's client:

```
SignInResponse result = client.signIn(username, password, personAddress, personUserAgent);
```

An administrator sets or removes a user's email address with `setUserEmail`:

```
client.setUserEmail("jordan", "jordan@example.com");
client.setUserEmail("jordan", null); // removes it
```

An address must have one `@`, something on each side, a dot in the domain, and no whitespace, and be at most 254
characters. Philter refuses one that does not with an HTTP 400, thrown as a `ClientException`, both here and in
`createUser`.

## Testing

Unit tests run against a mocked HTTP server and execute on every build with no external dependencies.

Live integration tests (`PhilterClientTest`) run against a real Philter instance and are skipped unless
`PHILTER_ENDPOINT` is set. To run them:

```
export PHILTER_ENDPOINT=https://localhost:8080/
export PHILTER_API_KEY=your-api-key   # optional
export PHILTER_INSECURE=true          # optional, to trust self-signed certificates
mvn test
```

Additional optional variable: `PHILTER_PDF_FILE` (to enable the PDF filtering test).

## Release History

See [RELEASE_NOTES.md](RELEASE_NOTES.md) for the release history.

## License

This project is licensed under the Apache License, version 2.0.

Copyright 2026 Philterd, LLC.
Philter is a registered trademark of Philterd, LLC.
