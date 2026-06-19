# End-to-End Example

```java
import ai.philterd.philter.PhilterClient;
import ai.philterd.philter.model.ExplainResponse;
import ai.philterd.philter.model.FilterResponse;
import ai.philterd.philter.model.Span;

public class RedactionExample {

    public static void main(String[] args) throws Exception {

        PhilterClient client = new PhilterClient.PhilterClientBuilder()
                .withEndpoint("https://localhost:8080")
                .withApiKey("your-api-key")
                .build();

        final String text = "Patient George Washington (SSN 123-45-6789) was seen on 03/14.";

        // 1. Redact.
        FilterResponse filtered = client.filter("clinic", "default", text);
        System.out.println("Redacted: " + filtered.getFilteredText());
        System.out.println("Document ID: " + filtered.getDocumentId());

        // 2. Explain what was found.
        ExplainResponse explained = client.explain("clinic", "default", text);
        for (Span span : explained.getExplanation().getAppliedSpans()) {
            System.out.printf("  %-12s %-20s -> %s%n",
                    span.getFilterType(), span.getText(), span.getReplacement());
        }
    }

}
```
