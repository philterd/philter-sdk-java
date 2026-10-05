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

import java.nio.charset.StandardCharsets;

/**
 * One page of the audit log exported as CSV, with the response headers that describe it.
 * Check {@link #isTruncated()} before treating the CSV as the whole range: when it is
 * {@code true}, more events remain and {@link #getNextOffset()} gives the offset to request next.
 */
public class AuditLogExport {

    private final byte[] content;
    private final int rows;
    private final boolean truncated;
    private final Integer nextOffset;
    private final String timeZone;

    public AuditLogExport(byte[] content, int rows, boolean truncated, Integer nextOffset, String timeZone) {
        this.content = content;
        this.rows = rows;
        this.truncated = truncated;
        this.nextOffset = nextOffset;
        this.timeZone = timeZone;
    }

    /** The CSV bytes, UTF-8 encoded. */
    public byte[] getContent() {
        return content;
    }

    /** The CSV as a string. */
    public String getCsv() {
        return new String(content, StandardCharsets.UTF_8);
    }

    /** The number of events in this page, from {@code X-Philter-Export-Rows}. */
    public int getRows() {
        return rows;
    }

    /**
     * Whether more events remain in the range after this page, from {@code X-Philter-Export-Truncated}.
     */
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * The offset to request the next page with, from {@code X-Philter-Export-Next-Offset}, or
     * {@code null} when the export was not truncated.
     */
    public Integer getNextOffset() {
        return nextOffset;
    }

    /** The time zone Philter read the dates in, from {@code X-Philter-Export-Time-Zone}. */
    public String getTimeZone() {
        return timeZone;
    }

}
