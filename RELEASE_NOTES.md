# Release Notes

Release notes for the Philter SDK for Java. Dates for tagged releases are taken from their git tags and
[GitHub releases](https://github.com/philterd/philter-sdk-java/releases).

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
