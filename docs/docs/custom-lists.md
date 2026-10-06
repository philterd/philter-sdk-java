# Custom Lists

Custom lists let a policy match (or ignore) an explicit set of terms.

```java
import ai.philterd.philter.model.CustomListSummary;
import ai.philterd.philter.model.GetListsResponse;
import java.util.List;

// Save a list of terms.
client.saveList("blocked-terms", "Terms to always redact", List.of("Acme", "Project X"));

// Retrieve a list's values and description.
GetListsResponse list = client.getList("blocked-terms");
System.out.println(list.getLists() + " " + list.getDescription());

// Every list's name, description, and size, without its terms.
for (CustomListSummary summary : client.listCustomLists()) {
    System.out.println(summary.getName() + " (" + summary.getSize() + ")");
}

// Delete a list.
client.deleteList("blocked-terms");
```
