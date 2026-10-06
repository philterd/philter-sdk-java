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
 * A sign-in refused with an HTTP 429. Philter refuses a sign-in this way for a username locked after
 * repeated failures ({@link SignInLockedException}) or for a client address over the rate limit
 * ({@link SignInRateLimitedException}). Its status code is {@code 429}, its message and error message
 * are both Philter's message, and its {@link #getReason()} is {@code locked} or {@code rate_limited}.
 */
public class SignInThrottledException extends ClientException {

    private final Integer retryAfterSeconds;

    public SignInThrottledException(String message, String reason, Integer retryAfterSeconds) {
        super(message, 429, message, reason);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** The most seconds to wait before trying again, from {@code Retry-After}, or {@code null} if absent. */
    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

}
