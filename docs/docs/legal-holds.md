# Legal Holds

A legal hold blocks deletion and purging of the evidence it covers until the hold is released. Ledger chains under an active hold cannot be deleted (Philter answers HTTP 423), and neither can the user's entries be purged.

```java
import ai.philterd.philter.model.LegalHoldRequest;
import ai.philterd.philter.model.LegalHoldResponse;
import java.util.List;

LegalHoldRequest request = new LegalHoldRequest();
request.setReference("matter-2026-014");     // your identifier, unique per owner
request.setScopeType("document_chain");      // or "user"
request.setScopeValue(documentId);           // the document ID, or the username
request.setReason("pending litigation");     // optional, recorded with the hold

LegalHoldResponse hold = client.createHold(request);
System.out.println(hold.getSetAt());
```

`reference`, `scopeType`, and `scopeValue` are all required, and `scopeType` must be `document_chain` or `user`. Anything else is rejected with an HTTP 400. Re-using a reference that is already held returns an HTTP 409. Both surface as a `ClientException`.

```java
// The first page of active holds, most recently set first.
List<LegalHoldResponse> holds = client.getHolds();

LegalHoldResponse one = client.getHold("matter-2026-014");

// Release the hold. What it covered becomes deletable again.
client.deleteHold("matter-2026-014");
```
