# Audit Log

Philter records security-relevant actions and redaction activity in its audit log. `getAuditLog` reads it as typed events, and `exportAuditLog` exports it as CSV for a range of whole days. Both require the `audit:read` scope and an administrator, and each read or export is itself recorded in the log.

## Reading the audit log

```java
import ai.philterd.philter.model.AuditEvent;
import ai.philterd.philter.model.GetAuditLogResponse;
import java.time.Instant;

// The most recent events, unfiltered.
GetAuditLogResponse recent = client.getAuditLog();

// Policy deletions in September 2026, 100 at a time.
GetAuditLogResponse page = client.getAuditLog("policy_deleted",
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"), null, 0, 100);

long total = page.getTotal();
for (AuditEvent e : page.getEvents()) {
    System.out.println(e.getTimestamp() + " " + e.getEvent() + " " + e.getDetails());
}
```

Events are returned most recent first. Every filter may be `null`:

- `event` - one event type, such as `policy_deleted`. A name Philter does not record is an HTTP 400 rather than an empty page.
- `from` and `to` - `from` is inclusive and `to` exclusive, so a day's events are `from` that day `to` the next.
- `owner` - the username of the acting user. Your own username always works; another user's requires `ADMIN_CROSS_USER_ACCESS_ENABLED=true`, and a username that does not exist is an HTTP 404. Some events record the affected entity rather than the acting user, so an `owner` filter does not return them.
- `offset` and `limit` - page through the matches (default 25, at most 100). `getTotal()` is the number of events matching the filters.

Every field of an `AuditEvent` except `getTimestamp()` and `getEvent()` is `null` for an event that did not record it. Audit events never carry redacted values.

`getSource()` says where an event came from: `api` for an event a request caused, including a redaction the asynchronous worker completes later, or `system` for one Philter recorded on its own, such as at startup or when a session key expires in the background. `getClientIpAddress()` is the address of the client whose request caused the event (the connection's address or, for a request from a trusted proxy, the address its `X-Forwarded-For` names), and `null` for an event with no request behind it. Events recorded by a Philter without philterd/philter#149 have no source, and some hold `api` or `system` in the client address instead. For the list of event types and what each field holds, see Philter's [auditing documentation](https://github.com/philterd/philter/blob/main/docs/docs/auditing.md).

## Exporting as CSV

`exportAuditLog` returns the log as CSV for a range of whole days.

```java
// October 1 through 5, both days included, read in UTC.
AuditLogExport export = client.exportAuditLog(
        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), ZoneId.of("UTC"), null, null);

String csv = export.getCsv();          // or getContent() for the bytes
int rows = export.getRows();
```

The CSV has a header row and the columns `timestamp`, `event`, `request_id`, `api_key_id`, `associated_object`, `client_ip_address`, `source`, and `details`, the same fields as an `AuditEvent`. A Philter without philterd/philter#149 has no `source` column.

`from` and `to` are read in the given time zone, or in the Philter server's time zone when it is `null` or omitted. `getTimeZone()` reports the zone Philter used. Timestamps in the CSV are UTC whatever the zone. A range may span at most 31 days (`to` at most 30 days after `from`).

### Paging

An export returns one page, most recent first: up to `limit` events (default 100, at most 1000), starting `offset` events into the range. When `isTruncated()` is `true`, more events remain; request the next page with `getNextOffset()`.

```java
LocalDate from = LocalDate.of(2026, 9, 1);
LocalDate to = LocalDate.of(2026, 9, 30);
ZoneId utc = ZoneId.of("UTC");

AuditLogExport page = client.exportAuditLog(from, to, utc, null, 1000);
// write page.getContent()
while (page.isTruncated()) {
    page = client.exportAuditLog(from, to, utc, page.getNextOffset(), 1000);
    // write page.getContent()
}
```

Every page starts with the CSV header row, so drop it from each page after the first when concatenating. Paging a range of past days returns each event exactly once. A range that includes the current day can repeat events across pages, because new events push older ones onto later pages; export complete days to avoid this.

### Errors

Philter rejects a reversed range, a range longer than 31 days, or an unknown time zone with an HTTP 400, raised as a `ClientException` whose message carries Philter's reason. A caller who is not an administrator gets an HTTP 403, also as a `ClientException`. See [Error Handling](error-handling.md).
