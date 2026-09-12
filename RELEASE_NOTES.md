# Release Notes

Release notes for the Philter SDK for Java. Dates for tagged releases are taken from their git tags and
[GitHub releases](https://github.com/philterd/philter-sdk-java/releases).

## 2.1.0 (unreleased)

### API coverage

Reconciled the client against Philter's OpenAPI specification and the API controllers on `philterd/philter`
`main`. The client now implements every endpoint in the specification, and no endpoint it calls is absent
from it.

* Added `getSigningKey(String keyId)` for `GET /api/signing-key/{keyId}`, which returns a retained public
  signing key by ID.
* Added `deleteLedgerEntry(String documentId)` for `DELETE /api/ledger/{documentId}`.
* Added `purgeLedger(int olderThanDays)` for `DELETE /api/ledger`, which prunes completed chains older than
  the given number of days. Philter restricts both ledger deletions to administrators on deployments that
  set `LEDGER_DELETION_ENABLED=true`.
* **Removed `status()`.** Philter 4.0.0 standardized on `/api/health` and removed `/api/status`, so the call
  could only ever return an HTTP 404. Use `health()`, which returns the same `StatusResponse`.

### Parameter coverage

Every query parameter Philter defines is now reachable. Each affected method keeps its existing signature
and gained an overload with the optional parameters appended, so no existing call changes.

* `owner` can be passed to every endpoint that accepts it, for administrators acting on another user's
  data. Previously only `getPolicies`, `exportContextEntries`, `importContextEntries`, and `reidentify`
  exposed it and the rest sent nothing.
* `offset` and `limit` can be passed to `getPolicyVersions`, `getContexts`, `getContextEntries`,
  `getDocuments`, `getHolds`, and `getLedger`, which previously always took the server's first page.
* `filter(context, policy, filename, text)` and `explain(context, policy, filename, text)` record the
  source filename against the document.
* Added `filterToPdf(...)`, which returns a redacted PDF rather than a ZIP archive.
* Added `filterAsync(...)` and `filterToPdfAsync(...)`, which submit a PDF for asynchronous redaction and
  return the assigned document ID for use with `getDocumentStatus` and `getDocument`. The PDF endpoint
  defaults to asynchronous, so the waiting calls now send `async=false` explicitly.
* The text `filter` no longer sends `async`. Philter's text endpoint does not define that parameter and is
  always synchronous.
* Added the `AsyncFilterResponse` model for the 202 response body.

### Error reporting

* `ClientException` now carries Philter's response body after the status code, truncated at 512
  characters. A rejected request previously reported only `Unknown error: HTTP 400`, discarding the
  server's explanation of what was wrong. `UnauthorizedException` and `ServiceUnavailableException`
  keep their fixed messages.

### Documentation

Audited every page of the documentation site against the client and Philter's API controllers. Every code
sample now compiles against the SDK.

* Corrected `health()`: the status field is `"UP"`, not `"Healthy"`.
* Corrected the re-identification example: the strategy must be `CRYPTO_REPLACE` or `FPE_ENCRYPT_REPLACE`.
  The previous `"encryption"` would have been rejected with an HTTP 400.
* Added pages for legal holds, the always-redact and never-redact lists, the redaction ledger, and the
  `owner` and pagination parameters. Every public method on the client is now documented.
* Documented the asynchronous PDF flow, `filterToPdf`, `compilePolicy`, the context entry endpoints, and the
  document status values (`PENDING`, `PROCESSING`, `COMPLETE`, `FAILED`).
* Noted that `createContext` and `updateContext` treat an omitted flag as `false` rather than as
  "leave unchanged", in both the guide and the javadoc.

### HTTP client

* Replaced Retrofit and OkHttp with the JDK's `java.net.http.HttpClient`. The only remaining runtime
  dependencies are Gson and `commons-lang3`.
* Replaced `withOkHttpClientBuilder(OkHttpClient.Builder)` with `withHttpClientBuilder(HttpClient.Builder)`.
  The per-request timeout and the `Authorization` header are still applied to a supplied builder; the
  connect timeout is not.
* `withMaxIdleConnections(...)` and `withKeepAliveDurationMs(...)` are deprecated no-ops. The JDK client
  sizes its connection pool through the `jdk.httpclient.connectionPoolSize` and
  `jdk.httpclient.keepalive.timeout` system properties.
* Removed the `AbstractClient` base class and the internal `PhilterService` Retrofit interface. The
  `UNAUTHORIZED` and `SERVICE_UNAVAILABLE` constants now live on `PhilterClient`.
* Requests are pinned to HTTP/1.1 and follow redirects, matching the wire behavior of the OkHttp-based
  releases. The default client also honors the `http.proxyHost` / `https.proxyHost` system properties,
  which the JDK client otherwise ignores.
* An endpoint URL without a trailing slash is now accepted; Retrofit rejected it.
* Unit tests run against the JDK's `com.sun.net.httpserver.HttpServer` rather than OkHttp's
  `MockWebServer`.

## 2.0.0 (2026-06-12)

**Compatible with Philter 4.0.0.** This is a major release that updates the client for the Philter 4.0.0 API and
is not backward compatible with earlier versions of the client.

### API compatibility

* Updated the client for compatibility with the **Philter 4.0.0** API.
* Added `withApiKey(...)` to the client builder for `Authorization` header authentication. The value is sent
  verbatim on every request, so include any scheme prefix (for example `"Bearer "`) if your deployment requires it.
* Dropped the document ID request parameter from `filter` and `explain`; Philter now assigns the document ID and
  returns it via the `x-document-id` response header.
* The text `filter` request now forces synchronous processing so the filtered text is returned directly.
* Replaced the `status()` string response with the structured `StatusResponse` object and added the unauthenticated
  `health()` endpoint.
* Removed mTLS / SSL client-certificate support (`withSslConfiguration(...)`) and the `ayza` dependency. It may be
  reintroduced in a future release if needed.

### New functionality

* Added support for the full Philter 4.0.0 API surface:
  * Policies: versions, diff, rollback, and compilation.
  * Contexts and context entries (create, update, delete, export, import).
  * Documents (list, retrieve, delete, status).
  * Legal holds.
  * Redaction ledger.
  * Custom lists and redact lists.
  * Re-identification of redacted values.
* Removed alerts support (no longer part of the Philter API).

### Build and tooling

* Targets Java 11 bytecode (via `<release>11</release>`) for broad consumer compatibility, while building with a current JDK.
* Migrated artifact publishing to Maven Central.
* Upgraded OkHttp from 3.14.9 to 4.9.2 (#13).
* Updated dependencies: `commons-lang3` (#11), `commons-io` (#10), and `log4j-core` (#14, #15).
* Added mocked unit tests (OkHttp `MockWebServer`) covering the full client surface, plus env-gated live
  integration tests that run against a real Philter instance when `PHILTER_ENDPOINT` is configured.

## 1.5.0 (2025-03-19)

* Updated dependency versions.
* Bumped `commons-io` from 2.8.0 to 2.14.0 (#9).
* Now available from Maven Central.

## 1.4.0 (2024-07-11)

* Changed the `/api/status` response format.
* Renamed filter profiles to policies.

## 1.3.1 (2023-10-17)

* Changed the group and package names from `com.mtnfog` to `ai.philterd`.

## 1.3.0 (2021-02-25)

* Added support for SSL client authentication.
* Added support for filtering PDF documents.
* Removed token-based API authentication.
* Removed the models client.

## 1.2.0 (2020-06-16)

* Changed the artifact name to `philter-sdk-java`.
* Added an option for API authentication support.
* Added `salt` to `Span` for when the `HASH_SHA256_REPLACE` filter strategy is applied by Philter.
* Added alerts retrieval and deletion to the client.
* Added the models client.

## 1.1.0 (2020-02-24)

* Split the SDKs into separate projects.
* Various changes and fixes.

## 1.0.0 (2020-04-07)

* Initial release.
