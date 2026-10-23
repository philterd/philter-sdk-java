# Owners and Pagination

Philter scopes its data to the user whose API key made the request. Every call in this client therefore acts on your own policies, contexts, documents, holds, ledger entries, and lists by default.

## Acting on another user's data

Endpoints that accept an `owner` have an overload taking that owner's username. It is the last argument everywhere except the listings that take `ListOptions`, where it comes just before the options, and `reidentifyValues(owner, request)` and `reidentifyText(owner, request)`, where it comes first.

```java
// Your own policies.
Page<PolicySummary> mine = client.listPolicies(null, null);

// Another user's policies, as an administrator.
Page<PolicySummary> theirs = client.listPolicies("someone@example.com", null);

String policy = client.getPolicy("default", "someone@example.com");
```

Naming your own username always works. Reaching another user's data requires both an administrator API key and `ADMIN_CROSS_USER_ACCESS_ENABLED=true` on the deployment; without them Philter answers 404, which the client raises as a `ClientException`. The 404 is deliberate, so an owner name cannot be used to discover which users exist.

Passing `null` for an owner is the same as omitting it: the call acts on your own data.

`getApiKeys` is the exception: listing another user's API keys requires an administrator key, but not `ADMIN_CROSS_USER_ACCESS_ENABLED`. Changing and revoking keys take a key ID rather than an owner. See [Users and API Keys](provisioning.md).

## Listings: paging, sorting, search, and filters

Every listing returns a page of items and `getTotal()`, how many items it has across every page, counting only those that match the search and filters. Pass a `ListOptions` to choose the page, the order, a search, and the listing's filters; every setting is optional, and `null` options use Philter's defaults: the first 25 items in the listing's default order.

```java
import ai.philterd.philter.model.ListOptions;
import ai.philterd.philter.model.Page;

// The third page of 50 policies whose name contains "claims", newest first.
Page<PolicySummary> page = client.listPolicies(null,
        new ListOptions().offset(100).limit(50).sort("created").descending().search("claims"));
long total = page.getTotal();
for (PolicySummary policy : page.getItems()) {
    System.out.println(policy.getName());
}
```

- `offset(n)` and `limit(n)` choose the page: the limit defaults to 25 and is at most 100.
- `sort(field)` orders by one of the listing's fields, below. An unknown field is an HTTP 400 naming the fields it accepts, raised as a `ClientException` whose `getField()` is `sort`.
- `ascending()` and `descending()` set the order. Left out, the default sort keeps its default direction, and any other sort is ascending.
- `search(text)` keeps items whose searched field contains the text, ignoring case, matched as plain text. It is sent as `q`.
- The filters apply to one listing each: `role(...)` and `active(...)` to users, `status(...)` to documents, and `session(...)` to API keys.

| Method | Returns | `sort` (default first) | `search` matches | Filters |
|--------|---------|------------------------|------------------|---------|
| `listPolicies(owner, options)` | `Page<PolicySummary>` | `name`, `created`, `updated` | name | |
| `listPoliciesAcrossUsers(options)` | `Page<PolicySummary>` | `name`, `created`, `updated` | name | |
| `listManagedPolicies(options)` | `Page<PolicySummary>` | `name` | name | |
| `getDeletedPolicies(owner, options)` | `GetDeletedPoliciesResponse` | `name` | name | |
| `listPolicyVersions(name, owner, options)` | `Page<PolicyVersionSummary>` | `revision` (newest first) | | |
| `listContexts(owner, options)` | `GetContextsResponse` | `name`, `created` | name | |
| `listContextsAcrossUsers(options)` | `GetContextsAcrossUsersResponse` | `name`, `created` | name | |
| `listContextEntries(name, owner, options)` | `GetContextEntriesResponse` | `created` (newest first), `reads` | | |
| `listCustomLists(owner, options)` | `Page<CustomListSummary>` | `name` | name | |
| `listCustomListsAcrossUsers(options)` | `Page<CustomListSummary>` | `name` | name | |
| `listHolds(owner, options)` | `Page<LegalHoldResponse>` | `set` (newest first), `reference` | reference | |
| `listHoldsAcrossUsers(options)` | `Page<OwnedLegalHoldResponse>` | `set` (newest first), `reference` | reference | |
| `listDocuments(owner, options)` | `GetDocumentsResponse` | `submitted` (newest first), `fileName` | | `status` |
| `listLedgerChains(owner, options)` | `GetLedgerResponse` | `created` (newest first), `filename` | document ID or file name | |
| `listLedgerChainsAcrossUsers(options)` | `GetLedgerResponse` | `created` (newest first), `filename` | document ID or file name | |
| `getUsers(options)` | `GetUsersResponse` | `username`, `created` | username | `role`, `active` |
| `getApiKeys(owner, options)` | `GetApiKeysResponse` | `created` (oldest first) | | `session` |
| `getAuditLog(event, from, to, owner, options)` | `GetAuditLogResponse` | `timestamp` (newest first), order only | | |
| `getWebhookDeliveries(owner, options)` | `GetWebhookDeliveriesResponse` | `created` (newest first), order only | | |

Items with the same sort value keep a stable order, so paging does not skip or repeat them.

## Listing across all users

An administrator can list every user's policies, contexts, custom lists, ledger chains, and legal holds in one call, with the `...AcrossUsers(options)` methods above. Each item names its owner. The same two conditions apply as for `owner`: an administrator key and `ADMIN_CROSS_USER_ACCESS_ENABLED=true`, otherwise a 404 raised as a `ClientException`. Managed policies are not included in `listPoliciesAcrossUsers`.

```java
Page<PolicySummary> policies = client.listPoliciesAcrossUsers(new ListOptions().search("claims"));
Page<OwnedLegalHoldResponse> holds = client.listHoldsAcrossUsers(new ListOptions().limit(100));
```

## Offset and limit overloads

Some listings, such as `listContexts(owner, offset, limit)` and `getUsers(offset, limit)`, also have an overload that takes `offset` and `limit` directly. It returns the same page and total, without sorting or search.

`exportAuditLog` is paged with `offset` and `limit` too, but with its own defaults and a truncation flag; see [Audit Log](audit-log.md). `listRedactLists` is not a listing: it returns the two redact lists.
