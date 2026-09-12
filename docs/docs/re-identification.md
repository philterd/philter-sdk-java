# Re-identifying Redacted Values

Values redacted with one of Philter's reversible strategies can be re-identified one value at a time. Philter accepts two strategies, named exactly as the policy names them:

| Strategy | Notes |
|----------|-------|
| `CRYPTO_REPLACE` | Requires `policyName`, which names the policy whose key was used. |
| `FPE_ENCRYPT_REPLACE` | `policyName` is not required. |

A `reason` is required on every request; Philter records it in the audit log.

```java
import ai.philterd.philter.model.ReidentifyRequest;
import java.util.List;

ReidentifyRequest request = new ReidentifyRequest();
request.setStrategy("CRYPTO_REPLACE");
request.setPolicyName("my-policy");
request.setReason("authorized support investigation");
request.setValues(List.of("<redacted-value>"));

// The first argument is the owner, for an administrator re-identifying another
// user's values. Pass null to act on your own.
String result = client.reidentify(null, request);
```

Any other strategy name, a missing reason, or an empty value list is rejected with an HTTP 400, which the client raises as a `ClientException`.

Re-identification is per value, not per document: pass the redacted values you want restored. It does not reverse a whole redacted document, and it does not apply to non-reversible strategies such as redaction, masking, or hashing.
