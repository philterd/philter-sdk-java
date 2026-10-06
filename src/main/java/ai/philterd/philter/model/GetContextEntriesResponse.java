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
 * A page of a context's entries and the total number of them.
 */
public class GetContextEntriesResponse {

    @Expose
    @SerializedName("entries")
    private List<ContextEntry> entries;

    @Expose
    @SerializedName("total")
    private int total;

    /** The entries in this page. */
    public List<ContextEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<ContextEntry> entries) {
        this.entries = entries;
    }

    /** Every entry in the context, across all pages. */
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

}
