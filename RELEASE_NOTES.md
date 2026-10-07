# Release Notes

Release notes for the Philter SDK for Java. Dates for tagged releases are taken from their git tags and
[GitHub releases](https://github.com/philterd/philter-sdk-java/releases).

## 2.0.0-SNAPSHOT (unreleased)

**Compatible with Philter 4.0.0.** A major version, not backward compatible with 1.x. Not yet released; changes
are relative to 1.5.0.

### Breaking changes

* `withApiKey(...)` sends its value verbatim in the `Authorization` header, so include any scheme prefix such
  as `"Bearer "`.
* `filter` and `explain` no longer take a document ID. Philter returns it in the `x-document-id` header.
* `savePolicy(json)` is replaced by `savePolicy(name, json)`, which only creates: a name in use is a 409
  `policy_exists`. Overwrite with `replacePolicy`. `Policy(policyName)` is renamed `getPolicy(policyName)`.
* Removed `status()` (use `health()`), mTLS client certificates (`withSslConfiguration(...)`), and alerts.
* The JDK's `java.net.http.HttpClient` replaces Retrofit and OkHttp. `withHttpClientBuilder(HttpClient.Builder)`
  replaces `withOkHttpClientBuilder(...)` and does not get the connect timeout. `withMaxIdleConnections(...)`
  and `withKeepAliveDurationMs(...)` are no-ops; use the `jdk.httpclient.connectionPoolSize` and
  `jdk.httpclient.keepalive.timeout` system properties. `AbstractClient` is removed, and `UNAUTHORIZED` and
  `SERVICE_UNAVAILABLE` moved to `PhilterClient`.

### Philter 4.0.0 API coverage

The client implements every operation in Philter's OpenAPI specification and every optional query parameter.
New parameters arrive as overloads, so existing signatures are kept. `owner` overloads let an administrator
act on another user's data, and paged calls take `offset` and `limit`.

* **Filtering:** redact PDFs with `filterToPdf`, or asynchronously with `filterAsync` and `filterToPdfAsync`
  and then `getDocumentState` and `getDocument`, from a `File` or a `byte[]`. `filter` and `explain` take an
  optional filename and can return a signed response (`X-Philter-Signature`).
* **Policies:** versions, diffs, rollback, PhiSQL compilation, descriptions and notes, managed policies
  (`listManagedPolicies`), and `copyPolicy`. Writes to a managed policy are a 409 `policy_managed`, and
  deleting `default` a 409 `policy_default`. A description or notes given to `savePolicy` or `replacePolicy`
  are sent in a second, non-atomic request.
* **Contexts:** settings, including the disambiguation scope (`DisambiguationScope`); per-filter-type counts;
  and entry listing, export, and import. On `updateContext`, a `null` setting keeps its current value.
* **Documents, custom lists, redact lists, and re-identification.** `saveList` only creates (a name in use is
  a 409 `list_exists`); `replaceList` replaces.
* **Redaction ledger and legal holds:** list, verify, export, and delete ledger chains (`deleteLedgerEntry`,
  `purgeLedger`). A chain Philter cannot check has a `getValidationError()` and `null` check results. An
  unreadable entry has a `getReadError()`, and exporting its chain is a 422 `entry_unreadable`. A `user` hold
  needs no `scopeValue`.
* **Users and sign-in:** user management; `signIn`, `completeSignIn` (MFA), and `signOut`; passwords, MFA
  enrollment, and `revokeSessionKeys`. `withClientAddress(Supplier<String>)` sends the end user's address as
  `X-Forwarded-For` on every request, so Philter rate-limits and audits per person.
* **API keys:** list, create, scope, and revoke keys, including session keys. `listApiKeyScopes` describes
  each scope.
* **Administration:** admin settings, the webhook, the audit log and its CSV export, signing-key rotation, and
  listings across all users. `AuditEvent.getSource()` is `api` for an event a request caused or `system` for
  one Philter recorded on its own, and `getClientIpAddress()` is only ever the requesting client's address.

Responses are typed models. Calls that returned raw JSON have typed alternatives, such as `listContexts`,
`getContextDetails`, `listLedgerChains`, and `getLedgerExport`, and the `String` versions are deprecated, as
is `getManagedPolicies`. Policy JSON, policy diffs, and
context exports stay strings.

Some of this needs a Philter that includes the matching change: path-unsafe names, which `deleteContext`,
`deleteList`, and `deleteHold` can still remove (philterd/philter#140); session key listing (#141); managed
policy descriptions (#142); policy descriptions and notes in a request body, without which earlier clients
fail (#144); unverifiable ledger chains (#145); the `policy_managed` reason (#146); unreadable ledger entries
(#147); `user` holds without a `scopeValue` (#148); an audit event's source (#149); and the disambiguation
scope (#150).

### Errors

* `ClientException` exposes `getStatusCode()`, `getErrorMessage()` (the body's `message`), and `getReason()`,
  a machine-readable value such as `context_exists`, so callers need not parse messages.
* `SignInLockedException` and `SignInRateLimitedException` extend it and carry the seconds to wait.
  `UnauthorizedException` carries Philter's message.

### Build and tooling

* Targets Java 11 bytecode. The only runtime dependencies are Gson and `commons-lang3`. Published to Maven
  Central, with snapshots from `main`.
* The default HTTP client uses HTTP/1.1, follows redirects, and honors the proxy system properties.
* Mocked tests cover the whole client; integration tests run against a live Philter when `PHILTER_ENDPOINT`
  is set.

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
