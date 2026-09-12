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

`getSigningKey()` is also unauthenticated. It returns the public key that Philter signs its output with, so a recipient can verify a signature without credentials. `getSigningKey(keyId)` returns a retained key by ID, for verifying output signed with a key that has since been rotated.

```java
String key = client.getSigningKey();
```
