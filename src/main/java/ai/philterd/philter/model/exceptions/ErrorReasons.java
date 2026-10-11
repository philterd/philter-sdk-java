/*******************************************************************************
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package ai.philterd.philter.model.exceptions;

/**
 * The reasons Philter gives for refusing a request, as {@link ClientException#getReason()} and
 * {@link PhilterError#getReason()} return them. Philter puts a reason on every error: a specific one where the
 * refusal has one, and otherwise the one its status implies. Switch on the reason rather than on the message, which
 * is written for people and may change, or on the status alone, which several refusals share.
 *
 * <pre>{@code
 * try {
 *     client.savePolicy(name, json);
 * } catch (ClientException e) {
 *     if (ErrorReasons.POLICY_EXISTS.equals(e.getReason())) {
 *         // Choose another name, or replace the policy.
 *     } else if (ErrorReasons.POLICY_MANAGED.equals(e.getReason())) {
 *         // Copy the managed policy instead.
 *     } else {
 *         throw e;
 *     }
 * }
 * }</pre>
 *
 * <p>Compare with {@code ErrorReasons.X.equals(e.getReason())}: the reason is {@code null} when the response was not
 * Philter's JSON, such as a proxy's error page. A Philter version may add reasons, so handle one you do not
 * recognize by its status.</p>
 */
public final class ErrorReasons {

    // Reasons a status implies, given when a refusal has no more specific reason.

    /**
     * 400: the request is not valid. {@link ClientException#getField()} names the invalid parameter or field when
     * Philter knows it.
     */
    public static final String INVALID_REQUEST = "invalid_request";

    /**
     * 401: the API key is missing, malformed, or unknown, or is a revoked long-lived key. Sign-in also gives it
     * for a wrong username or password, without saying which.
     */
    public static final String INVALID_CREDENTIALS = "invalid_credentials";

    /** 403: the request is not allowed. */
    public static final String FORBIDDEN = "forbidden";

    /**
     * 404: nothing with that name, id, or path exists, or the {@code owner} does not exist or may not be reached.
     * The two read the same.
     */
    public static final String NOT_FOUND = "not_found";

    /** 405: the path does not take this HTTP method. */
    public static final String METHOD_NOT_ALLOWED = "method_not_allowed";

    /** 406: Philter cannot answer in the requested format. */
    public static final String NOT_ACCEPTABLE = "not_acceptable";

    /** 409: the request conflicts with the current state. */
    public static final String CONFLICT = "conflict";

    /** 413: the request body is larger than Philter accepts. */
    public static final String PAYLOAD_TOO_LARGE = "payload_too_large";

    /** 415: the request body's content type is not one the endpoint takes. */
    public static final String UNSUPPORTED_MEDIA_TYPE = "unsupported_media_type";

    /**
     * 429: too many requests. Sign-in gives it for an address over the sign-in rate limit, as {@link
     * SignInRateLimitedException}.
     */
    public static final String RATE_LIMITED = "rate_limited";

    /** 500: Philter failed to handle the request. */
    public static final String INTERNAL_ERROR = "internal_error";

    /** 503: Philter cannot serve the request right now. Try again later. */
    public static final String SERVICE_UNAVAILABLE = "service_unavailable";

    // Authentication and authorization.

    /**
     * 401: the session key has ended: it passed its idle timeout or maximum lifetime, was revoked, or signed out.
     * Sign in again.
     */
    public static final String SESSION_EXPIRED = "session_expired";

    /** 401: the API key's user is deactivated. */
    public static final String USER_DEACTIVATED = "user_deactivated";

    /** 403: the API key does not hold the scope the endpoint requires. */
    public static final String MISSING_SCOPE = "missing_scope";

    /** 403: the operation requires an administrator. */
    public static final String ADMIN_REQUIRED = "admin_required";

    /** 403: the operation is turned off in this deployment, such as ledger deletion. */
    public static final String FEATURE_DISABLED = "feature_disabled";

    /** 403: the session key may only change its user's password until it is changed. */
    public static final String PASSWORD_CHANGE_REQUIRED = "password_change_required";

    /** 403: the session key may only enroll its user in MFA until enrollment is confirmed. */
    public static final String MFA_ENROLLMENT_REQUIRED = "mfa_enrollment_required";

    /** 403: the calling key cannot create a key, widen a key's scopes, or grant scopes it does not hold. */
    public static final String SCOPE_NOT_HELD = "scope_not_held";

    // Sign-in, passwords, and MFA.

    /** 429: the username is locked after repeated failed sign-ins, as {@link SignInLockedException}. */
    public static final String LOCKED = "locked";

    /** 403: the current password is not correct. */
    public static final String WRONG_PASSWORD = "wrong_password";

    /** 400 when confirming MFA enrollment, 403 when removing it: the MFA code is not valid. */
    public static final String INVALID_CODE = "invalid_code";

    /** 403: the user's MFA is locked after repeated bad codes; an administrator must unlock it. */
    public static final String MFA_LOCKED = "mfa_locked";

    /** 409: MFA is not turned on in this deployment. */
    public static final String MFA_UNAVAILABLE = "mfa_unavailable";

    /** 409: the user is already enrolled in MFA. */
    public static final String MFA_ALREADY_ENROLLED = "mfa_already_enrolled";

    /** 409: the user is not enrolled in MFA. */
    public static final String MFA_NOT_ENROLLED = "mfa_not_enrolled";

    /** 409: only a session key can sign itself out. */
    public static final String NOT_A_SESSION_KEY = "not_a_session_key";

    // Users and API keys.

    /**
     * 409: the caller cannot do this to their own user or to the key making the request, such as deactivating
     * themselves or revoking the calling key.
     */
    public static final String SELF_ACTION_REFUSED = "self_action_refused";

    /** 409: the user is the last active administrator. */
    public static final String LAST_ADMIN = "last_admin";

    /** 409: a user with that username already exists, active or deactivated. */
    public static final String ALREADY_EXISTS = "already_exists";

    /** 409: another request changed the same thing first, such as the password. Read it again and retry. */
    public static final String CHANGED_CONCURRENTLY = "changed_concurrently";

    // Policies.

    /** 409: the owner already has a policy with that name. Replace it with {@code replacePolicy}. */
    public static final String POLICY_EXISTS = "policy_exists";

    /**
     * 409: a managed policy, whose name begins with {@code managed_}, cannot be changed or deleted. Copy it with
     * {@code copyPolicy}.
     */
    public static final String POLICY_MANAGED = "policy_managed";

    /** 409: the policy changed after it was read. Read it again and retry. */
    public static final String POLICY_CHANGED = "policy_changed";

    /** 409: the {@code default} policy cannot be deleted. */
    public static final String POLICY_DEFAULT = "policy_default";

    // Contexts, lists, and redaction.

    /** 409: the owner already has a context with that name. */
    public static final String CONTEXT_EXISTS = "context_exists";

    /** 409: the owner already has the most contexts a user may have. */
    public static final String CONTEXT_LIMIT_REACHED = "context_limit_reached";

    /** 409: the context has queued or running redactions. */
    public static final String CONTEXT_IN_USE = "context_in_use";

    /** 409: the owner already has a custom list with that name. Replace it with {@code replaceList}. */
    public static final String LIST_EXISTS = "list_exists";

    /**
     * 409: a redact list changed after the revision passed to {@code replaceRedactList}. Nothing was written: read
     * the list again, apply the change to what you read, and retry with the new revision.
     */
    public static final String REDACT_LIST_CHANGED = "redact_list_changed";

    /** 409: the document is still being redacted. */
    public static final String DOCUMENT_NOT_READY = "document_not_ready";

    /** 410: the document's redaction failed, so there is nothing to download. */
    public static final String DOCUMENT_FAILED = "document_failed";

    // Evidence, ledger, signing, and webhook.

    /** 409: a legal hold with that reference exists. */
    public static final String HOLD_EXISTS = "hold_exists";

    /** 409: an evidence or hold operation for the owner is active or needs recovery. */
    public static final String OPERATION_IN_PROGRESS = "operation_in_progress";

    /** 422: an entry in the ledger chain could not be read, so the chain is not exported. */
    public static final String ENTRY_UNREADABLE = "entry_unreadable";

    /** 409: the signing key is managed outside Philter. */
    public static final String EXTERNALLY_MANAGED = "externally_managed";

    /** 409: no webhook is set, so there is nothing to test. */
    public static final String WEBHOOK_NOT_SET = "webhook_not_set";

    private ErrorReasons() {
    }

}
