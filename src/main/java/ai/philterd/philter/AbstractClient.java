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
package ai.philterd.philter;

import ai.philterd.philter.model.exceptions.ClientException;
import ai.philterd.philter.model.exceptions.ServiceUnavailableException;
import ai.philterd.philter.model.exceptions.UnauthorizedException;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

public abstract class AbstractClient {

    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String SERVICE_UNAVAILABLE = "Service unavailable";

    /**
     * Executes a call and returns its response body, translating non-successful
     * responses into the appropriate exception.
     * @param call The {@link Call} to execute.
     * @param <T> The response body type.
     * @return The response body.
     * @throws IOException Thrown if the request cannot be executed.
     */
    protected <T> T execute(final Call<T> call) throws IOException {

        final Response<T> response = call.execute();

        if (response.isSuccessful()) {
            return response.body();
        }

        throw toException(response.code());

    }

    /**
     * Executes a call that has no meaningful response body, translating
     * non-successful responses into the appropriate exception.
     * @param call The {@link Call} to execute.
     * @throws IOException Thrown if the request cannot be executed.
     */
    protected void executeVoid(final Call<?> call) throws IOException {

        final Response<?> response = call.execute();

        if (!response.isSuccessful()) {
            throw toException(response.code());
        }

    }

    /**
     * Maps an HTTP status code to a client exception.
     * @param code The HTTP status code.
     * @return A {@link RuntimeException} describing the error.
     */
    protected RuntimeException toException(final int code) {

        if (code == 401) {
            return new UnauthorizedException(UNAUTHORIZED);
        } else if (code == 503) {
            return new ServiceUnavailableException(SERVICE_UNAVAILABLE);
        } else {
            return new ClientException("Unknown error: HTTP " + code);
        }

    }

}
