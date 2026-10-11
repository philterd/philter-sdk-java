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
 * Philter answered with an HTTP 503: it cannot serve the request right now. Try again later.
 *
 * <p>Like {@link ClientException}, it carries the status, Philter's message and reason, and the request id,
 * through {@link PhilterError}. It is not a {@code ClientException}, so a {@code catch} of one does not catch the
 * other.</p>
 */
public class ServiceUnavailableException extends RuntimeException implements PhilterError {

    private final String errorMessage;
    private final String reason;
    private final String requestId;

    public ServiceUnavailableException(String message) {
        this(message, null);
    }

    /**
     * As above, carrying the response's request id, which is appended to the message.
     * @param message The message.
     * @param requestId The response's {@code X-Request-Id}, or {@code null} when it had none.
     */
    public ServiceUnavailableException(String message, String requestId) {
        this(message, null, null, requestId);
    }

    /**
     * An exception built from Philter's error body.
     * @param message The message. The request id, when there is one, is appended to it.
     * @param errorMessage The {@code message} field of Philter's JSON error body, or {@code null}.
     * @param reason The {@code reason} field of Philter's JSON error body, or {@code null}.
     * @param requestId The response's {@code X-Request-Id}, or {@code null} when it had none.
     */
    public ServiceUnavailableException(String message, String errorMessage, String reason, String requestId) {
        super(ClientException.withRequestId(message, requestId));
        this.errorMessage = errorMessage;
        this.reason = reason;
        this.requestId = requestId;
    }

    /** Always {@code 503}. */
    @Override
    public int getStatusCode() {
        return 503;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getReason() {
        return reason;
    }

    /**
     * The request id Philter gave the response, from its {@code X-Request-Id} header, or {@code null} when
     * the response had none. The audit events the request caused record the same id as {@code request_id}.
     */
    @Override
    public String getRequestId() {
        return requestId;
    }

}
