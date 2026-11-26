# Redacting Text

`filter(context, policyName, text)` sends text to Philter and returns the redacted text. This is the core redaction call.

```java
import ai.philterd.philter.model.FilterResponse;

FilterResponse response = client.filter("my-context", "default",
        "His name is George Washington and his SSN is 123-45-6789.");

String redacted = response.getFilteredText();
// e.g. "His name is {{{REDACTED-person}}} and his SSN is {{{REDACTED-ssn}}}."
// (the exact replacement depends on the policy's filter strategies)

String documentId = response.getDocumentId();  // assigned by Philter
String context = response.getContext();         // echoes "my-context"
```

Pass a filename with the four-argument overload to record the source of the text against the document:

```java
FilterResponse response = client.filter("my-context", "default", "notes.txt", text);
```

`explain(context, policyName, filename, text)` takes a filename the same way.

The replacement text (redaction tokens, masking characters, encrypted values, and so on) is entirely determined by the policy you name. See [Policies](policies.md).

## Requests selected for review

A policy with a review sampling rate (`config.review.samplingRate`) has that share of its requests selected at random for human spot review. `isReviewSampled()` is `true` when Philter selected the request, so the result can be routed to a reviewer:

```java
FilterResponse response = client.filter("my-context", "default", text);

if (response.isReviewSampled()) {
    // Queue response.getDocumentId() and the redacted text for a reviewer.
}
```

It reads Philter's `X-Philter-Review` response header, which lists the reasons a request was flagged, separated by commas (for example `sampled, leftovers`), and is `true` when `sampled` is among them. A response without the header, or one listing only other reasons, reads as `false`. The same method is on the responses from `explain` ([Explain](explain.md)), the PDF, Word, and Excel methods ([PDF](pdf.md#requests-selected-for-review), [Word and Excel](word-and-excel.md#requests-selected-for-review)), and asynchronous submissions, where the selection is reported only on the submission's response, not on the document's status or download. Each selection is recorded in the [audit log](audit-log.md) as `redaction_review_sampled`. Requires Philter 4.1.0 or later; from an earlier Philter it is always `false`.

A request that was not selected has not been checked by anyone. Detection is probabilistic, so validate results against your own data whether or not a request was selected. See Philter's [review sampling](https://github.com/philterd/philter/blob/main/docs/docs/policies/filter_policies.md#sampling-requests-for-review) documentation to configure the rate.

## Checking what is left (leftover check)

A policy can have Philter run detection a second time over the redacted output and report what is still detectable, with `config.review.leftoverCheck`. Under `mode: always` every request is checked. Under `onRequest`, a request is checked when it asks, with `FilterOptions`:

```java
FilterOptions options = FilterOptions.defaults().withLeftoverCheck(true);

FilterResponse response = client.filter("my-context", "default", null, text, false, options);

response.getLeftoverCheck().ifPresent(check -> {
    if (!check.isPerformed()) {
        // Nothing was checked: the output had no readable text, or the check failed.
    } else if (check.getFindingCount() > 0) {
        // Something is still detectable.
    }
});

boolean flagged = response.isLeftoversFlagged();
```

`getLeftoverCheck()` reads Philter's `X-Philter-Leftovers` header. It is empty when the check was not called for. `isPerformed()` is `false` when nothing was checked, because the output had no readable text or the check failed; the redacted output is returned either way, and `getFindingCount()` is then 0 without meaning that nothing was found. A header value that is not a count, including one a later Philter may send, also reads as not performed. `isLeftoversFlagged()` is `true` when `X-Philter-Review` lists `leftovers`, which Philter adds when the check found something.

`withLeftoverCheck(true)` sends `leftover_check=true`. Under `always` the check runs whatever the option says. Under `off`, Philter refuses the request with an HTTP 400, raised as a `ClientException` carrying Philter's message. The same option is taken by `explain` ([Explain](explain.md#leftover-check)) and by the PDF, Word, and Excel methods ([PDF](pdf.md#leftover-check), [Word and Excel](word-and-excel.md#leftover-check)). Each request with findings is recorded in the [audit log](audit-log.md) as `redaction_leftovers_found`. Requires Philter 4.1.0 or later.

The check runs detection again, so it adds latency to each request it runs on. A count of 0 does not mean the output is free of sensitive information: detection is probabilistic, and results still need validating against your own data. See Philter's [leftover check](https://github.com/philterd/philter/blob/main/docs/docs/policies/filter_policies.md#leftover-check) documentation to configure the policy.

## Signed responses

Philter can sign the text it returns, so a recipient can verify that the response came from a specific deployment, was governed by the named policy, and was not modified. Pass `true` as the last argument of `filter` or `explain` to ask for a signature:

```java
FilterResponse response = client.filter("my-context", "default", null, text, true);

String signature = response.getSignature();  // a compact ES256 JWT, or null if unsigned
```

A deployment with output signing enabled (the `signingEnabled` [admin setting](admin-settings.md)) signs every `filter` and `explain` response whether or not you ask, and passing `false` does not turn that off. PDF responses are not signed.

The client does not verify signatures. To verify one, fetch the public key named by the JWT's `kid` with `getSigningKeyDetails(keyId)`, which needs no API key, check the ES256 signature, and check that the `bodyHash` claim is the SHA-256 of the response body. For `filter` the body is `getFilteredText()`; for `explain` it is `getResponseBody()`, the JSON exactly as Philter sent it.

```java
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

String[] parts = signature.split("\\.");
Base64.Decoder base64url = Base64.getUrlDecoder();

// The public key named by the JWT's kid.
JsonObject header = JsonParser.parseString(new String(base64url.decode(parts[0]), StandardCharsets.UTF_8)).getAsJsonObject();
String pem = client.getSigningKeyDetails(header.get("kid").getAsString()).getPem()
        .replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));

// The ES256 signature over the JWT's header and payload.
Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
verifier.initVerify(publicKey);
verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
boolean signatureValid = verifier.verify(base64url.decode(parts[2]));

// The body the signature covers.
JsonObject payload = JsonParser.parseString(new String(base64url.decode(parts[1]), StandardCharsets.UTF_8)).getAsJsonObject();
byte[] digest = MessageDigest.getInstance("SHA-256").digest(response.getFilteredText().getBytes(StandardCharsets.UTF_8));
StringBuilder bodyHash = new StringBuilder();
for (byte b : digest) {
    bodyHash.append(String.format("%02x", b));
}
boolean bodyMatches = bodyHash.toString().equals(payload.get("bodyHash").getAsString());
```

The payload also carries `policyName`, `policyVersion`, `documentId`, and `iat`, the time it was signed. Cache public keys by ID: after the key is rotated, older signatures still verify against the key they name. See Philter's [Output Signing](https://github.com/philterd/philter/blob/main/docs/docs/output_signing.md) documentation.

