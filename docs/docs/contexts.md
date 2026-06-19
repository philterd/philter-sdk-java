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
