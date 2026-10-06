# Redaction Ledger

The ledger records what Philter redacted, as a signed chain per document. Enable it on a context (see [Contexts](contexts.md)); it is off by default.

```java
import ai.philterd.philter.model.GetLedgerResponse;
import ai.philterd.philter.model.LedgerChain;
import ai.philterd.philter.model.LedgerExport;

// The head of each chain, most recent first, or of those whose document ID or filename matches.
GetLedgerResponse chains = client.listLedgerChains(null);
GetLedgerResponse matching = client.listLedgerChains("note.txt");
int total = chains.getTotal();

// One document's chain, and whether its hash chain and signatures verify.
LedgerChain chain = client.getLedgerChain(documentId);
boolean valid = chain.isValid();

// Just whether it verifies, without the entries.
boolean stillValid = client.verifyLedgerChain(documentId).isValid();

// An export that verifies standalone. It carries the original redacted values.
LedgerExport export = client.getLedgerExport(documentId);
```

A chain that is not valid either failed a check or could not be checked at all:

```java
LedgerChain chain = client.verifyLedgerChain(documentId);
if (chain.isValid()) {
    // The hash chain and every signature verify.
} else if (chain.getValidationError() != null) {
    // Not checked, for example because an entry no longer decrypts. Not evidence of tampering.
    System.out.println(chain.getValidationError());
} else if (Boolean.FALSE.equals(chain.getHashChainValid())) {
    // An entry no longer matches its hash or its link to the previous entry.
} else {
    // chain.getSignaturesValid() is false: an entry is unsigned (counted by getUnsignedEntries())
    // or its signature does not verify.
}
```

When the chain could not be checked, `getHashChainValid()`, `getSignaturesValid()`, `getSignedEntries()`, `getUnsignedEntries()`, and `getEntries()` are `null`.

Reading or listing a chain does not return the values that were redacted, only their replacements. An export does, in each entry's `getToken()`, which is why it needs the `ledger:export` scope; treat it as sensitive.

`listLedgerChains` returns one page at a time. See [Owners and Pagination](owners-and-pagination.md) to page through the rest, or to read another user's ledger as an administrator.

Verify an exported chain against the public signing key, which is served without authentication:

```java
import ai.philterd.philter.model.SigningKey;

// The key an entry was signed with, by its getSigningKeyId(). Superseded keys stay retrievable.
SigningKey key = client.getSigningKeyDetails(entry.getSigningKeyId());
String pem = key.getPem();
```

An export also embeds the public keys its entries were signed with, in `export.getSigningKeys()`.

## Deleting ledger entries

Deletion is restricted: the caller must be an administrator and the deployment must set `LEDGER_DELETION_ENABLED=true`. A chain under an active legal hold cannot be deleted until the hold is released.

```java
// One document's chain.
client.deleteLedgerEntry(documentId);

// Every completed chain older than 90 days.
client.purgeLedger(90);
```
