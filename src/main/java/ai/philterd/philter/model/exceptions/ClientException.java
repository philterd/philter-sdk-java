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
 * A request Philter refused with a non-successful HTTP status, or a response the client could not use.
 * Act on {@link #getStatusCode()}, {@link #getReason()}, and {@link #getErrorMessage()} rather than
 * parsing {@link #getMessage()}, whose wording is for logs.
 *
 * <p>Philter's error body carries a machine-readable {@code reason} on every error: a specific one where the
 * refusal has one, and otherwise the one its status implies, such as {@code not_found}. {@link ErrorReasons} has a
 * constant for each. The reasons that tell apart refusals sharing a status include:</p>
 * <ul>
 *   <li>{@code createContext}, {@code 409}: {@code context_exists} (the caller already has a context with
 *   that name, even at the limit) or {@code context_limit_reached} (the caller already has the most
 *   contexts a user may have).</li>
 *   <li>{@code savePolicy}, {@code 409}: {@code policy_exists} (the owner already has a policy with that
 *   name; replace it with {@code replacePolicy}).</li>
 *   <li>{@code replacePolicy}, {@code 409}: {@code policy_managed} (a managed policy, whose name begins
 *   with {@code managed_}, cannot be replaced; copy it with {@code copyPolicy} and change the copy) or
 *   {@code policy_changed} (the policy changed since the read whose entity tag you passed, or while the
 *   request was writing; read it again and retry).</li>
 *   <li>{@code deletePolicy}, {@code 409}: {@code policy_default} (the {@code default} policy cannot be
 *   deleted) or {@code policy_managed} (a managed policy cannot be deleted).</li>
 *   <li>{@code copyPolicy}, {@code 409}: {@code policy_exists} (the owner already has a policy with the
 *   new name).</li>
 *   <li>{@code rollbackPolicy}, {@code 409}: {@code policy_managed} (a managed policy cannot be rolled
 *   back) or {@code policy_changed}, as for {@code replacePolicy}.</li>
 *   <li>{@code setPolicyDetails}, {@code 409}: {@code policy_managed} (a managed policy's description and
 *   notes cannot be changed) or {@code policy_changed}, as for {@code replacePolicy}.</li>
 *   <li>{@code saveList}, {@code 409}: {@code list_exists} (the owner already has a list with that name;
 *   replace it with {@code replaceList}).</li>
 *   <li>{@code createHold}, {@code 409}: {@code hold_exists} (a hold with that reference exists) or
 *   {@code operation_in_progress} (an evidence or hold operation for the owner is active or needs
 *   recovery). {@code deleteHold}, {@code 409}: {@code operation_in_progress}.</li>
 *   <li>{@code getLedgerExport}, {@code 422}: {@code entry_unreadable} (an entry in the chain could not
 *   be read, so the chain is not exported).</li>
 *   <li>{@code signIn}, {@code 429}: {@code locked} (the username is locked after repeated failures) or
 *   {@code rate_limited} (the client address is over the sign-in rate limit), raised as
 *   {@link SignInLockedException} and {@link SignInRateLimitedException}.</li>
 *   <li>{@code completeSignIn}, {@code 429}: {@code rate_limited}, raised as
 *   {@link SignInRateLimitedException}. The MFA step has no username lockout.</li>
 * </ul>
 */
public class ClientException extends RuntimeException implements PhilterError {

    private final int statusCode;
    private final String errorMessage;
    private final String reason;
    private final String requestId;
    private final String field;

    /**
     * An exception not caused by a non-successful HTTP status, such as a response missing a header the
     * client needs. Its status code is {@code 0}.
     * @param message The message.
     */
    public ClientException(String message) {
        this(message, 0, null);
    }

    /**
     * An exception caused by a non-successful HTTP status.
     * @param message The message.
     * @param statusCode The HTTP status code.
     * @param errorMessage The {@code message} field of Philter's JSON error body, or {@code null}.
     */
    public ClientException(String message, int statusCode, String errorMessage) {
        this(message, statusCode, errorMessage, null);
    }

    /**
     * An exception caused by a non-successful HTTP status whose error body carries a reason.
     * @param message The message.
     * @param statusCode The HTTP status code.
     * @param errorMessage The {@code message} field of Philter's JSON error body, or {@code null}.
     * @param reason The {@code reason} field of Philter's JSON error body, or {@code null}.
     */
    public ClientException(String message, int statusCode, String errorMessage, String reason) {
        this(message, statusCode, errorMessage, reason, null);
    }

    /**
     * An exception caused by a Philter response, carrying the response's request id. The id is appended to
     * the message, so a logged stack trace carries it.
     * @param message The message.
     * @param statusCode The HTTP status code, or {@code 0} when the status was successful.
     * @param errorMessage The {@code message} field of Philter's JSON error body, or {@code null}.
     * @param reason The {@code reason} field of Philter's JSON error body, or {@code null}.
     * @param requestId The response's {@code X-Request-Id}, or {@code null} when it had none.
     */
    public ClientException(String message, int statusCode, String errorMessage, String reason, String requestId) {
        this(message, statusCode, errorMessage, reason, requestId, null);
    }

    /**
     * As above, naming the invalid parameter or body field Philter reported.
     * @param message The message.
     * @param statusCode The HTTP status code, or {@code 0} when the status was successful.
     * @param errorMessage The {@code message} field of Philter's JSON error body, or {@code null}.
     * @param reason The {@code reason} field of Philter's JSON error body, or {@code null}.
     * @param requestId The response's {@code X-Request-Id}, or {@code null} when it had none.
     * @param field The {@code field} field of Philter's JSON error body, or {@code null}.
     */
    public ClientException(String message, int statusCode, String errorMessage, String reason, String requestId,
                           String field) {
        super(withRequestId(message, requestId));
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.reason = reason;
        this.requestId = requestId;
        this.field = field;
    }

    /** The message with the request id appended, when there is one. */
    static String withRequestId(final String message, final String requestId) {
        return requestId == null || requestId.isBlank() ? message : message + " (request id: " + requestId + ")";
    }

    /**
     * The HTTP status code, such as {@code 404}, or {@code 0} when the exception was not caused by a
     * non-successful status.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * The {@code message} field of Philter's JSON error body, read from the whole body rather than the
     * truncated exception message, or {@code null} when the body is not JSON or has no such field. It is
     * written for people, so show it rather than branching on its wording.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * The {@code reason} field of Philter's JSON error body: a stable, machine-readable value that tells
     * apart refusals sharing a status, such as {@code context_limit_reached}. {@code null} when the body
     * has none, is not JSON, or is empty. Branch on this rather than on the error message.
     */
    public String getReason() {
        return reason;
    }

    /**
     * The request id Philter gave the response, from its {@code X-Request-Id} header, or {@code null} when
     * the response had none or the exception was not caused by a response. Philter records the same id as
     * {@code request_id} on the audit events the request caused, so an administrator can find the request
     * in the audit log with it.
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * The parameter or body field Philter found invalid, from the {@code field} field of its JSON error body, such
     * as {@code sort}, {@code password}, or {@code email}. Philter gives it on some {@code 400} responses, whose
     * reason is {@link ErrorReasons#INVALID_REQUEST}, so a form can mark the field. {@code null} when Philter
     * does not name one.
     */
    public String getField() {
        return field;
    }

}
