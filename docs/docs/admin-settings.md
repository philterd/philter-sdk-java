# Admin Settings

The admin settings apply to the whole deployment: differential-privacy count recording, output signing, the webhook destination allowlist, and Phield publishing. Reading them requires the `settings:read` scope, and changing them `settings:write`. Both also require an administrator.

```java
import ai.philterd.philter.model.AdminSettings;

AdminSettings settings = client.getAdminSettings();

settings.isDiffuseCountsEnabled();   // record PII counts for differential-privacy reporting
settings.isSigningEnabled();         // sign every text filter and explain response
settings.getWebhookAllowlist();      // where webhooks may point; empty allows any public address
settings.isPhieldEnabled();
settings.getPhieldUrl();
settings.getPhieldSourceId();
settings.getPhieldOrganization();
settings.isPhieldApiKeySet();        // the key itself is never returned
```

## Changing settings

Set only the settings to change. The rest are not sent, and Philter leaves them as they are.

```java
import ai.philterd.philter.model.UpdateAdminSettingsRequest;

UpdateAdminSettingsRequest request = new UpdateAdminSettingsRequest();
request.setSigningEnabled(true);
request.setWebhookAllowlist("hooks.example.com, 10.4.0.0/16");

AdminSettings saved = client.updateAdminSettings(request);

for (String warning : saved.getWarnings()) {
    System.out.println(warning);
}
```

- `webhookAllowlist` is comma-separated hostnames, IP addresses, and CIDR ranges. An empty string allows any public address.
- `phieldApiKey` sets the Phield API key. An empty string removes it.
- `phieldSourceId` and `phieldOrganization` are set to `philter` when blank.

Philter validates every value before saving any, and changes nothing if one is invalid. A `webhookAllowlist` entry that is not a hostname, an IP address, or a CIDR range, a `phieldUrl` that is not an absolute `http` or `https` URL, or a request that sets `phieldEnabled` or `phieldUrl` and would leave Phield enabled without a URL is an HTTP 400, raised as a `ClientException` carrying Philter's reason.

An `http` Phield URL with an API key is saved, and `getWarnings()` on the response says the key will be sent in the clear. It is empty on a read.

Each Philter instance caches the settings for up to `ADMIN_SETTINGS_CACHE_TTL_SECONDS`. A change applies at once on the instance that made it, and on other instances when their cache expires.

A change is recorded in Philter's audit log as a `settings_updated` event naming the settings that changed. Their values are not recorded.
