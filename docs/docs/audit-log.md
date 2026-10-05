# Audit Log

Philter records security-relevant actions and redaction activity in its audit log. `exportAuditLog` exports it as CSV for a range of whole days. The calling key must hold the `audit:read` scope and belong to an administrator, and each export is itself recorded in the log.

```java
// October 1 through 5, both days included, read in UTC.
AuditLogExport export = client.exportAuditLog(
        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), ZoneId.of("UTC"), null, null);

String csv = export.getCsv();          // or getContent() for the bytes
int rows = export.getRows();
```

`from` and `to` are read in the given time zone, or in the Philter server's time zone when it is `null` or omitted. `getTimeZone()` reports the zone Philter used. Timestamps in the CSV are UTC whatever the zone. A range may span at most 31 days (`to` at most 30 days after `from`).

## Paging

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

## Errors

Philter rejects a reversed range, a range longer than 31 days, or an unknown time zone with an HTTP 400, raised as a `ClientException` whose message carries Philter's reason. A caller who is not an administrator gets an HTTP 403, also as a `ClientException`. See [Error Handling](error-handling.md).
