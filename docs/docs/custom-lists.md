# Custom Lists

Custom lists let a policy match (or ignore) an explicit set of terms.

```java
import ai.philterd.philter.model.CustomListSummary;
import ai.philterd.philter.model.GetListsResponse;
import java.util.List;

// Create a list of terms. A name already in use is refused.
client.saveList("blocked-terms", "Terms to always redact", List.of("Acme", "Project X"));

// Replace the list's terms. A null description keeps the current one; "" clears it.
client.replaceList("blocked-terms", null, List.of("Acme", "Project X", "Project Y"));

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

`saveList` only creates. A name the owner already uses is refused with a `ClientException` whose `getStatusCode()` is `409` and `getReason()` is `list_exists`; replace the list with `replaceList` instead, which is refused with a `404` if there is no such list. A list holds up to 100 items of up to 50 characters each; more is an HTTP 400.

A list name is used in request paths, so it cannot contain `/`, `\`, `;`, `%`, or a control character, and cannot be `.` or `..`. `saveList` with such a name gets an HTTP 400, sometimes from the web server before the request reaches Philter, in which case the `ClientException` has no error message. A list created before Philter checked names may still have one. `deleteList` sends such a name in the query string instead of the path, so it can still be deleted.
