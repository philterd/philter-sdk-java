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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/**
 * The result of a policy's leftover check: a second detection pass over a redaction's output that reports what is
 * still detectable. Philter runs it when the policy's {@code config.review.leftoverCheck} calls for it. Requires
 * Philter 4.1.0 or later.
 *
 * <p>A check that was not performed checked nothing: the output had no readable text, such as a PDF whose redacted
 * pages were flattened to images, or the check failed. The redacted output is returned either way. Its count is
 * then 0 without meaning that nothing was found.</p>
 *
 * <p>A count of 0 does not mean the output is free of sensitive information. Detection is probabilistic, and
 * results still need validating against your own data.</p>
 */
public class LeftoverCheck {

    /** The {@code X-Philter-Leftovers} value for a check that checked nothing. */
    public static final String NOT_CHECKED = "not-checked";

    @Expose
    @SerializedName("performed")
    private boolean performed;

    @Expose
    @SerializedName("reason")
    private String reason;

    @Expose
    @SerializedName("findingCount")
    private int findingCount;

    @Expose
    @SerializedName("findings")
    private List<LeftoverFinding> findings;

    /** For deserialization. */
    public LeftoverCheck() {
    }

    private LeftoverCheck(final boolean performed, final int findingCount) {
        this.performed = performed;
        this.findingCount = findingCount;
    }

    /**
     * Reads an {@code X-Philter-Leftovers} header value: a count, or {@link #NOT_CHECKED}. Any value that is not a
     * count, including one Philter may add later, is read as not checked rather than refused.
     * @param value The header value, or {@code null} when the header is absent.
     * @return The check, or {@code null} when {@code value} is {@code null}.
     */
    public static LeftoverCheck fromHeader(final String value) {
        if (value == null) {
            return null;
        }
        try {
            final int count = Integer.parseInt(value.trim());
            if (count >= 0) {
                return new LeftoverCheck(true, count);
            }
        } catch (final NumberFormatException e) {
            // Not a count.
        }
        return new LeftoverCheck(false, 0);
    }

    /**
     * Whether the check ran. When it did not, nothing was checked.
     * @return {@code true} when the check ran.
     */
    public boolean isPerformed() {
        return performed;
    }

    /**
     * Why the check was not performed, as {@code explain} reports it: {@code unreadableOutput} or
     * {@code checkFailed}. Other values may be added. {@code null} when the check ran, and always {@code null} for a
     * result read from a response header, which does not say why.
     * @return The reason, or {@code null}.
     */
    public String getReason() {
        return reason;
    }

    /**
     * The number of values the check found still detectable. 0 when the check was not performed.
     * @return The count.
     */
    public int getFindingCount() {
        return findingCount;
    }

    /**
     * Each finding's type, position in the filtered text, and confidence. Only {@code explain} returns findings;
     * a result read from a response header has none.
     * @return The findings, never {@code null}.
     */
    public List<LeftoverFinding> getFindings() {
        return findings == null ? Collections.emptyList() : Collections.unmodifiableList(findings);
    }

    @Override
    public boolean equals(Object o) {
        return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
    }

}
