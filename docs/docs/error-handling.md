# Error Handling

Every operation declares a checked `IOException`, thrown when the request cannot be executed (network failure, timeout, and so on). In addition, non-successful HTTP responses are mapped to unchecked exceptions:

| HTTP status | Exception |
|-------------|-----------|
| 401         | `UnauthorizedException` |
| 429 from `signIn` or `completeSignIn` | `SignInLockedException` or `SignInRateLimitedException` |
| 503         | `ServiceUnavailableException` |
| any other non-2xx | `ClientException` |

Philter explains a rejected request in the response body, and the `ClientException` message carries it (truncated at 512 characters) after the status code, so a 400 or a 403 says what was actually wrong:

```
Unknown error: HTTP 400: {"message":"'strategy' must be CRYPTO_REPLACE or FPE_ENCRYPT_REPLACE."}
```

`UnauthorizedException` and `ServiceUnavailableException` carry what `ClientException` does: `getStatusCode()` (`401` or `503`), `getErrorMessage()`, `getReason()`, and `getRequestId()`. Their message is Philter's message, or `Unauthorized` or `Service unavailable` when the response has none, followed by the [request id](#request-ids) when the response has one; `getErrorMessage()` and `getReason()` are then `null`.

A 401's `getReason()` tells apart why the credentials were refused, so a user interface can, for example, send a person whose session ended back to sign in:

| `getReason()` | Meaning |
|---------------|---------|
| `invalid_credentials` | The key is missing, malformed, or unknown, or is a revoked long-lived key. |
| `session_expired` | The session key expired, was revoked, or signed out. Sign in again. |
| `user_deactivated` | The key's user is deactivated. |

```java
try {
    client.listPolicies(null, null);
} catch (UnauthorizedException e) {
    if (ErrorReasons.SESSION_EXPIRED.equals(e.getReason())) {
        // Show the sign-in page.
    }
}
```

Neither is a subclass of `ClientException`, so a `catch (ClientException e)` does not catch a 401 or a 503, and a multi-catch of `ClientException | ServiceUnavailableException` compiles. All three implement `PhilterError`, which has the four accessors, to handle any Philter error the same way:

```java
import ai.philterd.philter.model.exceptions.PhilterError;

try {
    client.listPolicies(null, null);
} catch (RuntimeException e) {
    if (e instanceof PhilterError) {
        PhilterError error = (PhilterError) e;
        log.warn("Philter refused the request: {} {} ({}, request id {})", error.getStatusCode(),
                error.getErrorMessage(), error.getReason(), error.getRequestId());
    }
    throw e;
}
```

`SignInLockedException` and `SignInRateLimitedException` are subclasses of `ClientException`, so a handler for `ClientException` still catches them. See [Failed sign-ins](sign-in.md#failed-sign-ins).

```java
import ai.philterd.philter.model.FilterResponse;
import ai.philterd.philter.model.exceptions.ServiceUnavailableException;
import ai.philterd.philter.model.exceptions.UnauthorizedException;
import ai.philterd.philter.model.exceptions.ClientException;
import java.io.IOException;

try {
    FilterResponse response = client.filter("my-context", "default", "Some text...");
    System.out.println(response.getFilteredText());
} catch (UnauthorizedException e) {
    // Bad or missing API key.
} catch (ServiceUnavailableException e) {
    // Philter is temporarily unavailable; consider retrying with backoff.
} catch (ClientException e) {
    // Other non-successful response.
} catch (IOException e) {
    // The request could not be completed.
}
```

## Acting on the reason

Every error Philter returns is a JSON object with a `message`, a `reason`, and, for some invalid requests, a `field`. `ClientException` exposes them as `getErrorMessage()`, `getReason()`, and `getField()`, alongside the HTTP status as `getStatusCode()`.

- `getReason()` is a stable, machine-readable code. Branch on it: it tells apart refusals that share a status, and it does not change when the wording does. `ErrorReasons` has a constant for every reason Philter gives. Philter gives one on every error: a specific reason where the refusal has one, and otherwise the one its status implies, such as `ErrorReasons.NOT_FOUND` for a `404`.
- `getErrorMessage()` is written for people, so show it rather than branching on it. It is read from the whole body, so it is complete even when the exception message was truncated.
- `getField()` names the parameter or body field Philter found invalid, such as `sort`, `password`, or `email`, on some `400` responses whose reason is `invalid_request`, so a form can mark the field. It is `null` otherwise.

All three are `null` for a response that did not come from Philter, such as a proxy's error page; fall back to the status then. Compare with `ErrorReasons.X.equals(e.getReason())`, which is safe when the reason is `null`.

```java
import ai.philterd.philter.model.exceptions.ErrorReasons;

try {
    client.changePassword(currentPassword, newPassword);
} catch (ClientException e) {
    if (ErrorReasons.WRONG_PASSWORD.equals(e.getReason())) {
        // The current password is not correct.
    } else if (ErrorReasons.INVALID_REQUEST.equals(e.getReason())) {
        // The new password was rejected; show the person why.
        System.out.println(e.getErrorMessage());
    } else if (ErrorReasons.CHANGED_CONCURRENTLY.equals(e.getReason())) {
        // Another request changed the password first; try again.
    } else {
        // Such as missing_scope, or a 409 conflict for a user with no password yet, whose first one an
        // administrator sets.
        throw e;
    }
}
```

The reasons for each call's own refusals are below; `ErrorReasons` documents every reason, including the general ones such as `missing_scope`, `admin_required`, `session_expired`, and `not_found`, and Philter's [Errors documentation](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/api.md#errors) lists them with their statuses.

| Call | Status | `getReason()` |
|------|--------|---------------|
| `createContext` | `409` | `context_exists`: the caller already has a context with that name, even at the limit. `context_limit_reached`: the caller already has the most contexts a user may have. |
| `savePolicy` | `409` | `policy_exists`: the owner already has a policy with that name. Replace it with `replacePolicy`. |
| `replacePolicy` | `409` | `policy_managed`: a managed policy cannot be replaced; copy it with `copyPolicy` and change the copy. `policy_changed`: the policy changed concurrently. Reload it and retry. |
| `deletePolicy` | `409` | `policy_default`: the `default` policy cannot be deleted. `policy_managed`: a managed policy cannot be deleted. |
| `copyPolicy` | `409` | `policy_exists`: the owner already has a policy with the new name. |
| `rollbackPolicy` | `409` | `policy_managed`: a managed policy cannot be rolled back. `policy_changed`: the policy changed concurrently. Reload it and retry. |
| `setPolicyDetails` | `409` | `policy_managed`: a managed policy's description and notes cannot be changed. |
| `saveList` | `409` | `list_exists`: the owner already has a list with that name. Replace it with `replaceList`. |
| `createHold` | `409` | `hold_exists`: a hold with that reference exists. `operation_in_progress`: an evidence or hold operation for the owner is active or needs recovery. |
| `deleteHold` | `409` | `operation_in_progress`, as for `createHold`. |
| `replaceRedactList` | `409` | `redact_list_changed`: the list changed after the revision you passed. Read it again and retry. |
| `testWebhook` | `409` | `webhook_not_set`: no webhook is set. |
| `getLedgerExport` | `422` | `entry_unreadable`: an entry in the chain could not be read, so the chain is not exported. |
| `signIn` | `429` | `locked`: the username is locked after repeated failures. `rate_limited`: the client address is over the sign-in rate limit. These are raised as `SignInLockedException` and `SignInRateLimitedException`. |
| `completeSignIn` | `429` | `rate_limited`, raised as `SignInRateLimitedException`. The MFA step has no username lockout. |

```java
try {
    client.createContext("tenant-a", false, false);
} catch (ClientException e) {
    if (ErrorReasons.CONTEXT_LIMIT_REACHED.equals(e.getReason())) {
        // Offer to delete a context first.
    } else if (ErrorReasons.CONTEXT_EXISTS.equals(e.getReason())) {
        // Choose another name.
    } else {
        throw e;
    }
}
```

`getStatusCode()` is `0` when the exception was not caused by a non-successful status, such as a response the client could not use. `SignInLockedException` and `SignInRateLimitedException` report `429`.

## Request ids

Philter gives every API request an id and returns it in the `X-Request-Id` response header, on successes and refusals alike. It records the same id as `request_id` on every audit event the request causes, so the id ties what a client saw to what Philter recorded. Log it with errors, and quote it when asking an administrator, or Philterd support, about a request.

Every exception built from a Philter response carries it: `ClientException` (and so `SignInLockedException` and `SignInRateLimitedException`), `UnauthorizedException`, and `ServiceUnavailableException` all have `getRequestId()`, and their messages end with it, so a logged stack trace carries it without extra code:

```
Unknown error: HTTP 409: {"message":"A policy with this name already exists.","reason":"policy_exists"} (request id: 01929f2e-6c3a-7b1d-9e4f-3a2b1c0d9e8f)
```

`getRequestId()` is `null` when the response had none, as from a Philter version before 4.0, or when the exception was not caused by a response. `getErrorMessage()` is unchanged: it is Philter's message alone, without the id, for showing to people.

For a call that succeeded, `getLastRequestId()` returns the id of the most recent response the client received on the calling thread. Read it right after the call. It is kept per thread, so calls other threads make with the same client do not change it, and it is `null` when the thread's last call got no response, such as a connection failure.

```java
try {
    client.replacePolicy("claims", policyJson);
    log.info("Replaced the claims policy (request id {})", client.getLastRequestId());
} catch (ClientException e) {
    log.warn("Philter refused the change: {} (request id {})", e.getErrorMessage(), e.getRequestId());
}
```

An administrator finds the request's audit events by their `request_id`, in the audit log's CSV export or with `getAuditLog`:

```java
GetAuditLogResponse page = admin.getAuditLog(null, from, to, null, 0, 100);
page.getEvents().stream()
        .filter(event -> requestId.equals(event.getRequestId()))
        .forEach(event -> System.out.println(event.getEvent() + " " + event.getDetails()));
```

Redaction events are the exception: `document_redaction_initiated` and `document_redaction_completed` record the document id as their `request_id`, so a queued redaction's start and completion share one id. Find those by the document id, from `FilterResponse.getDocumentId()`.

