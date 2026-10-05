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
 * Act on {@link #getStatusCode()} and {@link #getErrorMessage()} rather than parsing
 * {@link #getMessage()}, whose wording is for logs.
 */
public class ClientException extends RuntimeException {

    private final int statusCode;
    private final String errorMessage;

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
        super(message);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
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

}
