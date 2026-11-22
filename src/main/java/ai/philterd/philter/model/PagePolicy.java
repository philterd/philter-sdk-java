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
 * Assigns a policy to a set of pages of a PDF, sent to Philter as a {@code page_policy} parameter,
 * {@code <pages>:<policy>}. Requires Philter 4.1.0 or later.
 *
 * <p>Pages are numbered from 1. A page set is single pages ({@code 3}), ranges ({@code 2-5}), and
 * open-ended ranges ({@code 12-}, page 12 to the end), separated by commas, such as {@code 2-5,8}.
 * Philter validates the page set; the client sends it as given.</p>
 */
public final class PagePolicy {

    private final String pages;
    private final String policyName;

    /**
     * @param pages The page set, such as {@code 2-5,8} or {@code 12-}.
     * @param policyName The name of the policy that redacts those pages.
     */
    public PagePolicy(String pages, String policyName) {
        if (pages == null || pages.isBlank()) {
            throw new IllegalArgumentException("A page policy needs a page set.");
        }
        if (policyName == null || policyName.isBlank()) {
            throw new IllegalArgumentException("A page policy needs a policy name.");
        }
        this.pages = pages;
        this.policyName = policyName;
    }

    public String getPages() {
        return pages;
    }

    public String getPolicyName() {
        return policyName;
    }

    /** The {@code page_policy} parameter value, {@code <pages>:<policy>}. */
    @Override
    public String toString() {
        return pages + ":" + policyName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PagePolicy)) {
            return false;
        }
        final PagePolicy other = (PagePolicy) o;
        return pages.equals(other.pages) && policyName.equals(other.policyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pages, policyName);
    }

}
