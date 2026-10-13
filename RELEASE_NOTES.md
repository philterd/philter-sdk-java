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
* Removed `status()` (use `health()`) and alerts. `withSslConfiguration(...)` is replaced by
  `withClientCertificate(...)` and `withTrustStore(...)`, which use only the JDK.
* The JDK's `java.net.http.HttpClient` replaces Retrofit and OkHttp. `withHttpClientBuilder(HttpClient.Builder)`
  replaces `withOkHttpClientBuilder(...)` and does not get the connect timeout. `withMaxIdleConnections(...)`
  and `withKeepAliveDurationMs(...)` are no-ops; use the `jdk.httpclient.connectionPoolSize` and
  `jdk.httpclient.keepalive.timeout` system properties. `AbstractClient` is removed, and `UNAUTHORIZED` and
  `SERVICE_UNAVAILABLE` moved to `PhilterClient`.
* Listings read Philter's listing envelope (philterd/philter#160), so the policy, version, hold, and custom list
  listings need a Philter that includes it; the others read an older response with a `total` of 0.
  `/api/policies` (own, all users, managed, and deleted), `/versions`, `/api/holds`, and `/api/lists` now return
  an object with the items and `total`, not a bare array; `/api/contexts` and `/api/documents` add `total`.
  Custom lists are paged. The methods that returned a plain `List` (`getPolicies`, `getPoliciesAcrossUsers`,
  `listManagedPolicies(offset, limit)`, `getPolicyVersions`, `getHolds`, `getHoldsAcrossUsers`,
  `listCustomLists(owner)`, and `listCustomListsAcrossUsers(offset, limit)`) read the new shape and still return
  one page's items, and are deprecated; `listCustomLists(owner)` still returns every list, reading each page.
  The deprecated `String` methods, such as `getLists` and `getContexts`, return Philter's new body as it is.
* `savePolicy` and `replacePolicy` return the policy's new entity tag instead of `void`. Calls that ignore the
  result still compile, but code compiled against an earlier 2.0.0 snapshot must be recompiled.

### Philter 4.0.0 API coverage

The client implements every operation in Philter's OpenAPI specification and every optional query parameter. New
parameters arrive as overloads, so existing signatures are kept. `owner` overloads let an administrator act on
another user's data. Every listing has an overload taking `ListOptions` (offset, limit, `sort`, `order`,
`search`, and the users, documents, and API keys filters) and returns its items with `total`, as a `Page<T>` or
the listing's response type.

* **Filtering:** redact PDFs with `filterToPdf`, or asynchronously with `filterAsync` and `filterToPdfAsync`
  and then `getDocumentState` and `getDocument`, from a `File` or a `byte[]`. The asynchronous methods take an
  optional fairness key (`fairness_key`, philterd/philter#106), so documents one API key submits for different
  people take turns in Philter's queue. `filter` and `explain` take an optional filename and can return a signed
  response (`X-Philter-Signature`).
* **Policies:** versions (each with its `author`), diffs (`diffPolicy`, and `diffPolicy(name)` compares the two
  most recent revisions), rollback, deleted policies (`getDeletedPolicies`), templates (`getPolicyTemplate`),
  PhiSQL compilation (`compilePhiSql`), descriptions and notes, managed policies (`listManagedPolicies`), and
  `copyPolicy`. Writes to a managed policy are a 409 `policy_managed`, and deleting `default` a 409
  `policy_default`. A description or notes given to `savePolicy` or `replacePolicy` are written with the policy
  in one request. `getPolicyWithETag` and `PolicyDetails.getEtag()` give a policy's entity tag, and
  `replacePolicy`, `setPolicyDetails`, and `rollbackPolicy` take it as an expected tag, so a change made against
  an older read is refused with `policy_changed` instead of overwriting someone else's.
* **Contexts:** settings, including the disambiguation scope (`DisambiguationScope`); per-filter-type counts;
  and entry listing, export, and import. On `updateContext`, a `null` setting keeps its current value.
* **Documents, custom lists, redact lists, and re-identification.** `saveList` only creates (a name in use is a
  409 `list_exists`); `replaceList` replaces. `getRedactList` and `replaceRedactList` read and replace one
  redact list, with a revision check that refuses a write made against an older read (409 `redact_list_changed`,
  `ErrorReasons.REDACT_LIST_CHANGED`).
* **Redaction ledger and legal holds:** list, verify, export, and delete ledger chains (`deleteLedgerEntry`,
  `purgeLedger`). A chain Philter cannot check has a `getValidationError()` and `null` check results. An
  unreadable entry has a `getReadError()`, and exporting its chain is a 422 `entry_unreadable`. A `user` hold
  needs no `scopeValue`.
* **Users and sign-in:** user management, including `setUserEmail`; `getSignInOptions` (the password rules,
  empty when password sign-in is off), `signIn`, `completeSignIn` (MFA), and `signOut`; passwords, MFA
  enrollment, `revokeSessionKeys`, and `signOutOtherSessions`. `withClientAddress(Supplier<String>)` sends the
  end user's address as `X-Forwarded-For` on every request, so Philter rate-limits and audits per person.
  `signIn` and `completeSignIn` overloads also take the person's user agent, sent as the sign-in request's
  `User-Agent`, so each session shows the person's browser rather than the application's client.
* **API keys:** list, create, scope, and revoke keys, including session keys, each with the client address and
  user agent that signed in and whether it is the current key. `listApiKeyScopes` describes each scope.
* **Limits:** `getLimits` returns the limits and rules Philter enforces (request and page sizes, password, name,
  policy, list, context, webhook, legal hold, user, audit export, and session key rules) and what the caller may
  do, so a client need not hard-code them.
* **Administration:** admin settings, the webhook (with `testWebhook`, `getWebhookDeliveries`, and changing the
  URL while keeping the secret), the audit log and its CSV export (paged by cursor with `exportAuditLogAfter`
  and `getNextCursor()`, so a range that includes today returns each event once), signing-key rotation, and
  listings across all users. `AuditEvent.getUsername()` names the user who acted, and users have their
  `getId()`, which audit events refer to them by. `AuditEvent.getSource()` is `api` for an event a request
  caused or `system` for one Philter recorded on its own, and `getClientIpAddress()` is only ever the requesting
  client's address.
* **PII counts:** `getPiiCounts` reads the PII type counts Philter records, per context and UTC day, for
  Philter Diffuse or any similar reporting, filtered by date range and context, and `getAllPiiCounts` reads
  every page. `GetPiiCountsResponse.isRecordingEnabled()` tells "no data" from "not recording". Requires the
  `pii-counts:read` scope and an administrator.

Responses are typed models. Calls that returned raw JSON have typed alternatives, and the `String` versions are
deprecated: `listContexts`, `getContextDetails`, `listLedgerChains`, `getLedgerExport`, `compilePhiSql` (for
`compilePolicy`), `reidentifyValues` (for `reidentify`), `diffPolicy` (for `getPolicyDiff`), and
`getContextEntriesExport` and `importContextEntriesExport`, whose export imports with no JSON to handle (for
`exportContextEntries` and `importContextEntries`). `getManagedPolicies` is deprecated too. Policy JSON stays a
string, so it can be edited and saved as it is.

Some of this needs a Philter that includes the matching change: context, list, and hold names that can be used
in a request path (philterd/philter#140); session key listing (#141); managed policy descriptions (#142); policy
descriptions and notes in a request body, without which earlier clients fail (#144); unverifiable ledger chains
(#145); the `policy_managed` reason (#146); unreadable ledger entries (#147); `user` holds without a
`scopeValue` (#148); an audit event's source (#149); the disambiguation scope (#150); conditional policy edits
and a policy's details in the same request (#154); paging the audit log export by cursor (#155); reading and
replacing one redact list (#157); a reason and field on every error (#158); the listing envelopes, sorting, and
search (#160), which the policy, version, hold, and custom list listings need to parse at all; limits and
sign-in options (#159); audit event usernames and user ids (#161); policy templates (#164); webhook tests,
delivery history, and keeping the secret (#165); deleted policies and version authors (#166); changing a user's
email address, and the email rule on `createUser` (#168); session client details and signing out other sessions
(#169); request ids (#156); and PII counts (#174).

### Errors

* `ClientException` exposes `getStatusCode()`, `getErrorMessage()` (the body's `message`), and `getReason()`, a
  machine-readable value such as `context_exists`, so callers need not parse messages. `ErrorReasons` has a
  constant for every reason Philter gives, and `getField()` names the invalid parameter or body field on a `400`
  when Philter reports it.
* `SignInLockedException` and `SignInRateLimitedException` extend it and carry the seconds to wait.
  `UnauthorizedException` (401) and `ServiceUnavailableException` (503) carry the same status code, error
  message, reason, and request id as `ClientException`, and all three implement `PhilterError`, without either
  becoming a `ClientException`, so existing `catch` blocks keep working. A 401's reason tells an expired session
  (`session_expired`) from an unknown key (`invalid_credentials`) or a deactivated user (`user_deactivated`).
* Every exception built from a Philter response has `getRequestId()`, the response's `X-Request-Id`, and its
  message ends with the id, so a logged stack trace carries it. `getLastRequestId()` returns the id of the
  calling thread's most recent response, for calls that succeed. Philter records the same id on the audit
  events the request caused.

### Build and tooling

* Targets Java 11 bytecode. The only runtime dependencies are Gson and `commons-lang3`. Published to Maven
  Central, with snapshots from `main`.
* The default HTTP client uses HTTP/1.1, follows redirects, and honors the proxy system properties.
* TLS options on the builder, using only the JDK: `withClientCertificate` presents a client certificate for mutual
  TLS, `withTrustStore` trusts a private or self-signed server certificate, and `withSslContext` takes an
  `SSLContext` for anything else. Each keeps the default HTTP client's other settings, and hostname verification
  stays on. `PhilterTls.sslContext` builds the same `SSLContext` from keystores for a caller that supplies its own
  `HttpClient.Builder`.
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
