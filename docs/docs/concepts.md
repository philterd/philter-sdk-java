# Concepts

- **Policy**: A named set of rules that tells Philter which entity types to find (names, SSNs, email addresses, dates, and so on) and what *filter strategy* to apply to each (redact, replace, mask, encrypt, hash, and others). Every filter request names the policy to apply; Philter ships a policy named `default`. Policies are authored as JSON; this client treats the policy body as an opaque JSON string.
- **Context**: An arbitrary label used to group requests (for example, by tenant or job). It is passed on each filter request and is echoed back on the response.
- **Document ID**: Philter assigns an identifier to each filtered document and returns it in the `x-document-id` response header; the client exposes it on the response object.
