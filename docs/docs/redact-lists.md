# Always-Redact and Never-Redact Lists

Each account has two term lists that apply across its policies: terms to always redact, and terms to never redact. Both are returned together as a JSON object with `alwaysRedact` and `neverRedact` arrays.

```java
// Read both lists.
String lists = client.getRedactLists();
// {"alwaysRedact":["Project X"],"neverRedact":["Acme"]}

// Replace both lists in full. This is not a merge: a field you omit is emptied.
client.createRedactList("{\"alwaysRedact\":[\"Project X\"],\"neverRedact\":[\"Acme\"]}");

// Append to the existing lists instead of replacing them.
client.updateRedactList("{\"alwaysRedact\":[\"Project Y\"]}");
```

Philter caps how many terms a list may hold and how long each term may be; exceeding either is an HTTP 400, raised as a `ClientException`.

These lists are distinct from the named [custom lists](custom-lists.md), which a policy references by name.
