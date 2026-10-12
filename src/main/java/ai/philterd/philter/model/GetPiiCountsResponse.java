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
 * A page of the PII type counts Philter recorded, the total number matching the filters, and whether Philter is
 * recording counts now.
 */
public class GetPiiCountsResponse {

    @Expose
    @SerializedName("recordingEnabled")
    private boolean recordingEnabled;

    @Expose
    @SerializedName("counts")
    private List<PiiCount> counts;

    @Expose
    @SerializedName("total")
    private long total;

    /**
     * Whether Philter is recording counts now, from its {@code diffuseCountsEnabled} admin setting. When
     * {@code false}, an empty listing does not mean that no PII was found, only that nothing is being recorded.
     * Counts recorded while recording was on are still returned.
     */
    public boolean isRecordingEnabled() {
        return recordingEnabled;
    }

    public void setRecordingEnabled(boolean recordingEnabled) {
        this.recordingEnabled = recordingEnabled;
    }

    /** One entry per context and day, most recent day first, then by context. */
    public List<PiiCount> getCounts() {
        return counts;
    }

    public void setCounts(List<PiiCount> counts) {
        this.counts = counts;
    }

    /** Every entry matching the filters, across all pages. */
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

}
