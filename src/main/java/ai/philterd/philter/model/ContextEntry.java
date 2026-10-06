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

/**
 * An entry in a context: a replacement Philter keeps for a value it redacted. The original value
 * is never returned.
 */
public class ContextEntry {

    @Expose
    @SerializedName("id")
    private String id;

    @Expose
    @SerializedName("filterType")
    private String filterType;

    @Expose
    @SerializedName("replacement")
    private String replacement;

    @Expose
    @SerializedName("reads")
    private long reads;

    @Expose
    @SerializedName("timestamp")
    private String timestamp;

    /** The entry ID, used to delete it. */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** The filter type, or {@code null} for an entry created by an import without one. */
    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    /** The replacement. */
    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    /** How many times the replacement has been reused. */
    public long getReads() {
        return reads;
    }

    public void setReads(long reads) {
        this.reads = reads;
    }

    /** When the entry was created. */
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}
