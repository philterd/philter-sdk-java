# Policies: the Redaction Rules

A policy defines which entity types are detected and how each is redacted. Manage them with the policy methods. Policy bodies are JSON strings whose schema is defined by Philter; refer to the [Philter API specification](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/openapi.json) for the policy schema.

```java
import java.util.List;

// The first page of policy names. See Owners and Pagination for the rest.
List<String> policies = client.getPolicies();

// Retrieve a policy's JSON.
String policyJson = client.getPolicy("default");

// Create a policy. The body is the policy JSON. A name already in use is refused.
client.savePolicy("my-policy", policyJson);

// Replace an existing policy with a new revision.
client.replacePolicy("my-policy", policyJson);

// Apply your policy when filtering.
client.filter("my-context", "my-policy", "Some sensitive text...");

// Delete a policy.
client.deletePolicy("my-policy");
```

`savePolicy` only creates. A name the owner already uses is refused with a `ClientException` whose `getStatusCode()` is `409` and `getReason()` is `policy_exists`; replace the policy with `replacePolicy` instead. `replacePolicy` is refused with a `404` if there is no such policy, and with a `409` whose reason is `policy_changed` if the policy changed concurrently, in which case reload it and retry. `deletePolicy` is refused with a `404` if there is no such policy, and with a `409` whose reason is `policy_default` for the `default` policy, which cannot be deleted.

## Description and notes

A policy can carry a description (up to 200 characters) and notes (up to 1000). They are not part of the policy's JSON, so `getPolicy` does not return them and changing them does not create a new revision. Creating and replacing require `policies:write`, and reading requires `policies:read`.

```java
import ai.philterd.philter.model.PolicyDetails;

// Create the policy with a description and notes.
client.savePolicy("court", policyJson, "Federal court filings", "Reviewed with the clerk's office.");

// Replacing it without them keeps its current description and notes.
client.replacePolicy("court", policyJson);

// The description, notes, revision, whether it is managed, and when it was created and last updated.
PolicyDetails details = client.getPolicyDetails("court");

// Change them without saving the policy. null leaves a value as it is; "" clears it.
client.setPolicyDetails("court", null, "");
```

Philter takes a description and notes only through `setPolicyDetails`, in a JSON body. When you pass them to `savePolicy` or `replacePolicy`, the client sends the policy first and then calls `setPolicyDetails` with the same `owner`; a `null` value is left out. The two requests are not atomic: if the second fails, its `ClientException` is thrown but the policy was already created or replaced, without the new description or notes. Call `setPolicyDetails` to set them; retrying it is safe.

## Managed policies

Philter ships built-in managed policies, whose names begin with `managed_`. They can be read and copied, but not changed. Listing them requires `policies:read`; copying requires `policies:write`.

```java
import ai.philterd.philter.model.ManagedPolicySummary;

// The first page of managed policies, each with its name and description, paged like getPolicies.
for (ManagedPolicySummary policy : client.listManagedPolicies()) {
    System.out.println(policy.getName() + ": " + policy.getDescription());
}

// Read one by name, as any other policy.
String managedJson = client.getPolicy("managed_common_pii");
PolicyDetails managedDetails = client.getPolicyDetails("managed_common_pii");

// Create a policy of your own from it. The copy is active at once and has its own version history.
PolicyDetails copy = client.copyPolicy("managed_common_pii", "my-pii");
```

`copyPolicy` also duplicates one of your own policies. The copy has the source's policy and description; a copy of a managed policy has the note `Created from managed policy <name>`, and a copy of your own policy keeps its notes. A new name already in use is an HTTP 409, raised as a `ClientException` whose `getReason()` is `policy_exists`.

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

A rollback is refused with a `404` if the policy or the revision does not exist, and `getErrorMessage()` says which, for example `Revision 99 does not exist.` Only your own policies can be rolled back, so a managed policy is also a `404`. A rollback is refused with a `409` whose `getReason()` is `policy_changed` if the policy changed concurrently, in which case reload it and retry.

## Compiling PhiSQL

`compilePolicy(phiSql)` compiles [PhiSQL](https://github.com/philterd/phisql) source into a native policy without saving it. It requires the `policies:read` scope.

```java
String compiled = client.compilePolicy("POLICY ssn_only;\nREDACT SSN WITH MASK;");
// JSON: {"name": "ssn_only", "policy": {"identifiers": {...}}}
```

The response carries the compiled `policy`, the `name` from the source's `POLICY` declaration (`null` when it has none), and the `description` when the source declares one. The policy is validated before it is returned. To store it, pass the `policy` object's JSON to `savePolicy`, supplying a name if the source had none.

Source that fails to parse or compile, or a compiled policy that fails validation, is rejected with an HTTP 400, raised as a `ClientException` carrying the compiler's message.
