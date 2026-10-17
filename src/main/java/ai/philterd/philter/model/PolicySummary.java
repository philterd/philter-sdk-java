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

/**
 * A policy in a listing of policies: the caller's, another user's, every user's, or the managed policies.
 * Read its content with {@code getPolicy} and its notes with {@code getPolicyDetails}, by its name.
 */
public class PolicySummary {

    @Expose
    @SerializedName("name")
    private String name;

    @Expose
    @SerializedName("description")
    private String description;

    @Expose
    @SerializedName("revision")
    private int revision;

    @Expose
    @SerializedName("createdAt")
    private String createdAt;

    @Expose
    @SerializedName("updatedAt")
    private String updatedAt;

    @Expose
    @SerializedName("owner")
    private String owner;

    /** The policy's name. A managed policy's begins with {@code managed_}. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** The policy's description, or an empty string if it has none. */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** The policy's current revision. A policy's first revision is {@code 0}. */
    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    /**
     * When the policy was created, or {@code null} when Philter's value cannot be read as a timestamp
     * ({@link #getCreatedAtText()} then has it).
     */
    public Instant getCreatedAt() {
        return Timestamps.parse(createdAt);
    }

    /** When the policy was created, as Philter sent it, or {@code null}. */
    public String getCreatedAtText() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * When the policy was last changed, or {@code null} when Philter's value cannot be read as a timestamp
     * ({@link #getUpdatedAtText()} then has it).
     */
    public Instant getUpdatedAt() {
        return Timestamps.parse(updatedAt);
    }

    /** When the policy was last changed, as Philter sent it, or {@code null}. */
    public String getUpdatedAtText() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** The username of the user the policy belongs to. Set only in a listing across every user. */
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

}
