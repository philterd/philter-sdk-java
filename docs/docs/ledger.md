# Redaction Ledger

The ledger records what Philter redacted, as a signed chain per document. Enable it on a context (see [Contexts](contexts.md)); it is off by default.

Ledger responses are returned as raw JSON strings, so parse them with whichever JSON library your application already uses.

```java
// The first page of entries, or of those matching a query.
String entries = client.getLedger(null);
String matching = client.getLedger("ssn");

// One document's chain, and an export that verifies standalone.
String chain = client.getLedgerEntry(documentId);
String export = client.exportLedger(documentId);

// Whether the chain's signatures still verify.
String valid = client.isLedgerValid(documentId);
```

`getLedger` returns one page at a time. See [Owners and Pagination](owners-and-pagination.md) to page through the rest, or to read another user's ledger as an administrator.

Verify an exported chain against the public signing key, which is served without authentication:

```java
String currentKey = client.getSigningKey();
String retiredKey = client.getSigningKey(keyId);  // a key an older entry was signed with
```

## Deleting ledger entries

Deletion is restricted: the caller must be an administrator and the deployment must set `LEDGER_DELETION_ENABLED=true`. A chain under an active legal hold cannot be deleted until the hold is released.

```java
// One document's chain.
client.deleteLedgerEntry(documentId);

// Every completed chain older than 90 days.
client.purgeLedger(90);
```
