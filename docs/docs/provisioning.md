# Users and API Keys

Users own API keys and the policies, contexts, and other resources made with them. Automation authenticates with long-lived API keys; a person can also sign in with a password through a user interface, which gets a session key (see [Sign-in, Passwords, and MFA](sign-in.md)). These calls create and manage users and their keys, for example to stand up a deployment in CI, in a marketplace image, or for a test harness.

Every user call on this page requires an administrator's key, except `getCurrentUser`, along with the scope noted for each. A user can manage its own API keys without an administrator; managing another user's requires one. Philter refuses both a missing scope and a non-administrator with an HTTP 403, raised as a `ClientException`. The message tells them apart: a missing scope names the scope (`This API key does not have the 'users:read' scope.`), and a non-administrator is told an administrator is required (`Listing users requires an administrator.`).

## Creating a user

Requires the `users:write` scope.

```java
import ai.philterd.philter.model.CreatedUserResponse;

CreatedUserResponse user = client.createUser("ci", "ci@example.com");

System.out.println(user.getUsername());  // ci
System.out.println(user.getRole());      // user

// An administrator.
CreatedUserResponse admin = client.createUser("ops", null, "admin");
```

The email is optional. The role is `user` or `admin`, and defaults to `user`. `createUser(username, email, role, password)` also gives the user a password, which they must change at next sign-in; without one, the user can only use API keys. See [Passwords](sign-in.md#passwords). The new user gets a default policy and context.

A missing or reserved username (`me`), or any other role, is rejected with an HTTP 400. A username already taken, by an active user or a deactivated one holding the name in reserve, returns an HTTP 409. Both surface as a `ClientException` carrying Philter's explanation.

## Reading users

Requires the `users:read` scope.

```java
import ai.philterd.philter.model.CurrentUser;
import ai.philterd.philter.model.GetUsersResponse;
import ai.philterd.philter.model.User;

// Users sorted by username, including deactivated users. Paged with offset and limit.
GetUsersResponse page = client.getUsers(0, 100);
long total = page.getTotal();
for (User u : page.getUsers()) {
    System.out.println(u.getUsername() + " " + u.getRole() + " " + u.isActive());
}

User ci = client.getUser("ci");

// The user that owns the calling key, with the deployment's MFA settings. Does not require an administrator.
CurrentUser me = client.getCurrentUser();
boolean offerMfa = me.isMfaAvailable() && !me.isMfaEnabled();
```

`getUsers` defaults to the first 25 users and returns at most 100 per page. `getUser` returns an HTTP 404 for a username that does not exist.

## Changing a user

Requires the `users:write` scope. Each call returns the updated `User`.

```java
client.setUserRole("ci", "admin");

// Its API keys stop working; the user and its data are kept.
client.deactivateUser("ci");

// Its API keys work again.
client.reactivateUser("ci");
```

A deactivated user keeps its username in reserve, so it cannot be recreated. Philter refuses, with an HTTP 409, demoting or deactivating the last active administrator, deactivating your own user, deactivating a user that is already deactivated, and reactivating one that is already active. An unknown username is an HTTP 404.

## Listing API keys

Requires the `api-keys:read` scope.

```java
import ai.philterd.philter.model.ApiKey;
import ai.philterd.philter.model.GetApiKeysResponse;

// The calling key's user's active keys, oldest first. Paged with offset and limit.
GetApiKeysResponse mine = client.getApiKeys();
for (ApiKey k : mine.getApiKeys()) {
    System.out.println(k.getId() + " " + k.getPrefix() + " " + k.getScopes() + " " + k.isBootstrap());
}

// Another user's keys. Requires an administrator.
GetApiKeysResponse theirs = client.getApiKeys("ci", 0, 100);

// Only long-lived keys, leaving out session keys: false. Only session keys: true.
GetApiKeysResponse longLived = client.getApiKeys(null, 0, 100, false);
```

Each key has its ID, a prefix to tell keys apart, its scopes, when it was created, and whether it is the bootstrap key seeded from `PHILTER_BOOTSTRAP_API_KEY`. Session keys, issued when a person signs in, are listed too, with their expiry; see [Session keys](sign-in.md#session-keys). The key itself is never returned after it is created. Listing another user's keys requires an administrator, but not `ADMIN_CROSS_USER_ACCESS_ENABLED`; an unknown username is an HTTP 404.

## Creating an API key

List the scopes a key can carry before choosing them, rather than hard-coding the names. Any valid key can list them, whatever its scopes, and the list includes any scope Philter adds later.

```java
import ai.philterd.philter.model.ApiKeyScopeDescription;

for (ApiKeyScopeDescription scope : client.listApiKeyScopes().getScopes()) {
    System.out.println(scope.getName() + ": " + scope.getDescription());
}
// redact: Redact text and documents, and explain redactions.
// contexts:read: List and read contexts and their entries, including exports.
// ...
```

Creating a key requires the `api-keys:write` scope.

```java
import ai.philterd.philter.model.CreatedApiKeyResponse;
import java.util.List;

// A key for the calling key's own user. Does not require an administrator.
CreatedApiKeyResponse mine = client.createApiKey(List.of("redact"));

// A key for another user. Requires an administrator.
CreatedApiKeyResponse key = client.createApiKey("ci", List.of("redact", "policies:read"));

System.out.println(key.getApiKey());  // the only time this value is available
System.out.println(key.getId());      // used to change its scopes or revoke it
```

**Capture the value from the response.** Philter stores only the key's hash, so a value not read here cannot be recovered; the only remedy is to create another key.

At least one scope is required, and each must name a scope Philter defines. An unrecognized name is refused with an HTTP 400 rather than dropped, so a key is never silently narrower than the one that was asked for. The requested scopes must also be a subset of those the calling key holds: a key cannot grant a scope it does not itself carry, and trying returns an HTTP 403.

For another user, an HTTP 404 means there is no active user with that username. A deactivated account counts as absent, though its username stays reserved and cannot be recreated. Philter checks the scopes before it resolves the user, so a caller who could not grant those scopes anyway learns nothing about whether the username exists.

## Changing a key's scopes and revoking a key

Requires the `api-keys:write` scope. Both take the key's ID. A user can change or revoke its own keys; an administrator can change or revoke any user's.

```java
// Replace the key's scopes. The key value does not change.
ApiKey changed = client.setApiKeyScopes(keyId, List.of("redact"));

// Revoke the key. It cannot be restored.
client.revokeApiKey(keyId);
```

The calling key cannot change or revoke a key holding a scope it does not hold (HTTP 403), and cannot change its own scopes, even to narrow them, or revoke itself (HTTP 409); use another key. A key the caller may not manage, including another user's key for a non-administrator, is an HTTP 404.

To rotate a key, create its replacement with `createApiKey(scopes)`, switch the integration to the new key, then revoke the old key using the new one.

Philter caches each resolved key for up to `API_KEY_CACHE_TTL_SECONDS` (60 by default). Revoking a key or changing its scopes takes effect at once on the Philter instance that handles the request and on every instance sharing its Valkey or Redis cache. Other nodes in a cluster that do not share the cache may accept the old key or scopes until their cache entry expires.

## Auditing

Every change on this page is recorded in Philter's audit log. Creating a user, setting a role, deactivating, and reactivating record `user_created`, `user_role_changed`, `user_deactivated`, and `user_reactivated` events naming the calling administrator as the principal, with the calling API key in the details. An `api_key_created` event names the new key as the principal and the user it belongs to as the object, as a key made in the dashboard does; the caller who asked for it is in the event's details, along with the scopes granted. Changing a key's scopes records `api_key_scopes_changed` with the scopes before and after, and revoking one records `api_key_deleted`.
