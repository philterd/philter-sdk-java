# Contexts

Contexts group related requests under an arbitrary label and can enable features such as entity-type disambiguation and the redaction ledger.

```java
import ai.philterd.philter.model.DisambiguationScope;
import ai.philterd.philter.model.GenericResponse;

// Create a context with entity-type disambiguation and the ledger enabled.
GenericResponse created = client.createContext("tenant-a", true, true);

// Or have disambiguation learn across every document redacted in the context.
client.createContext("tenant-b", true, DisambiguationScope.CONTEXT, true, null);

// Then pass the context name on filter/explain calls.
client.filter("tenant-a", "default", "Some text...");

// Update or delete the context.
client.updateContext("tenant-a", false, true);
client.updateContext("tenant-a", null, DisambiguationScope.CONTEXT, null, null);
client.deleteContext("tenant-a");
```

A user can have at most 10 contexts, counting `default`. `createContext` is refused with a `409`, raised as a `ClientException`, whose `getReason()` is `context_limit_reached` at the limit and `context_exists` for a name the user already has. See [Error Handling](error-handling.md#acting-on-the-status).

A context name is used in request paths, so it cannot contain `/`, `\`, `;`, `%`, or a control character, and cannot be `.` or `..`; Philter refuses one that does with an HTTP 400, raised as a `ClientException`. A context created before Philter checked names may still have one. `deleteContext` sends such a name in the query string instead of the path, so it can still be deleted.

On `createContext`, a setting passed as `null` takes its default: `false` for both flags, and `document` for the disambiguation scope. On `updateContext`, only the settings given change; one passed as `null` keeps its current value. An update that gives no setting is refused with a `400`.

The disambiguation scope sets what [span disambiguation](https://github.com/philterd/philter/blob/main/docs/docs/other_features/span_disambiguation.md) learns from when entity-type disambiguation is enabled:

* `DisambiguationScope.DOCUMENT` (`document`, the default): each document is disambiguated on its own, from what Philter learns in that document. Nothing is stored.
* `DisambiguationScope.CONTEXT` (`context`): what Philter learns is stored and used for every later document redacted in the context. It is kept if the scope or disambiguation changes, and deleted when the context is emptied or deleted.

The value is not case-sensitive. Any other value is refused with a `400`, raised as a `ClientException`, and an update refused this way changes no setting.

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
String scope = context.getDisambiguationScope();

// Its entries, a page at a time. The original values are never returned.
GetContextEntriesResponse page = client.listContextEntries("tenant-a", null, 0, 100);
for (ContextEntry entry : page.getEntries()) {
    System.out.println(entry.getFilterType() + " -> " + entry.getReplacement());
}

String export = client.exportContextEntries("tenant-a", null);

client.deleteContextEntry("tenant-a", entryId);
client.deleteContextEntries("tenant-a");
```

`getContextDetails` returns the context's settings (`isEntityTypeDisambiguation()`, `getDisambiguationScope()`, which is `document` or `context`, and `isLedger()`), its size, and its entries counted by filter type. The counts are computed by Philter in one query and sum to `size`; `untyped` counts entries with no filter type, which only an import creates.

```json
{
  "size": 125,
  "filterTypes": {
    "EMAIL_ADDRESS": 40,
    "PERSON": 83
  },
  "untyped": 2,
  "entityTypeDisambiguation": true,
  "disambiguationScope": "document",
  "ledger": false
}
```

A context's entries are its token-to-replacement mapping table, which is what gives [consistent pseudonymization](concepts.md) its referential integrity: the same source value keeps the same replacement until the entries are deleted. An export carries only token hashes and their replacements, never the original values, so it can be moved between environments. `importContextEntries(name, onConflict, owner, json)` loads one into another context; `onConflict` defaults to `skip`.
