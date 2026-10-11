# Policies: the Redaction Rules

A policy defines which entity types are detected and how each is redacted. Manage them with the policy methods. Policy bodies are JSON strings whose schema is defined by Philter; refer to the [Philter API specification](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/openapi.json) for the policy schema.

```java
import java.util.List;

// The first page of policy names, and how many there are. See Owners and Pagination to sort, search, and page.
Page<String> policies = client.listPolicies(null, null);

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

`savePolicy` only creates. A name the owner already uses is refused with a `ClientException` whose `getStatusCode()` is `409` and `getReason()` is `policy_exists`; replace the policy with `replacePolicy` instead. `replacePolicy` is refused with a `404` if there is no such policy, and with a `409` whose reason is `policy_changed` if the policy changed concurrently, in which case reload it and retry. `deletePolicy` is refused with a `404` if there is no such policy, and with a `409` whose reason is `policy_default` for the `default` policy, which cannot be deleted. Both refuse a [managed policy](#managed-policies) with a `409` whose reason is `policy_managed`.

## Starting from a template

Rather than keep your own starting policy, which can fall behind the schema as Philter adds filters, start from one of Philter's templates. `getPolicyTemplate` returns a template's policy JSON, which Philter accepts for its running schema version. Edit it, then save it as a policy:

```java
String template = client.getPolicyTemplate("default");
client.savePolicy("claims", template);
```

The `default` template is the one every new user's `default` policy is created from: it redacts person names, Social Security numbers, and email addresses. Philter detects person names with PhEye, so that part needs a PhEye service your deployment can reach. More templates may be added. A template is not a policy, and cannot be used to redact until it is saved as one. It requires the `policies:read` scope. A name Philter has no template for is a `404`, and the `ClientException`'s `getErrorMessage()` lists the templates there are.

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

Philter ships built-in managed policies, whose names begin with `managed_`. They can be read and copied, but not changed: `replacePolicy`, `deletePolicy`, `rollbackPolicy`, and `setPolicyDetails` refuse a `managed_` name with a `409` whose `getReason()` is `policy_managed`. Copy one with `copyPolicy` and change the copy instead. Listing them requires `policies:read`; copying requires `policies:write`.

```java
import ai.philterd.philter.model.ManagedPolicySummary;

// The first page of managed policies, each with its name and description, paged like listPolicies.
for (ManagedPolicySummary policy : client.listManagedPolicies(null).getItems()) {
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
import ai.philterd.philter.model.PolicyDiff;

// Newest first, a page at a time; getTotal() counts the retained revisions.
for (PolicyVersionSummary v : client.listPolicyVersions("my-policy", null, null).getItems()) {
    // The author is the username whose change produced the revision, or null when it is not known.
    System.out.println("revision " + v.getRevision() + " @ " + v.getCapturedTimestamp() + " by " + v.getAuthor());
}

String revisionJson = client.getPolicyVersion("my-policy", 2);   // a specific revision
PolicyDiff diff = client.diffPolicy("my-policy", 1, 2);           // diff between revisions
PolicyDiff latest = client.diffPolicy("my-policy");               // the two most recent revisions
for (PolicyDiff.Change change : latest.getChanges()) {
    System.out.println(change.getOp() + " " + change.getPath() + ": " + change.getOldValue() + " -> " + change.getValue());
}

PolicyRollbackResponse rollback = client.rollbackPolicy("my-policy", 1);
System.out.println("rolled back to revision " + rollback.getRevision());
```

A rollback is refused with a `404` if the policy or the revision does not exist, and `getErrorMessage()` says which, for example `Revision 99 does not exist.` It is refused with a `409` whose `getReason()` is `policy_managed` for a managed policy, and `policy_changed` if the policy changed concurrently, in which case reload it and retry.

A `PolicyDiff` has the `from` and `to` revisions and the RFC 6902 changes that turn one into the other. Each change has its `op` (`add`, `remove`, or `replace`), the JSON Pointer `path` that changed, the new `value` (`null` for a removal), and the `oldValue` before it (`null` for an addition), both as Gson `JsonElement`s. Without revision numbers, `diffPolicy` compares the two most recent retained revisions, and `from` and `to` say which they were. It reads the retained history, so it works for a [deleted policy](#deleted-policies) too. A policy with fewer than two retained revisions, including a name with no history at all, is a `400`. `getPolicyDiff`, which returns the diff as a JSON string, is deprecated.

`getAuthor()` is `null` for a revision made before Philter recorded authors, for a managed policy, and for a revision first captured when the policy was used to redact.

## Deleted policies

Deleting a policy keeps its revision history. `getDeletedPolicies` lists the deleted policies whose history is kept, sorted by name, with each one's latest revision and when and by whom it was deleted. It requires `policies:read`; an administrator can pass another user's username as `owner`.

```java
import ai.philterd.philter.model.DeletedPolicy;
import ai.philterd.philter.model.GetDeletedPoliciesResponse;

GetDeletedPoliciesResponse page = client.getDeletedPolicies(null, 0, 25);
for (DeletedPolicy deleted : page.getPolicies()) {
    System.out.println(deleted.getName() + " revision " + deleted.getLatestRevision()
            + " deleted " + deleted.getDeletedAt() + " by " + deleted.getDeletedBy());
}
```

`getDeletedAt()` is an `Instant`, and it and `getDeletedBy()` are `null` for a policy deleted before Philter recorded deletions. `getTotal()` counts every deleted policy whose history is kept, across pages.

Read a deleted policy's history by its name, as for a live one, with `listPolicyVersions` and `getPolicyVersion`. To restore it, save the revision you want under the name with `savePolicy`; the restored policy continues the same revision numbers. `rollbackPolicy` only applies to a live policy.

```java
String json = client.getPolicyVersion("claims-2025", 4);
client.savePolicy("claims-2025", json);
```

## Compiling PhiSQL

`compilePhiSql(phiSql)` compiles [PhiSQL](https://github.com/philterd/phisql) source into a native policy without saving it. It requires the `policies:read` scope.

```java
import ai.philterd.philter.model.CompilePolicyResponse;

CompilePolicyResponse compiled = client.compilePhiSql("POLICY ssn_only;\nREDACT SSN WITH MASK;");
client.savePolicy(compiled.getName(), compiled.getPolicyJson());
```

The result carries the compiled policy (`getPolicy()` as a JSON object, `getPolicyJson()` as a string for `savePolicy`), the `getName()` from the source's `POLICY` declaration (`null` when it has none), and the `getDescription()` when the source declares one. The policy is validated before it is returned. Supply a name to `savePolicy` if the source had none. `compilePolicy`, which returns the response as a JSON string, is deprecated.

Source that fails to parse or compile, or a compiled policy that fails validation, is rejected with an HTTP 400, raised as a `ClientException` carrying the compiler's message.
