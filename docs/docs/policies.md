# Policies: the Redaction Rules

A policy defines which entity types are detected and how each is redacted. Manage them with the policy methods. Policy bodies are JSON strings whose schema is defined by Philter; refer to the [Philter API specification](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/openapi.json) for the policy schema.

```java
import java.util.List;

// The first page of policy names. See Owners and Pagination for the rest.
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

## Description and notes

A policy can carry a description (up to 200 characters) and notes (up to 1000). They are not part of the policy's JSON, so `getPolicy` does not return them and changing them does not create a new revision. Saving requires `policies:write`, and reading requires `policies:read`.

```java
import ai.philterd.philter.model.PolicyDetails;

// Save the policy with a description and notes.
client.savePolicy("court", policyJson, "Federal court filings", "Reviewed with the clerk's office.");

// Saving without them keeps the policy's current description and notes.
client.savePolicy("court", policyJson);

// The description, notes, revision, whether it is managed, and when it was created and last updated.
PolicyDetails details = client.getPolicyDetails("court");

// Change them without saving the policy. null leaves a value as it is; "" clears it.
client.setPolicyDetails("court", null, "");
```

## Managed policies

Philter ships built-in managed policies, whose names begin with `managed_`. They can be read and copied, but not changed. Listing them requires `policies:read`; copying requires `policies:write`.

```java
// The first page of managed policy names, paged like getPolicies.
List<String> managed = client.getManagedPolicies();

// Read one by name, as any other policy.
String managedJson = client.getPolicy("managed_common_pii");
PolicyDetails managedDetails = client.getPolicyDetails("managed_common_pii");

// Create a policy of your own from it. The copy is active at once and has its own version history.
PolicyDetails copy = client.copyPolicy("managed_common_pii", "my-pii");
```

`copyPolicy` also duplicates one of your own policies. The copy has the source's policy and description; a copy of a managed policy has the note `Created from managed policy <name>`, and a copy of your own policy keeps its notes. A new name already in use is an HTTP 409, raised as a `ClientException`.

## Revision history

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

`compilePolicy(policyJson)` asks Philter to compile a policy without saving it, which is a way to check a policy body before you store it:

```java
String compiled = client.compilePolicy(policyJson);
```

An invalid policy is rejected with an HTTP 400, raised as a `ClientException`.
