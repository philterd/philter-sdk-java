# Re-identifying Redacted Values

Values redacted with one of Philter's reversible strategies can be re-identified one value at a time. Philter accepts two strategies, named exactly as the policy names them:

| Strategy | Notes |
|----------|-------|
| `CRYPTO_REPLACE` | Requires `policyName`, which names the policy whose key was used. |
| `FPE_ENCRYPT_REPLACE` | `policyName` is not required. |

The `ReidentifyStrategy` enum names both. `setStrategy(String)` is still available for sending a strategy name the enum does not have.

A `reason` is required on every request; Philter records it in the audit log.

```java
import ai.philterd.philter.model.ReidentifyRequest;
import ai.philterd.philter.model.ReidentifyResponse;
import ai.philterd.philter.model.ReidentifyStrategy;
import java.util.List;

ReidentifyRequest request = new ReidentifyRequest();
request.setStrategy(ReidentifyStrategy.CRYPTO_REPLACE);
request.setPolicyName("my-policy");
request.setReason("authorized support investigation");
request.setValues(List.of("<redacted-value>"));

ReidentifyResponse response = client.reidentifyValues(request);
for (ReidentifyResponse.ReidentifyResult result : response.getResults()) {
    // The original value, or null with an error saying why it could not be re-identified.
    System.out.println(result.getEncrypted() + " -> " + result.getDecrypted() + " " + result.getError());
}

// An administrator re-identifying another user's values passes the owner first.
client.reidentifyValues("someone@example.com", request);
```

There is one result for each value, in the order sent. A value that cannot be re-identified does not fail the request: its result has an error and no decrypted value.

Any other strategy name, a missing reason, or an empty value list is rejected with an HTTP 400, which the client raises as a `ClientException`.

`reidentifyValues` is per value: pass the redacted values you want restored. To restore a whole text, see [Re-identifying a text](#re-identifying-a-text). Neither applies to non-reversible strategies such as redaction, masking, or hashing.

## Re-identifying a text

`reidentifyText` takes a text, such as a redacted document or an LLM's response to a redacted prompt, and returns it with each `CRYPTO_REPLACE` value restored. Philter finds every `{{...}}` token in the text, decrypts it with the named policy's key, and puts the original value in its place. `text`, `policyName`, and `reason` are all required.

```java
import ai.philterd.philter.model.ReidentifyTextRequest;
import ai.philterd.philter.model.ReidentifyTextResponse;

ReidentifyTextRequest request = new ReidentifyTextRequest();
request.setText(redactedText);
request.setPolicyName("my-policy");
request.setReason("authorized claims review");

ReidentifyTextResponse response = client.reidentifyText(request);
String original = response.getText();
System.out.println(response.getFound() + " found, " + response.getRestored() + " restored, "
        + response.getFailed() + " failed");

// An administrator restoring a text redacted under another user's policy passes the owner first.
client.reidentifyText("someone@example.com", request);
```

Limits:

- Only `CRYPTO_REPLACE` tokens are restored. `FPE_ENCRYPT_REPLACE` replacements cannot be told apart from ordinary data in a text, so reverse them with `reidentifyValues`.
- A token that does not decrypt is left in the text unchanged and counted in `getFailed()`. This includes a token encrypted under another policy's key and one altered after redaction, for example shortened by an LLM. The call still succeeds.
- A token altered so that it is no longer `{{` followed by Base64 and `}}`, for example split by a space or missing a brace, is not found, so it is counted in neither `getFound()` nor `getFailed()` and is left unchanged. A model's response can therefore come back partly restored with `getFailed()` at `0`: check the text, not only the counts.

A missing field or a policy without a crypto key is an HTTP 400, a key without the `reidentify` scope a 403, and a policy that does not exist a 404, each raised as a `ClientException`. The `owner` overload behaves as it does for `reidentifyValues`: another user's policy requires an administrator and `ADMIN_CROSS_USER_ACCESS_ENABLED=true`, and is otherwise a 404. A text larger than the deployment's request body limit, `getLimits().getRequests().getMaxBodyBytes()` (256 KiB by default), is a 413. Requires Philter 4.0.0 or later.
