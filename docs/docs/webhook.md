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

// Stop delivery. Succeeds when no webhook is set.
client.removeWebhook();
```

`getWebhook` requires the `webhooks:read` scope; `setWebhook` and `removeWebhook` require `webhooks:write`. None requires an administrator for your own webhook.

`setWebhook` replaces any URL and secret already set. Philter refuses, with an HTTP 400 raised as a `ClientException` carrying its reason, and saves nothing for:

- a missing URL or secret;
- a URL that is not `http` or `https`;
- a host the administrator's webhook destination allowlist does not permit, or, with no allowlist, a private or loopback address;
- a secret shorter than 16 characters.

Setting and removing a webhook are recorded in Philter's audit log as `webhook_configured` and `webhook_removed`. The URL and secret are not recorded.

For the events Philter sends and how to verify their signatures, see Philter's [webhook documentation](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/api/webhooks.md).

## Another user's webhook

Each call has an overload taking an `owner` username as its last argument, for an administrator to act on another user's webhook. See [Owners and Pagination](owners-and-pagination.md#acting-on-another-users-data) for the conditions.

```java
Webhook theirs = client.getWebhook("someone@example.com");
client.setWebhook(url, secret, "someone@example.com");
client.removeWebhook("someone@example.com");
```
