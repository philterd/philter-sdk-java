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
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * A deleted policy whose version history Philter keeps. Read its history with
 * {@code getPolicyVersions} and a revision with {@code getPolicyVersion}, by the policy's name; restore it
 * by saving a revision's JSON with {@code savePolicy}.
 */
public class DeletedPolicy {

    @Expose
    @SerializedName("name")
    private String name;

    @Expose
    @SerializedName("latestRevision")
    private int latestRevision;

    @Expose
    @SerializedName("deletedAt")
    private String deletedAt;

    @Expose
    @SerializedName("deletedBy")
    private String deletedBy;

    /** The policy's name, which its version history is read by. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** The most recent retained revision: the policy as it was when it was deleted. */
    public int getLatestRevision() {
        return latestRevision;
    }

    public void setLatestRevision(int latestRevision) {
        this.latestRevision = latestRevision;
    }

    /**
     * When the policy was deleted, or {@code null} for a policy deleted before Philter recorded deletions,
     * or when Philter's value cannot be read as a timestamp ({@link #getDeletedAtText()} then has it).
     */
    public Instant getDeletedAt() {
        if (deletedAt == null || deletedAt.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(deletedAt).toInstant();
        } catch (final DateTimeParseException ex) {
            return null;
        }
    }

    /** When the policy was deleted, as Philter sent it, or {@code null}. */
    public String getDeletedAtText() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }

    /** The username of the user who deleted it, or {@code null} when that is not known. */
    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

}
