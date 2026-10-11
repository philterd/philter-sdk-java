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

## One list at a time, without losing changes

`getRedactList` reads one list with its revision, and `replaceRedactList` replaces only that list, leaving the other as it is. Every write of a list moves its revision forward; a list never written is at `0`. `listRedactLists()` also gives both revisions, as `getAlwaysRedactRevision()` and `getNeverRedactRevision()`.

Pass the revision you read to `replaceRedactList`, and the list is replaced only if nobody changed it in between. Otherwise nothing is written and Philter answers HTTP 409, raised as a `ClientException` whose `getReason()` is `ErrorReasons.REDACT_LIST_CHANGED`. Read the list again, apply your change to what you read, and retry:

```java
import ai.philterd.philter.model.RedactList;
import ai.philterd.philter.model.RedactListTerms;
import ai.philterd.philter.model.exceptions.ClientException;
import ai.philterd.philter.model.exceptions.ErrorReasons;

boolean written = false;
for (int attempt = 0; attempt < 3 && !written; attempt++) {
    RedactListTerms current = client.getRedactList(RedactList.ALWAYS);
    // The whole list is replaced, so start from the terms it has.
    List<String> terms = new ArrayList<>(current.getTerms());
    terms.add("Project X");
    try {
        client.replaceRedactList(RedactList.ALWAYS, terms, current.getRevision());
        written = true;
    } catch (ClientException e) {
        if (!ErrorReasons.REDACT_LIST_CHANGED.equals(e.getReason())) {
            throw e;
        }
        // Someone else changed the list; read it again and reapply the change.
    }
}
if (!written) {
    // The list kept changing; tell the person their change was not saved.
}
```

With a `null` revision, `replaceRedactList` replaces the list whatever its revision. A `null` or empty terms list clears it. The result is the list as written, with its new revision, ready for the next change. Philter trims terms and drops blank ones. Reading requires `lists:read` and replacing `lists:write`, and both take an `owner` for an administrator acting on another user's lists. `createRedactList` and `updateRedactList`, which write both lists at once, take no revision.

These lists are distinct from the named [custom lists](custom-lists.md), which a policy references by name.
