# Re-identifying Redacted Values

When a policy uses a reversible strategy (such as encryption), previously redacted values can be re-identified.

```java
import ai.philterd.philter.model.ReidentifyRequest;
import java.util.List;

ReidentifyRequest request = new ReidentifyRequest();
request.setPolicyName("my-policy");
request.setReason("authorized support investigation");
request.setStrategy("encryption");
request.setValues(List.of("<redacted-or-tokenized-value>"));

String result = client.reidentify("owner-id", request);
```
