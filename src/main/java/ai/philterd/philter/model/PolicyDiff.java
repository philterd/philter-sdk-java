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
import com.google.gson.JsonElement;

import java.util.List;

/**
 * The difference between two revisions of a policy: the revisions compared, and the RFC 6902 JSON Patch
 * operations that turn the {@code from} revision into the {@code to} revision.
 */
public class PolicyDiff {

    /** One change: an RFC 6902 operation, with the value before the change. */
    public static class Change {

        /** A member added. */
        public static final String ADD = "add";

        /** A member removed. */
        public static final String REMOVE = "remove";

        /** A value replaced. */
        public static final String REPLACE = "replace";

        @Expose
        @SerializedName("op")
        private String op;

        @Expose
        @SerializedName("path")
        private String path;

        @Expose
        @SerializedName("value")
        private JsonElement value;

        @Expose
        @SerializedName("oldValue")
        private JsonElement oldValue;

        /** The operation: {@link #ADD}, {@link #REMOVE}, or {@link #REPLACE}. */
        public String getOp() {
            return op;
        }

        public void setOp(String op) {
            this.op = op;
        }

        /** The JSON Pointer (RFC 6901) to the member that changed, such as {@code /identifiers/ssn}. */
        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        /**
         * The value after the change, for {@link #ADD} and {@link #REPLACE}. {@code null} for {@link #REMOVE};
         * a JSON {@code null} value is a {@link com.google.gson.JsonNull}.
         */
        public JsonElement getValue() {
            return value;
        }

        public void setValue(JsonElement value) {
            this.value = value;
        }

        /**
         * The value before the change, for {@link #REMOVE} and {@link #REPLACE}, to show what changed.
         * {@code null} for {@link #ADD}. RFC 6902 does not define this member, so a client applying the patch
         * ignores it.
         */
        public JsonElement getOldValue() {
            return oldValue;
        }

        public void setOldValue(JsonElement oldValue) {
            this.oldValue = oldValue;
        }

    }

    @Expose
    @SerializedName("from")
    private int from;

    @Expose
    @SerializedName("to")
    private int to;

    @Expose
    @SerializedName("changes")
    private List<Change> changes;

    /** The revision compared from. */
    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    /** The revision compared to. */
    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    /** The changes, empty when the two revisions have the same content. */
    public List<Change> getChanges() {
        return changes;
    }

    public void setChanges(List<Change> changes) {
        this.changes = changes;
    }

}
