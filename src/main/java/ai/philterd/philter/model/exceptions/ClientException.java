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
 * <p>Where refusals share a status, Philter's error body carries a machine-readable {@code reason}:</p>
 * <ul>
 *   <li>{@code createContext}, {@code 409}: {@code context_exists} (the caller already has a context with
 *   that name, even at the limit) or {@code context_limit_reached} (the caller already has the most
 *   contexts a user may have).</li>
 *   <li>{@code savePolicy}, {@code 409}: {@code policy_exists} (the owner already has a policy with that
 *   name; replace it with {@code replacePolicy}).</li>
 *   <li>{@code replacePolicy}, {@code 409}: {@code policy_changed} (the policy changed concurrently;
 *   reload it and retry).</li>
 *   <li>{@code deletePolicy}, {@code 409}: {@code policy_default} (the {@code default} policy cannot be
 *   deleted).</li>
 *   <li>{@code saveList}, {@code 409}: {@code list_exists} (the owner already has a list with that name;
 *   replace it with {@code replaceList}).</li>
 *   <li>{@code createHold}, {@code 409}: {@code hold_exists} (a hold with that reference exists) or
 *   {@code operation_in_progress} (an evidence or hold operation for the owner is active or needs
 *   recovery). {@code deleteHold}, {@code 409}: {@code operation_in_progress}.</li>
 *   <li>{@code signIn}, {@code 429}: {@code locked} (the username is locked after repeated failures) or
 *   {@code rate_limited} (the client address is over the sign-in rate limit), raised as
 *   {@link SignInLockedException} and {@link SignInRateLimitedException}.</li>
 *   <li>{@code completeSignIn}, {@code 429}: {@code rate_limited}, raised as
 *   {@link SignInRateLimitedException}. The MFA step has no username lockout.</li>
 * </ul>
 */
public class ClientException extends RuntimeException {

    private final int statusCode;
    private final String errorMessage;
    private final String reason;

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
        super(message);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.reason = reason;
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

}
