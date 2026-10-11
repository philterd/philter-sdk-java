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

import java.util.List;

/**
 * A context's exported mapping table: each original value's hash and its replacement. Pass it to
 * {@code importContextEntriesExport} to load the mappings into a context on this or another Philter, so the same
 * values keep the same replacements there. The hash is a plain SHA-256 of the value, the same everywhere.
 */
public class ContextEntriesExport {

    /** One exported mapping. */
    public static class Entry {

        @Expose
        @SerializedName("tokenHash")
        private String tokenHash;

        @Expose
        @SerializedName("replacement")
        private String replacement;

        @Expose
        @SerializedName("filterType")
        private String filterType;

        @Expose
        @SerializedName("replacementUuid")
        private boolean replacementUuid;

        /** The SHA-256 hash of the original value, as 64 hexadecimal characters. The value itself is never exported. */
        public String getTokenHash() {
            return tokenHash;
        }

        public void setTokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
        }

        /** The replacement the value was given. */
        public String getReplacement() {
            return replacement;
        }

        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }

        /** The filter type that found the value, such as {@code ssn}. */
        public String getFilterType() {
            return filterType;
        }

        public void setFilterType(String filterType) {
            this.filterType = filterType;
        }

        /** Whether the replacement is a generated UUID. */
        public boolean isReplacementUuid() {
            return replacementUuid;
        }

        public void setReplacementUuid(boolean replacementUuid) {
            this.replacementUuid = replacementUuid;
        }

    }

    @Expose
    @SerializedName("version")
    private int version;

    @Expose
    @SerializedName("context")
    private String context;

    @Expose
    @SerializedName("count")
    private int count;

    @Expose
    @SerializedName("entries")
    private List<Entry> entries;

    /** The export format's version. */
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /** The context the entries were exported from. An import may name a different context. */
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    /** How many entries the export holds. */
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    /** The mappings. */
    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

}
