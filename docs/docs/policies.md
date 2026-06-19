# Policies: the Redaction Rules

A policy defines which entity types are detected and how each is redacted. Manage them with the policy methods. Policy bodies are JSON strings whose schema is defined by Philter; refer to the [Philter API specification](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/openapi.json) for the policy schema.

```java
import java.util.List;

// List the available policy names.
List<String> policies = client.getPolicies();

// Retrieve a policy's JSON.
String policyJson = client.getPolicy("default");

// Create or overwrite a policy. The body is the policy JSON.
client.savePolicy("my-policy", policyJson);

// Apply your policy when filtering.
client.filter("my-context", "my-policy", "Some sensitive text...");

// Delete a policy.
client.deletePolicy("my-policy");
```

Philter keeps a revision history for each policy:

```java
import ai.philterd.philter.model.PolicyVersionSummary;
import ai.philterd.philter.model.PolicyRollbackResponse;

for (PolicyVersionSummary v : client.getPolicyVersions("my-policy")) {
    System.out.println("revision " + v.getRevision() + " @ " + v.getCapturedTimestamp());
}

String revisionJson = client.getPolicyVersion("my-policy", 2);   // a specific revision
String diff = client.getPolicyDiff("my-policy", 1, 2);            // diff between revisions

PolicyRollbackResponse rollback = client.rollbackPolicy("my-policy", 1);
System.out.println("rolled back to revision " + rollback.getRevision());
```
