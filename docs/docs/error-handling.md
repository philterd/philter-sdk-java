# Error Handling

Every operation declares a checked `IOException`, thrown when the request cannot be executed (network failure, timeout, and so on). In addition, non-successful HTTP responses are mapped to unchecked exceptions:

| HTTP status | Exception |
|-------------|-----------|
| 401         | `UnauthorizedException` |
| 503         | `ServiceUnavailableException` |
| any other non-2xx | `ClientException` |

Philter explains a rejected request in the response body, and the `ClientException` message carries it (truncated at 512 characters) after the status code, so a 400 or a 403 says what was actually wrong:

```
Unknown error: HTTP 400: {"message":"'strategy' must be CRYPTO_REPLACE or FPE_ENCRYPT_REPLACE."}
```

`UnauthorizedException` and `ServiceUnavailableException` carry fixed messages, since a 401 and a 503 say all there is to say.

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
