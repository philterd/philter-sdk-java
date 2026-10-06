# Sign-in, Passwords, and MFA

A user interface can sign a person in as their own Philter user, without holding an administrator key of its own. The interface sends the person's username and password, and Philter returns a short-lived session key for that user, which the interface uses for the person's requests. Philter stays the user store, the user's role and the key's scopes decide access, and the audit log names the person.

Password sign-in is disabled unless the deployment sets `PASSWORD_SIGN_IN_ENABLED=true`. While it is disabled, the sign-in calls fail with an HTTP 404, raised as a `ClientException`. A user can only sign in once they have a password; see [Passwords](#passwords).

## Signing in

`signIn` needs no API key. Its result is either a session key or, for a user enrolled in MFA, a challenge.

```java
import ai.philterd.philter.PhilterClient;
import ai.philterd.philter.model.SignInResponse;

SignInResponse result = client.signIn("jordan", password);

if (result.isMfaRequired()) {
    // Ask the person for a code from their authenticator app. The challenge expires in five minutes.
    result = client.completeSignIn(result.getChallenge(), code);
}

// A client for the person's own requests, using the session key.
PhilterClient session = new PhilterClient.PhilterClientBuilder()
        .withEndpoint("https://localhost:8080")
        .withApiKey("Bearer " + result.getApiKey())
        .build();
```

The session key holds every scope, with the user's role still deciding administrator access. It cannot create API keys. It expires after a period without a request (`getIdleExpiresAt()`, moved forward by each request) or at the end of its maximum lifetime (`getExpiresAt()`). A request with an expired or revoked key is refused with an `UnauthorizedException`, which a user interface should treat as the session having ended.

Two flags restrict what the key can do:

- `isPasswordChangeRequired()` - an administrator set the password, so the key can only change it with `changePassword` and sign out. Changing it revokes the key, and the person signs in again with the new password.
- `isMfaEnrollmentRequired()` - the deployment requires MFA and the user is not enrolled, so the key can only [enroll](#multi-factor-authentication) and sign out. Confirming enrollment revokes the key, and the person signs in again, with a code.

Any other request with a restricted key is refused with an HTTP 403, raised as a `ClientException` that says what to do first.

To sign out, call `signOut()` on the client holding the session key. It revokes that key. A long-lived key cannot revoke itself, so `signOut()` with one is an HTTP 409.

```java
session.signOut();
```

### Failed sign-ins

| Situation | Exception |
|-----------|-----------|
| Wrong password, unknown username, a user without a password, or a deactivated user | `UnauthorizedException`. Philter does not say which, so it does not reveal which usernames exist. |
| Wrong MFA code, or an unknown, used, or expired challenge | `UnauthorizedException`. The challenge is used up by any attempt, so sign in again with the password. |
| The user's MFA is locked after five consecutive bad codes | `ClientException` (HTTP 403), until an administrator unlocks it. |
| The username is locked after repeated failed sign-ins | `SignInLockedException`. Every sign-in for the username is refused, even with the right password, until the lock lapses. |
| The client address made too many sign-in requests in the last minute | `SignInRateLimitedException`. |

`SignInLockedException` and `SignInRateLimitedException` extend `SignInThrottledException`, which extends `ClientException`. Both carry `getRetryAfterSeconds()`, the most seconds to wait, and `getReason()`, `locked` or `rate_limited`.

```java
import ai.philterd.philter.model.exceptions.SignInLockedException;
import ai.philterd.philter.model.exceptions.SignInRateLimitedException;
import ai.philterd.philter.model.exceptions.UnauthorizedException;

try {
    SignInResponse result = client.signIn(username, password);
} catch (UnauthorizedException e) {
    // Invalid username or password.
} catch (SignInLockedException e) {
    // Too many failed sign-ins for this username; try again after e.getRetryAfterSeconds().
} catch (SignInRateLimitedException e) {
    // Too many sign-in requests from this address; try again after e.getRetryAfterSeconds().
}
```

### Signing in on behalf of a person

Philter rate-limits sign-ins by client address (`SIGN_IN_RATE_LIMIT_PER_MINUTE`) and records that address on `sign_in_succeeded` and `sign_in_failed` audit events. When an application signs people in through one client, every sign-in comes from the application's address, so its users share one rate-limit budget and the audit log cannot tell them apart. Pass each person's address instead:

```java
// The address of the person's connection to the application. If the application is itself behind a
// load balancer, use the client address it resolves from that balancer's header, not the balancer's.
String personAddress = httpRequest.getRemoteAddr();

SignInResponse result = client.signIn(username, password, personAddress);
if (result.isMfaRequired()) {
    result = client.completeSignIn(result.getChallenge(), code, personAddress);
}
```

The address is sent as `X-Forwarded-For`. Philter uses it only when the request comes from an address in its `TRUSTED_PROXIES`, which by default are the loopback, private, link-local, and IPv6 unique-local ranges; otherwise, and for a value that is not an IP address, it uses the address of the connection. A port is allowed and ignored. Pass an address the application determined itself, not one the person's browser supplied, such as their own `X-Forwarded-For`, which they can set to anything. A `null` or empty address, or one of only spaces, sends no header. Surrounding spaces are trimmed, and an address containing a comma, a control character such as a line break, a character outside ASCII, or a space within it is refused with an `IllegalArgumentException` before anything is sent.

## Passwords

A password is 16 characters to 72 bytes in UTF-8. Setting, changing, or resetting a password revokes the user's session keys; long-lived API keys are not affected.

```java
// A person changes their own password. Requires users:write; not an administrator.
client.changePassword(currentPassword, newPassword);

// An administrator sets or resets another user's password. The user must change it at next sign-in.
client.setPassword("jordan", temporaryPassword);

// An administrator creates a user with a password, which the user must change at next sign-in.
client.createUser("jordan", "jordan@example.com", "user", temporaryPassword);
```

`changePassword` needs the current password, so a stolen key cannot replace its own user's password unnoticed. A wrong current password is an HTTP 403, and a user without a password, whose first password an administrator sets, is an HTTP 409.

`setPassword` on the calling administrator's own user sets only the first password, with no change required. This is how the `admin` user gets a password: call it with the bootstrap API key. After that, the administrator changes it with `changePassword`.

`User.isPasswordSet()` and `User.isPasswordChangeRequired()` report a user's password state; the password and its hash are never returned.

## Multi-factor authentication

A user who signs in with a password can add TOTP multi-factor authentication, with an authenticator app. MFA is available only when an administrator turns on `mfaAvailable` in the [admin settings](admin-settings.md); `mfaRequired` makes every user who signs in enroll. Enrolling and removing your own enrollment require `users:write` but not an administrator. A user who is not an administrator cannot read the admin settings, so `getCurrentUser()` returns `isMfaAvailable()` and `isMfaRequired()` for deciding whether to offer enrollment.

```java
import ai.philterd.philter.model.MfaEnrollment;

// Start: show the URI as a QR code, or have the person type in the secret. Neither is returned again.
MfaEnrollment enrollment = client.startMfaEnrollment();
String qrCodeContent = enrollment.getOtpauthUri();

// Confirm with a code from the app. This revokes the user's session keys.
client.confirmMfaEnrollment(code);

// Remove your own enrollment. It takes a valid code, so a stolen key cannot turn MFA off.
client.removeMfaEnrollment(code);
```

After five consecutive bad codes the user's MFA is locked. An administrator unlocks it, or removes the enrollment for a person who has lost their device:

```java
client.unlockUserMfa("jordan");
client.removeUserMfa("jordan");
```

`User.isMfaEnabled()` and `User.isMfaLocked()` report a user's MFA state; the secret is never returned.

## Session keys

`getApiKeys` lists session keys alongside long-lived keys. `ApiKey.isSession()` marks them, and `getExpiresAt()`, `getIdleExpiresAt()`, and `getLastUsedAt()` give their expiry and last use; they are `null` for long-lived keys. Revoke one with `revokeApiKey`, or every session key a user holds, signing the person out everywhere, with `revokeSessionKeys`, which requires an administrator:

```java
int revoked = client.revokeSessionKeys("jordan");
```

Pass `session` to `getApiKeys` to list only one kind of key; the total then counts only those. `SignInResponse.getId()` is the session key's ID as the listing gives it, so a user interface can recognize its own key among the user's sessions:

```java
import ai.philterd.philter.model.ApiKey;
import ai.philterd.philter.model.GetApiKeysResponse;

// With the client built from the sign-in result above. true lists only session keys, false only
// long-lived keys, and null both.
GetApiKeysResponse sessions = session.getApiKeys(null, null, null, true);
for (ApiKey key : sessions.getApiKeys()) {
    boolean current = key.getId().equals(result.getId());
    System.out.println(key.getPrefix() + (current ? " (this session)" : ""));
}
```

`setApiKeyScopes` and `revokeApiKey` refuse the key making the request with an HTTP 409, so a session key cannot change its own scopes; it ends itself with `signOut()`.

Session keys are checked against the database on every request, so a revoked or expired session key is refused on every Philter instance at once.
