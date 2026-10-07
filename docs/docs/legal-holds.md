# Legal Holds

A legal hold blocks deletion and purging of the evidence it covers until the hold is released. Ledger chains under an active hold cannot be deleted (Philter answers HTTP 423), and neither can the user's entries be purged.

```java
import ai.philterd.philter.model.LegalHoldRequest;
import ai.philterd.philter.model.LegalHoldResponse;
import java.util.List;

LegalHoldRequest request = new LegalHoldRequest();
request.setReference("matter-2026-014");     // your identifier, unique per owner
request.setScopeType("document_chain");      // one document's ledger chain
request.setScopeValue(documentId);           // the document ID
request.setReason("pending litigation");     // optional, recorded with the hold

LegalHoldResponse hold = client.createHold(request);
System.out.println(hold.getSetAt());
```

`reference` and `scopeType` are required, and `scopeType` must be `document_chain` or `user`. A `document_chain` hold also needs `scopeValue`, the document ID.

A `user` hold protects all of its owner's ledger evidence. The owner is the caller, or the user an administrator names with `owner`, so a `user` hold needs no `scopeValue`; one that is given must be the owner's username. Philter returns the owner's username as the hold's `scopeValue`:

```java
LegalHoldRequest all = new LegalHoldRequest();
all.setReference("matter-2026-015");
all.setScopeType("user");                    // everything the owner holds

LegalHoldResponse mine = client.createHold(all);            // the caller's evidence
LegalHoldResponse theirs = client.createHold(all, "jordan"); // an administrator, for another user
System.out.println(theirs.getScopeValue());                  // "jordan"
```

Naming another user with `owner` requires an administrator and `ADMIN_CROSS_USER_ACCESS_ENABLED`; see [Owners and Pagination](owners-and-pagination.md).

Philter rejects with an HTTP 400 a missing `reference` or `scopeType`, any other scope type, a `document_chain` hold without `scopeValue`, a `user` hold whose `scopeValue` names anyone but its owner, and a reference that cannot be used in a request path: one containing `/`, `\`, `;`, `%`, or a control character, or that is `.` or `..`. Re-using a reference that is already held returns an HTTP 409. Both surface as a `ClientException`.

```java
// The first page of active holds, most recently set first.
List<LegalHoldResponse> holds = client.getHolds();

LegalHoldResponse one = client.getHold("matter-2026-014");

// Release the hold. What it covered becomes deletable again.
client.deleteHold("matter-2026-014");
```

A hold set before Philter checked references may have one that cannot be used in a path. `deleteHold` sends such a reference in the query string instead, so the hold can still be released.
