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

Both flags default to `false` on the server, so `updateContext` sets the context to exactly the values you pass: a flag you leave out (or pass as `null`) is turned off, not left as it was. Send the full pair every time.

List the contexts, and inspect or clear the values a context has accumulated:

```java
String contexts = client.getContexts();                   // JSON
String entries = client.getContextEntries("tenant-a");    // JSON
String export = client.exportContextEntries("tenant-a", null);

client.deleteContextEntry("tenant-a", entryId);
client.deleteContextEntries("tenant-a");
```

A context's entries are its token-to-replacement mapping table, which is what gives [consistent pseudonymization](concepts.md) its referential integrity: the same source value keeps the same replacement until the entries are deleted. An export carries only token hashes and their replacements, never the original values, so it can be moved between environments. `importContextEntries(name, onConflict, owner, json)` loads one into another context; `onConflict` defaults to `skip`.
