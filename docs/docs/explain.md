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
```

`getClassification()` gives what a span was classified as beyond its filter type, and is `null` when the filter gives none. For a driver's license number it is the state whose format matched, such as `UTAH`; several states share a format, so treat it as a candidate rather than the issuing state. For a custom identifier or custom dictionary it is the classification set in the policy (`custom-identifier` by default for an identifier). PhEye spans have no classification.

Pass `true` as the last argument of `explain(context, policyName, filename, text, sign)` to ask for a signed response. `getSignature()` returns the signature, and `getResponseBody()` the JSON exactly as Philter sent it, which is what the signature covers. See [Signed responses](redacting-text.md#signed-responses).

