# Limits and Rules

`getLimits()` returns the limits and rules Philter enforces, and what the caller may do. Read them instead of hard-coding them, for example to check a name or a password before sending it, so your client stays right when a deployment changes a setting or Philter changes a rule.

```java
import ai.philterd.philter.model.Limits;

Limits limits = client.getLimits();

// Check input before sending it.
if (description.length() > limits.getPolicies().getDescriptionMaxLength()) {
    // Tell the person the description is too long.
}

// Offer only what the caller may do.
boolean canCreateContext = limits.getCaller().getContextCount() < limits.getContexts().getMaxPerUser();
boolean showAllUsers = limits.getCaller().isCrossUserAccess();
```

Any valid API key can call it, whatever its scopes, including a session key that must first change its password or enroll in MFA. Fields a later Philter adds are ignored.

| Group | Getters |
|-------|---------|
| `getRequests()` | `getMaxDocumentBytes()` for `filter` and `explain`, `getMaxBodyBytes()` for any other request body, `getDefaultPageSize()`, and `getMaxPageSize()` for listings. |
| `getPassword()` | `getMinCharacters()` and `getMaxBytes()` (in UTF-8). |
| `getNames()` | The rule for custom list names, context names, legal hold references, and usernames: `getForbiddenCharacters()`, `isControlCharactersForbidden()`, `getReservedNames()`, and `getRule()`, the rule in words. |
| `getPolicies()` | `getNameMaxLength()`, `getNamePattern()`, `getReservedNamePrefix()` (`managed_`), `getReservedNames()`, `getDefaultPolicyName()`, `getDescriptionMaxLength()`, and `getNotesMaxLength()`. |
| `getCustomLists()` | `getMaxItems()` and `getItemMaxLength()`. |
| `getRedactLists()` | `getMaxTerms()` and `getTermMaxLength()`, for each of the always-redact and never-redact lists. |
| `getContexts()` | `getMaxPerUser()`, and `getMaxEntries()`, beyond which adding an entry evicts the least-read one. |
| `getWebhook()` | `getSecretMinLength()`. |
| `getLegalHolds()` | `getScopeTypes()`. |
| `getUsers()` | `getRoles()` and `getReservedUsernames()`. |
| `getAuditExport()` | `getMaxWindowDays()`, `getDefaultPageSize()`, and `getMaxPageSize()`. |
| `getSessionKeys()` | `getIdleTimeoutMinutes()` and `getMaxLifetimeMinutes()`. |
| `getCaller()` | `getRole()`, `getContextCount()`, `isCrossUserAccess()` (may use `owner` and all-users listings), and `isLedgerDeletion()` (may delete and purge ledger chains). |

The values are the deployment's: several, such as the request sizes and the session key lifetimes, are settings an administrator can change. For what each means in Philter, see Philter's [limits documentation](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/api/limits_api.md).
