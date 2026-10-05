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

`UnauthorizedException` carries Philter's message, such as `Invalid or missing credentials`, or `Unauthorized` when the response has none. Philter gives the same 401 for an unknown key, an expired or revoked session key, and a deactivated user, so the client cannot tell them apart. `ServiceUnavailableException` carries a fixed message.

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

## Acting on the status

`ClientException` exposes the HTTP status with `getStatusCode()` and the `message` field of Philter's JSON error body with `getErrorMessage()`. Branch on the status, not on the exception message, whose wording is for logs and may change. `getErrorMessage()` is read from the whole body, so it is complete even when the exception message was truncated, and it is `null` when the body was not JSON or had no `message`. Philter returns its errors as JSON with a `message`, so in practice it is `null` for a response with no body, such as the `404` from `getUser`, `revokeApiKey`, or `getPolicy` when what it names does not exist; for a `403` from `exportAuditLog` for a caller who is not an administrator, which is plain text; and for a response that did not come from Philter, such as a proxy's error page. Fall back to the status in those cases.

```java
try {
    client.changePassword(currentPassword, newPassword);
} catch (ClientException e) {
    switch (e.getStatusCode()) {
        case 400:
            // The new password was rejected; show the person why.
            System.out.println(e.getErrorMessage());
            break;
        case 403:
            // The current password is not correct, or the key lacks users:write.
            break;
        case 409:
            // The user has no password yet; an administrator sets the first one.
            break;
        default:
            throw e;
    }
}
```

`getStatusCode()` is `0` when the exception was not caused by a non-successful status, such as a response the client could not use. `SignInLockedException` and `SignInRateLimitedException` report `429`.

