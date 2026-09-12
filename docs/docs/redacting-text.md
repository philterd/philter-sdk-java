# Redacting Text

`filter(context, policyName, text)` sends text to Philter and returns the redacted text. This is the core redaction call.

```java
import ai.philterd.philter.model.FilterResponse;

FilterResponse response = client.filter("my-context", "default",
        "His name is George Washington and his SSN is 123-45-6789.");

String redacted = response.getFilteredText();
// e.g. "His name is {{{REDACTED-person}}} and his SSN is {{{REDACTED-ssn}}}."
// (the exact replacement depends on the policy's filter strategies)

String documentId = response.getDocumentId();  // assigned by Philter
String context = response.getContext();         // echoes "my-context"
```

Pass a filename with the four-argument overload to record the source of the text against the document:

```java
FilterResponse response = client.filter("my-context", "default", "notes.txt", text);
```

`explain(context, policyName, filename, text)` takes a filename the same way.

The replacement text (redaction tokens, masking characters, encrypted values, and so on) is entirely determined by the policy you name. See [Policies](policies.md).
