# Checking Server Health

`health()` does not require authentication and is useful for readiness checks.

```java
import ai.philterd.philter.model.StatusResponse;

StatusResponse status = client.health();
System.out.println(status.getStatus());             // "UP" when Philter is healthy
System.out.println(status.getApplicationVersion()); // e.g. "4.0.0"
System.out.println(status.getRedactionPolicySchemaVersion());
System.out.println(status.getGitCommit());
```

`getSigningKeyDetails()` is also unauthenticated. It returns the public key that Philter signs its output with, as PEM and as a JWK, with its fingerprint, so a recipient can verify a signature without credentials. `getSigningKeyDetails(keyId)` returns a retained key by ID, for verifying output signed with a key that has since been rotated.

```java
SigningKey key = client.getSigningKeyDetails();
String pem = key.getPem();
```

An administrator can rotate the signing key with `regenerateSigningKey()`, which requires the `signing:write` scope. Philter generates a new key and makes it active, and keeps the old one retrievable with `getSigningKeyDetails(keyId)`, so output signed with it stays verifiable. Resolve each signature's key ID rather than caching one key. Where the key is managed by `PHILTER_SIGNING_KEY_PATH`, rotating over the API is refused with an HTTP 409; replace the file and restart every instance instead.

```java
String activeKeyId = client.regenerateSigningKey();
```
