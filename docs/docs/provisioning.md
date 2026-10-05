# Users and API Keys

Users own API keys and the policies, contexts, and other resources made with them. Users have no password: they authenticate with API keys. These calls create and manage users and their keys, for example to stand up a deployment in CI, in a marketplace image, or for a test harness.

Every call on this page requires an administrator's key, except `getCurrentUser`, along with the scope noted for each. Philter refuses both a missing scope and a non-administrator with an HTTP 403, raised as a `ClientException`. The message tells them apart: a missing scope names the scope (`This API key does not have the 'users:read' scope.`), and a non-administrator is told an administrator is required (`Listing users requires an administrator.`).

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

The email is optional. The role is `user` or `admin`, and defaults to `user`. The new user gets a default policy and context.

A missing or reserved username (`me`), or any other role, is rejected with an HTTP 400. A username already taken, by an active user or a deactivated one holding the name in reserve, returns an HTTP 409. Both surface as a `ClientException` carrying Philter's explanation.

## Reading users

Requires the `users:read` scope.

```java
import ai.philterd.philter.model.GetUsersResponse;
import ai.philterd.philter.model.User;

// Users sorted by username, including deactivated users. Paged with offset and limit.
GetUsersResponse page = client.getUsers(0, 100);
long total = page.getTotal();
for (User u : page.getUsers()) {
    System.out.println(u.getUsername() + " " + u.getRole() + " " + u.isActive());
}

User ci = client.getUser("ci");

// The user that owns the calling key. Does not require an administrator.
User me = client.getCurrentUser();
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

## Creating an API key

Requires the `api-keys:write` scope.

```java
import ai.philterd.philter.model.CreatedApiKeyResponse;
import java.util.List;

CreatedApiKeyResponse key = client.createApiKey("ci", List.of("redact", "policies:read"));

System.out.println(key.getApiKey());  // the only time this value is available
```

**Capture the value from the response.** Philter stores only the key's hash, so a value not read here cannot be recovered; the only remedy is to create another key.

At least one scope is required, and each must name a scope Philter defines. An unrecognized name is refused with an HTTP 400 rather than dropped, so a key is never silently narrower than the one that was asked for. The requested scopes must also be a subset of those the calling key holds: a key cannot grant a scope it does not itself carry, and trying returns an HTTP 403.

An HTTP 404 here means there is no active user with that username. A deactivated account counts as absent, though its username stays reserved and cannot be recreated. Philter checks the scopes before it resolves the user, so a caller who could not grant those scopes anyway learns nothing about whether the username exists.

## Auditing

Every change on this page is recorded in Philter's audit log. Creating a user, setting a role, deactivating, and reactivating record `user_created`, `user_role_changed`, `user_deactivated`, and `user_reactivated` events naming the calling administrator as the principal, with the calling API key in the details. An `api_key_created` event names the new key as the principal and the user it belongs to as the object, as a key made in the dashboard does; the administrator who asked for it is in the event's details, along with the scopes granted.
