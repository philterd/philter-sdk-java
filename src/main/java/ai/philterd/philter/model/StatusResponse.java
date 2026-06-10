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
 * The response from Philter's health/status endpoints.
 */
public class StatusResponse {

    @Expose
    @SerializedName("applicationVersion")
    private String applicationVersion;

    @Expose
    @SerializedName("gitCommit")
    private String gitCommit;

    @Expose
    @SerializedName("redactionPolicySchemaVersion")
    private String redactionPolicySchemaVersion;

    @Expose
    @SerializedName("status")
    private String status;

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public String getRedactionPolicySchemaVersion() {
        return redactionPolicySchemaVersion;
    }

    public void setRedactionPolicySchemaVersion(String redactionPolicySchemaVersion) {
        this.redactionPolicySchemaVersion = redactionPolicySchemaVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
