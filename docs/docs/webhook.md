# Webhook

When an [asynchronous PDF redaction](pdf.md#submitting-a-document-asynchronously) completes or fails, Philter can notify your application with a signed HTTP POST, instead of you polling the document's status. Each user has at most one webhook: a URL and a shared secret Philter signs each request body with. Synchronous redactions never produce a webhook.

```java
import ai.philterd.philter.model.Webhook;

Webhook webhook = client.setWebhook("https://hooks.example.com/philter",
        "a-shared-secret-of-at-least-16-characters");

// The URL, and whether a secret is set. The secret is never returned.
Webhook current = client.getWebhook();
String url = current.getUrl();           // null when no webhook is set
boolean hasSecret = current.isSecretSet();

// Change the URL and keep the secret already set.
client.setWebhook("https://hooks.example.com/philter/v2", null);

// Stop delivery. Succeeds when no webhook is set.
client.removeWebhook();
```

`getWebhook` and `getWebhookDeliveries` require the `webhooks:read` scope; `setWebhook`, `testWebhook`, and `removeWebhook` require `webhooks:write`. None requires an administrator for your own webhook.

`setWebhook` sets the URL and the secret. With a `null` secret it sends none, and Philter keeps the secret already set, so the URL can change without rotating the secret. Philter refuses, with an HTTP 400 raised as a `ClientException` carrying its reason, and saves nothing for:

- a missing URL, or a missing secret when none is set yet;
- a URL that is not `http` or `https`;
- a host the administrator's webhook destination allowlist (see [Admin Settings](admin-settings.md)) does not permit, or, with no allowlist, a private or loopback address;
- a secret shorter than 16 characters.

Setting, testing, and removing a webhook are recorded in Philter's audit log as `webhook_configured`, `webhook_tested`, and `webhook_removed`. The URL and secret are not recorded.

## Testing a webhook

`testWebhook` sends one signed `WEBHOOK_TEST` event to the webhook now, so you can check the receiver's URL and its signature check without redacting a document. It is signed and checked against the destination allowlist as a real delivery is. The test is sent once, without retries, and is not added to the delivery history.

```java
import ai.philterd.philter.model.WebhookTestResult;

WebhookTestResult result = client.testWebhook();
if (result.isDelivered()) {
    System.out.println("Delivered in " + result.getDurationMillis() + " ms");
} else {
    // The receiver's status, or null when it could not be reached, and why.
    System.out.println(result.getStatusCode() + ": " + result.getError());
}
```

A receiver that refuses the test, or cannot be reached, is a result with `isDelivered()` `false`, not an exception. `getDeliveryId()` is the test's `X-Philter-Delivery-Id`, to find it in the receiver's logs. With no webhook set, Philter answers with an HTTP 409 whose `getReason()` is `webhook_not_set`, raised as a `ClientException`.

Philter waits for the receiver up to its webhook connect and response timeouts (5 and 10 seconds by default) before answering. A client built with `withTimeout` shorter than their sum can end the call with an `IOException` instead of a result.

## Delivery history

`getWebhookDeliveries` lists the events Philter queued for your webhook, newest first, with how sending each one went. Use it to find out why a receiver did not get an event.

```java
import ai.philterd.philter.model.GetWebhookDeliveriesResponse;
import ai.philterd.philter.model.WebhookDelivery;

GetWebhookDeliveriesResponse page = client.getWebhookDeliveries(null, 0, 25);
for (WebhookDelivery delivery : page.getDeliveries()) {
    System.out.println(delivery.getEvent() + " for " + delivery.getDocumentId() + ": " + delivery.getStatus()
            + " after " + delivery.getAttempts() + " attempts" + (delivery.getLastError() == null ? ""
            : " (" + delivery.getLastError() + ")"));
}
```

A delivery's status is `WebhookDelivery.PENDING` (waiting for its next attempt, at `getNextAttemptAt()`), `PROCESSING` (being sent), `DELIVERED`, or `FAILED` (every attempt failed, and Philter stopped trying). Its times are `Instant`s: `getCreatedAt()`, `getUpdatedAt()`, `getNextAttemptAt()`, and `getDeliveredAt()`, the last two `null` when they do not apply. `getTotal()` counts every delivery. `limit` defaults to 25 and is at most 100.

## Events

`WebhookEvents` names the events Philter sends, in the body's `event` field and the `X-Philter-Event` header: `DOCUMENT_REDACTION_COMPLETE`, `DOCUMENT_REDACTION_FAILED`, and `WEBHOOK_TEST`, which only `testWebhook` sends. A receiver should accept a test with a 2xx status and otherwise ignore it. For the request bodies and how to verify their signatures, see Philter's [webhook documentation](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/api/webhooks.md).

## Another user's webhook

Each call has an overload taking an `owner` username as its last argument, for an administrator to act on another user's webhook. See [Owners and Pagination](owners-and-pagination.md#acting-on-another-users-data) for the conditions.

```java
Webhook theirs = client.getWebhook("someone@example.com");
client.setWebhook(url, secret, "someone@example.com");
client.testWebhook("someone@example.com");
client.getWebhookDeliveries("someone@example.com", 0, 25);
client.removeWebhook("someone@example.com");
```
