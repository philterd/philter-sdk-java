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

## Paging through results

Seven calls are paged, taking `offset` and `limit` alongside `owner`: `getPolicies`, `getPolicyVersions`, `getContexts`, `getContextEntries`, `getDocuments`, `getHolds`, and `getLedger`. Philter defaults to an offset of 0 and a limit of 25, so the short form of each returns only the first 25 results.

```java
// The third page of 50 policies.
List<String> policies = client.getPolicies(null, 100, 50);
```

Pass `null` for either value to use the server's default. The other collection calls, `getLists` and `getRedactLists`, are not paged and take only an `owner`.
