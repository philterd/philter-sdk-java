# Release Notes

Release notes for the Philter SDK for Java. Dates for tagged releases are taken from their git tags and
[GitHub releases](https://github.com/philterd/philter-sdk-java/releases).

## 2.0.0-SNAPSHOT (unreleased)

**Compatible with Philter 4.0.0.** A major version, not backward compatible with 1.x. Not yet released; changes
are relative to 1.5.0.

### Breaking changes

* Authenticate with `withApiKey(...)`, which sends its value verbatim in the `Authorization` header, so include
  any scheme prefix such as `"Bearer "`.
* `filter` and `explain` no longer take a document ID. Philter assigns it and returns it in the
  `x-document-id` header.
* Removed `status()`, since Philter 4.0.0 removed `/api/status`. Use `health()`, which returns a
  `StatusResponse`.
* Removed mTLS client-certificate support (`withSslConfiguration(...)`) and alerts, which Philter no longer
  has.
* Replaced Retrofit and OkHttp with the JDK's `java.net.http.HttpClient`. `withOkHttpClientBuilder(...)` is
  replaced by `withHttpClientBuilder(HttpClient.Builder)`, which still gets the per-request timeout and the
  `Authorization` header but not the connect timeout. `withMaxIdleConnections(...)` and
  `withKeepAliveDurationMs(...)` are deprecated no-ops; use the `jdk.httpclient.connectionPoolSize` and
  `jdk.httpclient.keepalive.timeout` system properties. `AbstractClient` is removed, and the `UNAUTHORIZED`
  and `SERVICE_UNAVAILABLE` constants moved to `PhilterClient`.

### Philter 4.0.0 API coverage

The client implements every operation in Philter's OpenAPI specification, and every optional query parameter
can be supplied. Methods that gained parameters keep their signatures and gained overloads.

* **Filtering:** `filterToPdf` returns a redacted PDF; `filterAsync` and `filterToPdfAsync` submit a PDF for
  asynchronous redaction and return its document ID for `getDocumentStatus` and `getDocument`. `filter` and
  `explain` take an optional filename, and can ask for a signed response: `FilterResponse` and
  `ExplainResponse` return the `X-Philter-Signature` JWT, and `ExplainResponse` keeps the body it covers.
* **Policies:** versions, diffs, rollback, and PhiSQL compilation; descriptions and notes (`getPolicyDetails`,
  `setPolicyDetails`, and a `savePolicy` overload); managed policies (`getManagedPolicies`); and `copyPolicy`.
* **Contexts, documents, legal holds, the redaction ledger, custom lists, redact lists, and
  re-identification**, including context entry export and import, `getContext`'s per-filter-type counts,
  and ledger deletion (`deleteLedgerEntry`, `purgeLedger`).
* **Users:** `getUsers`, `getUser`, `getCurrentUser`, `createUser` (optionally with a password),
  `setUserRole`, `deactivateUser`, and `reactivateUser`.
* **Sign-in:** `signIn` returns a session key or an MFA challenge for `completeSignIn`, and `signOut` revokes
  the session key. Passwords (`changePassword`, `setPassword`), MFA (`startMfaEnrollment`,
  `confirmMfaEnrollment`, `removeMfaEnrollment`, `removeUserMfa`, `unlockUserMfa`), and
  `revokeSessionKeys`.
* **API keys:** `getApiKeys`, `createApiKey` for the caller or another user, `setApiKeyScopes`, and
  `revokeApiKey`. A key's value is returned only when it is created.
* **Administration:** `getAdminSettings` and `updateAdminSettings`; `getWebhook`, `setWebhook`, and
  `removeWebhook`; `getAuditLog` and `exportAuditLog` (CSV); `regenerateSigningKey`; and listings of
  policies, contexts, lists, ledger chains, and holds across all users.
* `owner` overloads let an administrator act on another user's data, and paged calls take `offset` and
  `limit`.

Responses are typed models, such as `User`, `ApiKey`, `PolicyDetails`, `AuditEvent`, and `SignInResponse`.
The calls that returned raw JSON have typed alternatives, and the `String` versions are deprecated:
`listContexts`, `listContextsAcrossUsers`, `getContextDetails`, `listContextEntries`, `listCustomLists`,
`listCustomListsAcrossUsers`, `listRedactLists`, `listLedgerChains`, `listLedgerChainsAcrossUsers`,
`getLedgerChain`, `getLedgerExport`, `verifyLedgerChain`, `listDocuments`, `getDocumentState`, and
`getSigningKeyDetails`. `createRedactList` and `updateRedactList` take a `RedactListsRequest`. Policy JSON,
policy diffs, and context exports stay strings.

### Errors

* `ClientException` carries Philter's response body after the status code, truncated at 512 characters, and
  exposes the status with `getStatusCode()`, the body's `message` field with `getErrorMessage()`, read
  from the whole body, and its machine-readable `reason` with `getReason()`, such as `context_limit_reached`
  or `context_exists` from `createContext`, so callers can act on an error without parsing its message.
* `UnauthorizedException` carries Philter's message.
* A sign-in for a locked username raises `SignInLockedException`, and one over the rate limit
  `SignInRateLimitedException`. Both extend `ClientException` and carry the seconds to wait.

### Build and tooling

* Targets Java 11 bytecode. The only runtime dependencies are Gson and `commons-lang3`.
* Publishes to Maven Central; snapshots are published from `main`.
* The default HTTP client uses HTTP/1.1, follows redirects, and honors the `http.proxyHost` and
  `https.proxyHost` system properties; a builder passed to `withHttpClientBuilder` is used as given. An
  endpoint URL without a trailing slash is accepted.
* Updated `commons-lang3` (#11) and `log4j-core` (#14, #15), now used only by the tests.
* Mocked unit tests cover the whole client, and live integration tests run against a real Philter when
  `PHILTER_ENDPOINT` is set.
* The documentation covers every public method, and every code sample compiles against the SDK.

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
