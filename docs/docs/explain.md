# Inspecting What Was Redacted (Explain)

When you need to know *what* Philter found and where, use `explain(...)`. It returns the redacted text plus a structured explanation listing each span that was identified.

```java
import ai.philterd.philter.model.ExplainResponse;
import ai.philterd.philter.model.Span;

ExplainResponse response = client.explain("my-context", "default",
        "His name is George Washington and his SSN is 123-45-6789.");

System.out.println("Redacted: " + response.getFilteredText());

for (Span span : response.getExplanation().getAppliedSpans()) {
    System.out.printf("Found %s \"%s\" at [%d, %d) -> replaced with \"%s\" (confidence %.2f)%n",
            span.getFilterType(),
            span.getText(),
            span.getCharacterStart(),
            span.getCharacterEnd(),
            span.getReplacement(),
            span.getConfidence());
}

// Spans that matched but were ignored (e.g. by ignore lists) are available too:
response.getExplanation().getIgnoredSpans().forEach(s ->
        System.out.println("Ignored: " + s.getText()));
```

Pass `true` as the last argument of `explain(context, policyName, filename, text, sign)` to ask for a signed response. `getSignature()` returns the signature, and `getResponseBody()` the JSON exactly as Philter sent it, which is what the signature covers. See [Signed responses](redacting-text.md#signed-responses).

