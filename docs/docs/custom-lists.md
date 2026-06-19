# Custom Lists

Custom lists let a policy match (or ignore) an explicit set of terms.

```java
import ai.philterd.philter.model.GetListsResponse;
import java.util.List;

// Save a list of terms.
client.saveList("blocked-terms", "Terms to always redact", List.of("Acme", "Project X"));

// Retrieve a list's values.
GetListsResponse list = client.getList("blocked-terms");
System.out.println(list.getLists());

// Delete a list.
client.deleteList("blocked-terms");
```
