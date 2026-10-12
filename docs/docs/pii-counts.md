# PII Counts

Philter can record how often each PII type is found, per context and UTC day, for differential-privacy reporting with [Philter Diffuse](https://github.com/philterd/philter-diffuse). `getPiiCounts` reads those counts through Philter's API, so a reporting tool needs an API key rather than a connection to Philter's database. The counts are counts only: they never carry any PII. These methods need Philter 4.0 or later.

Reading the counts requires an administrator and an API key holding the `pii-counts:read` scope, since the counts cover the whole deployment. Each call is recorded in the audit log as a `pii_counts_retrieved` event.

## Reading the counts

```java
import ai.philterd.philter.model.GetPiiCountsResponse;
import ai.philterd.philter.model.PiiCount;
import java.time.LocalDate;

// September 2026 for one context, every page.
GetPiiCountsResponse september = client.getAllPiiCounts(
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "claims-intake");

if (!september.isRecordingEnabled()) {
    System.out.println("Philter is not recording PII counts now.");
}

for (PiiCount count : september.getCounts()) {
    System.out.println(count.getDay() + " " + count.getContext() + ": " + count.getCounts()
            + " across " + count.getTotalDocuments() + " documents");
}
```

`getAllPiiCounts` reads page after page until it has every entry matching the filters. Philter keeps counting while the pages are read, so for a stable result end the range before the current UTC day. To read one page at a time, use `getPiiCounts(from, to, context, offset, limit)`. Every argument may be `null`:

- `from` and `to` - the first and last UTC day to return, both inclusive. `from` after `to` is an HTTP 400.
- `context` - one context, or `null` for every context.
- `offset` and `limit` - page through the matches (default 25, at most 100). `getTotal()` is the number of entries matching the filters.

Entries are returned most recent day first, then by context. Each `PiiCount` has:

- `getContext()` - the context the redactions ran in.
- `getDay()` - the UTC day the redactions ran on.
- `getCounts()` - for each PII type, such as `SSN`, how many redactions that day found at least one of it. A redaction adds at most one to each type, however many of that type it found.
- `getTotalDocuments()` - how many redactions that day found any PII. Redactions that found no PII are not counted.
- `getUpdatedAt()` - when a redaction was last counted.

## When recording is off

Philter records counts only while the `diffuseCountsEnabled` [admin setting](admin-settings.md) is on, and it is off by default. `isRecordingEnabled()` says whether Philter is recording now. When it is `false`, an empty result means nothing is being recorded, not that no PII was found. This is not an error: counts recorded while recording was on are still returned.

## Errors

- A key without the `pii-counts:read` scope gets an HTTP 403 with the `missing_scope` reason.
- A key whose owner is not an administrator gets an HTTP 403 with the `admin_required` reason.
- An invalid date range gets an HTTP 400 with the `invalid_request` reason.

Each is thrown as a `ClientException` carrying the reason and Philter's request id. See [Error Handling](error-handling.md).

For the endpoint itself, see Philter's [PII Counts API documentation](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/api/pii_counts_api.md).
