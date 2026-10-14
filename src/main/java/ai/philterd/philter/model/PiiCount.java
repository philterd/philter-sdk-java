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

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * The PII type counts Philter recorded for one user's context on one UTC day, for reporting such as
 * differential-privacy reporting with Philter Diffuse. Counts only: never any PII.
 */
public class PiiCount {

    @Expose
    @SerializedName("id")
    private String id;

    @Expose
    @SerializedName("owner")
    private String owner;

    @Expose
    @SerializedName("context")
    private String context;

    @Expose
    @SerializedName("day")
    private String day;

    @Expose
    @SerializedName("counts")
    private Map<String, Long> counts;

    @Expose
    @SerializedName("totalDocuments")
    private long totalDocuments;

    @Expose
    @SerializedName("updatedAt")
    private String updatedAt;

    /**
     * Identifies this user's context and day, the same on every read, so entries can be told apart without their
     * names: two users' contexts of one name are separate entries.
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * The username of the user whose redactions these are, or {@code null} for another user's counts when Philter's
     * {@code ADMIN_CROSS_USER_ACCESS_ENABLED} is off.
     */
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * The context the redactions ran in, or {@code null} for another user's counts when Philter's
     * {@code ADMIN_CROSS_USER_ACCESS_ENABLED} is off.
     */
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    /**
     * The UTC day the redactions ran on, or {@code null} when Philter sent none or a value that cannot be read
     * as a date ({@link #getDayText()} then has it).
     */
    public LocalDate getDay() {
        if (day == null || day.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(day);
        } catch (final DateTimeParseException ex) {
            return null;
        }
    }

    /** The UTC day the redactions ran on, as Philter sent it ({@code YYYY-MM-DD}), or {@code null}. */
    public String getDayText() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    /**
     * For each PII type, such as {@code SSN}, how many redactions that day found at least one of it. A redaction
     * adds at most one to each type, however many of that type it found.
     */
    public Map<String, Long> getCounts() {
        return counts;
    }

    public void setCounts(Map<String, Long> counts) {
        this.counts = counts;
    }

    /** How many redactions that day found any PII. Redactions that found no PII are not counted. */
    public long getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(long totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    /**
     * When a redaction was last counted, or {@code null} when Philter sent none or a value that cannot be read
     * as a timestamp ({@link #getUpdatedAtText()} then has it).
     */
    public Instant getUpdatedAt() {
        return Timestamps.parse(updatedAt);
    }

    /** When a redaction was last counted, as Philter sent it, or {@code null}. */
    public String getUpdatedAtText() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

}
