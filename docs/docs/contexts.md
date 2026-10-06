# Contexts

Contexts group related requests under an arbitrary label and can enable features such as entity-type disambiguation and the redaction ledger.

```java
import ai.philterd.philter.model.GenericResponse;

// Create a context with entity-type disambiguation and the ledger enabled.
GenericResponse created = client.createContext("tenant-a", true, true);

// Then pass the context name on filter/explain calls.
client.filter("tenant-a", "default", "Some text...");

// Update or delete the context.
client.updateContext("tenant-a", false, true);
client.deleteContext("tenant-a");
```

A user can have at most 10 contexts, counting `default`. `createContext` is refused with a `409`, raised as a `ClientException`, whose `getReason()` is `context_limit_reached` at the limit and `context_exists` for a name the user already has. See [Error Handling](error-handling.md#acting-on-the-status).

Both flags default to `false` on the server, so `updateContext` sets the context to exactly the values you pass: a flag you leave out (or pass as `null`) is turned off, not left as it was. Send the full pair every time.

List the contexts, and inspect or clear the values a context has accumulated:

```java
import ai.philterd.philter.model.ContextDetails;
import ai.philterd.philter.model.ContextEntry;
import ai.philterd.philter.model.GetContextEntriesResponse;

List<String> contexts = client.listContexts().getContexts();

// The context's settings and its entries counted by filter type.
ContextDetails context = client.getContextDetails("tenant-a");
long size = context.getSize();
Map<String, Long> byFilterType = context.getFilterTypes();

// Its entries, a page at a time. The original values are never returned.
GetContextEntriesResponse page = client.listContextEntries("tenant-a", null, 0, 100);
for (ContextEntry entry : page.getEntries()) {
    System.out.println(entry.getFilterType() + " -> " + entry.getReplacement());
}

String export = client.exportContextEntries("tenant-a", null);

client.deleteContextEntry("tenant-a", entryId);
client.deleteContextEntries("tenant-a");
```

`getContextDetails` returns the context's size and its entries counted by filter type. The counts are computed by Philter in one query and sum to `size`; `untyped` counts entries with no filter type, which only an import creates.

```json
{
  "size": 125,
  "filterTypes": {
    "EMAIL_ADDRESS": 40,
    "PERSON": 83
  },
  "untyped": 2
}
```

A context's entries are its token-to-replacement mapping table, which is what gives [consistent pseudonymization](concepts.md) its referential integrity: the same source value keeps the same replacement until the entries are deleted. An export carries only token hashes and their replacements, never the original values, so it can be moved between environments. `importContextEntries(name, onConflict, owner, json)` loads one into another context; `onConflict` defaults to `skip`.
