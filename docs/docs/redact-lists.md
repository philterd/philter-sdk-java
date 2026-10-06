# Always-Redact and Never-Redact Lists

Each account has two term lists that apply across its policies: terms to always redact, and terms to never redact.

```java
import ai.philterd.philter.model.RedactLists;
import ai.philterd.philter.model.RedactListsRequest;

// Read both lists.
RedactLists lists = client.listRedactLists();
List<String> always = lists.getAlwaysRedact();
List<String> never = lists.getNeverRedact();

// Replace both lists in full. This is not a merge: a list you leave null or empty is cleared.
client.createRedactList(new RedactListsRequest(List.of("Project X"), List.of("Acme")));

// Append to the existing lists instead of replacing them. A null list is left as it is.
client.updateRedactList(new RedactListsRequest(List.of("Project Y"), null));
```

A list holds up to 1000 terms of up to 100 characters each; exceeding either is an HTTP 400, raised as a `ClientException`.

These lists are distinct from the named [custom lists](custom-lists.md), which a policy references by name.
