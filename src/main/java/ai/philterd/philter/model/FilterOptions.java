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
package ai.philterd.philter.model;

import java.util.Objects;

/**
 * Per-request options for filtering and {@code explain}. Start from {@link #defaults()}, which asks for nothing
 * beyond the plain request, and change what you need. Each {@code with} method returns a new instance.
 *
 * <pre>{@code
 * FilterOptions options = FilterOptions.defaults().withLeftoverCheck(true);
 * FilterResponse response = client.filter("ctx", "policy", null, text, false, options);
 * }</pre>
 */
public final class FilterOptions {

    private static final FilterOptions DEFAULTS = new FilterOptions(false);

    private final boolean leftoverCheck;

    private FilterOptions(final boolean leftoverCheck) {
        this.leftoverCheck = leftoverCheck;
    }

    /**
     * Options that ask for nothing beyond the plain request.
     * @return The default options.
     */
    public static FilterOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Whether to request the policy's leftover check, sent as {@code leftover_check=true}. It matters when the
     * policy's {@code config.review.leftoverCheck.mode} is {@code onRequest}. Under {@code always} every request is
     * checked, and {@code false} does not change that. Under {@code off}, Philter refuses {@code true} with a 400.
     * Requires Philter 4.1.0 or later.
     * @param leftoverCheck {@code true} to request the check.
     * @return Options with the setting changed.
     */
    public FilterOptions withLeftoverCheck(final boolean leftoverCheck) {
        return new FilterOptions(leftoverCheck);
    }

    /**
     * Whether these options request the policy's leftover check.
     * @return {@code true} when {@code leftover_check=true} is sent.
     */
    public boolean isLeftoverCheck() {
        return leftoverCheck;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof FilterOptions && leftoverCheck == ((FilterOptions) o).leftoverCheck;
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftoverCheck);
    }

    @Override
    public String toString() {
        return "FilterOptions{leftoverCheck=" + leftoverCheck + "}";
    }

}
