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
 * What every exception built from a Philter error response carries: {@link ClientException},
 * {@link UnauthorizedException}, and {@link ServiceUnavailableException}. Catch {@code RuntimeException} and test
 * for this interface to handle any Philter error the same way, or catch the exception types to tell them apart.
 */
public interface PhilterError {

    /** The HTTP status code, such as {@code 401}, or {@code 0} when the error was not caused by a status. */
    int getStatusCode();

    /**
     * The {@code message} field of Philter's JSON error body, or {@code null} when the body was empty, not JSON,
     * or had none. It is written for people, so show it rather than branching on its wording.
     */
    String getErrorMessage();

    /**
     * The {@code reason} field of Philter's JSON error body: a stable, machine-readable code, such as
     * {@code session_expired}. {@code null} when the body has none. Branch on this rather than on the message.
     */
    String getReason();

    /** The response's {@code X-Request-Id}, or {@code null} when it had none. */
    String getRequestId();

}
