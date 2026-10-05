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

## Signed responses

Philter can sign the text it returns, so a recipient can verify that the response came from a specific deployment, was governed by the named policy, and was not modified. Pass `true` as the last argument of `filter` or `explain` to ask for a signature:

```java
FilterResponse response = client.filter("my-context", "default", null, text, true);

String signature = response.getSignature();  // a compact ES256 JWT, or null if unsigned
```

A deployment with output signing enabled (the `signingEnabled` [admin setting](admin-settings.md)) signs every `filter` and `explain` response whether or not you ask, and passing `false` does not turn that off. PDF responses are not signed.

The client does not verify signatures. To verify one, fetch the public key named by the JWT's `kid` with `getSigningKey(keyId)`, which needs no API key, check the ES256 signature, and check that the `bodyHash` claim is the SHA-256 of the response body. For `filter` the body is `getFilteredText()`; for `explain` it is `getResponseBody()`, the JSON exactly as Philter sent it.

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
String pem = JsonParser.parseString(client.getSigningKey(header.get("kid").getAsString()))
        .getAsJsonObject().get("pem").getAsString()
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

