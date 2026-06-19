# Checking Server Health

`health()` does not require authentication and is useful for readiness checks.

```java
import ai.philterd.philter.model.StatusResponse;

StatusResponse status = client.health();
System.out.println(status.getStatus());             // e.g. "Healthy"
System.out.println(status.getApplicationVersion()); // e.g. "4.0.0"
```
