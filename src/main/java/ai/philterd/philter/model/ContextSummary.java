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
 * A context in a listing of contexts: the caller's, another user's, or every user's. Read its settings with
 * {@code getContextDetails}, by its name.
 */
public class ContextSummary {

    @Expose
    @SerializedName("name")
    private String name;

    @Expose
    @SerializedName("createdAt")
    private String createdAt;

    @Expose
    @SerializedName("owner")
    private String owner;

    /** The context's name. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * When the context was created, or {@code null} when Philter's value cannot be read as a timestamp
     * ({@link #getCreatedAtText()} then has it).
     */
    public Instant getCreatedAt() {
        return Timestamps.parse(createdAt);
    }

    /** When the context was created, as Philter sent it, or {@code null}. */
    public String getCreatedAtText() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /** The username of the user the context belongs to. Set only in a listing across every user. */
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

}
