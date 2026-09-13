# Provisioning Users and API Keys

Philter creates users and API keys from its dashboard, where the caller has passed the login and whatever it requires, including MFA. The provisioning endpoints are the way to do the same thing without a person and a browser, for standing up a deployment in CI, in a marketplace image, or for a test harness.

They are opt-in. A deployment has them only when it sets `PROVISIONING_API_ENABLED=true` in its environment; where it does not, both calls fail with an HTTP 404 because the endpoints are not there. This cannot be turned on over the API or by writing the database: it is a property of the deployment, set when it is stood up.

Both calls also require the calling key to belong to an administrator, on top of the scope each needs.

## Creating a user

Requires the `users:write` scope.

```java
import ai.philterd.philter.model.CreatedUserResponse;

CreatedUserResponse user = client.createUser("ci", "ci@example.com", "a-sixteen-char-password");

System.out.println(user.getUsername());  // ci
System.out.println(user.getRole());      // user
```

The password must be at least 16 characters, matching the dashboard's minimum, so a password this call accepts is one the login accepts. The email is optional. The role is not a parameter: this endpoint only creates a non-administrator, and an administrator account is still made in the dashboard.

A missing username or a password that is too short is rejected with an HTTP 400. A username already taken, by an active account or a deactivated one holding the name in reserve, returns an HTTP 409. Both surface as a `ClientException` carrying Philter's explanation.

The new user gets a default policy and context, as one created in the dashboard does.

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

An HTTP 404 here means either that the deployment has not opted in or that there is no active user with that username. A deactivated account counts as absent, though its username stays reserved and cannot be recreated. Philter checks the scopes before it resolves the user, so a caller who could not grant those scopes anyway learns nothing about whether the username exists.

## Both are audited

Both creations are recorded in Philter's audit log, readable through `GET /api/audit`.

A `user_created` event names the calling administrator as the principal. An `api_key_created` event names the new key as the principal and the user it belongs to as the object, as a key made in the dashboard does; the administrator who asked for it is in the event's details, along with the scopes granted.
