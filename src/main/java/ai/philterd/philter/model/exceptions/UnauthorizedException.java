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
 * Philter refused the request with an HTTP 401: the credentials are missing or not accepted, such as an unknown,
 * revoked, or expired key, or at sign-in a wrong username or password.
 */
public class UnauthorizedException extends RuntimeException {

    private final String requestId;

    public UnauthorizedException(String message) {
        this(message, null);
    }

    /**
     * As above, carrying the response's request id, which is appended to the message.
     * @param message The message.
     * @param requestId The response's {@code X-Request-Id}, or {@code null} when it had none.
     */
    public UnauthorizedException(String message, String requestId) {
        super(ClientException.withRequestId(message, requestId));
        this.requestId = requestId;
    }

    /**
     * The request id Philter gave the response, from its {@code X-Request-Id} header, or {@code null} when
     * the response had none. The audit events the request caused record the same id as {@code request_id}.
     */
    public String getRequestId() {
        return requestId;
    }

}
