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
 * A summary of a single revision of a policy.
 */
public class PolicyVersionSummary {

    @Expose
    @SerializedName("capturedTimestamp")
    private String capturedTimestamp;

    @Expose
    @SerializedName("contentHash")
    private String contentHash;

    @Expose
    @SerializedName("revision")
    private int revision;

    public String getCapturedTimestamp() {
        return capturedTimestamp;
    }

    public void setCapturedTimestamp(String capturedTimestamp) {
        this.capturedTimestamp = capturedTimestamp;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

}
