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

/** The outcome of importing a context's entries: how many there were, and what happened to each. */
public class ImportContextEntriesResponse {

    @Expose
    @SerializedName("total")
    private int total;

    @Expose
    @SerializedName("inserted")
    private int inserted;

    @Expose
    @SerializedName("overwritten")
    private int overwritten;

    @Expose
    @SerializedName("skipped")
    private int skipped;

    /** How many entries the import held. */
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    /** How many were new to the context and added. */
    public int getInserted() {
        return inserted;
    }

    public void setInserted(int inserted) {
        this.inserted = inserted;
    }

    /** How many replaced an existing mapping, with {@code on_conflict=overwrite}. */
    public int getOverwritten() {
        return overwritten;
    }

    public void setOverwritten(int overwritten) {
        this.overwritten = overwritten;
    }

    /** How many were already in the context and kept as they were, by default. */
    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

}
