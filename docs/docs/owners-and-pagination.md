# Owners and Pagination

Philter scopes its data to the user whose API key made the request. Every call in this client therefore acts on your own policies, contexts, documents, holds, ledger entries, and lists by default.

## Acting on another user's data

Endpoints that accept an `owner` have an overload taking that owner's username. It is the last argument everywhere except `reidentify(owner, request)`, where it comes first, and `importContextEntries(name, onConflict, owner, json)`, where it is third.

```java
// Your own policies.
List<String> mine = client.getPolicies();

// Another user's policies, as an administrator.
List<String> theirs = client.getPolicies("someone@example.com", null, null);

String policy = client.getPolicy("default", "someone@example.com");
```

Naming your own username always works. Reaching another user's data requires both an administrator API key and `ADMIN_CROSS_USER_ACCESS_ENABLED=true` on the deployment; without them Philter answers 404, which the client raises as a `ClientException`. The 404 is deliberate, so an owner name cannot be used to discover which users exist.

Passing `null` for an owner is the same as omitting it: the call acts on your own data.

## Listing across all users

An administrator can list every user's policies, contexts, custom lists, ledger chains, and legal holds in one call. Each item names its owner. The same two conditions apply as for `owner`: an administrator key and `ADMIN_CROSS_USER_ACCESS_ENABLED=true`, otherwise a 404 raised as a `ClientException`.

```java
// Typed: each item has getName() and getOwner().
List<OwnedName> policies = client.getPoliciesAcrossUsers();

// Typed: LegalHoldResponse plus getOwner().
List<OwnedLegalHoldResponse> holds = client.getHoldsAcrossUsers(0, 100);

// Raw JSON, as the per-user calls return. Each item carries an "owner" field.
String contexts = client.getContextsAcrossUsers();
String lists = client.getListsAcrossUsers();
String chains = client.getLedgerAcrossUsers();
```

All five are paged with `offset` and `limit` (default 25, at most 100), including `getListsAcrossUsers`, although the per-user `getLists` is not. Managed policies are not included in `getPoliciesAcrossUsers`, and `getLedgerAcrossUsers` cannot filter by query.

## Paging through results

Seven per-user calls are paged, taking `offset` and `limit` alongside `owner`: `getPolicies`, `getPolicyVersions`, `getContexts`, `getContextEntries`, `getDocuments`, `getHolds`, and `getLedger`. Philter defaults to an offset of 0 and a limit of 25, so the short form of each returns only the first 25 results.

```java
// The third page of 50 policies.
List<String> policies = client.getPolicies(null, 100, 50);
```

Pass `null` for either value to use the server's default. `getManagedPolicies(offset, limit)` is paged the same way, though it takes no `owner`. `exportAuditLog` is also paged with `offset` and `limit`, but with its own defaults and a truncation flag; see [Audit Log](audit-log.md). The other per-user collection calls, `getLists` and `getRedactLists`, are not paged and take only an `owner`.
