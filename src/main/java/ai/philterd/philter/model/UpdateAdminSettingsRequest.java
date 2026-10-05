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
 * A change to the deployment's admin settings. Only the fields that are set are sent, and Philter
 * leaves the rest as they are.
 */
public class UpdateAdminSettingsRequest {

    @Expose
    @SerializedName("diffuseCountsEnabled")
    private Boolean diffuseCountsEnabled;

    @Expose
    @SerializedName("signingEnabled")
    private Boolean signingEnabled;

    @Expose
    @SerializedName("webhookAllowlist")
    private String webhookAllowlist;

    @Expose
    @SerializedName("phieldEnabled")
    private Boolean phieldEnabled;

    @Expose
    @SerializedName("phieldUrl")
    private String phieldUrl;

    @Expose
    @SerializedName("phieldSourceId")
    private String phieldSourceId;

    @Expose
    @SerializedName("phieldOrganization")
    private String phieldOrganization;

    @Expose
    @SerializedName("phieldApiKey")
    private String phieldApiKey;

    /** Whether to record PII counts for differential-privacy reporting. */
    public Boolean getDiffuseCountsEnabled() {
        return diffuseCountsEnabled;
    }

    public void setDiffuseCountsEnabled(Boolean diffuseCountsEnabled) {
        this.diffuseCountsEnabled = diffuseCountsEnabled;
    }

    /** Whether to sign every text filter and explain response. */
    public Boolean getSigningEnabled() {
        return signingEnabled;
    }

    public void setSigningEnabled(Boolean signingEnabled) {
        this.signingEnabled = signingEnabled;
    }

    /**
     * Comma-separated hostnames, IP addresses, and CIDR ranges a webhook may point to. Empty
     * allows any public address.
     */
    public String getWebhookAllowlist() {
        return webhookAllowlist;
    }

    public void setWebhookAllowlist(String webhookAllowlist) {
        this.webhookAllowlist = webhookAllowlist;
    }

    /** Whether to publish PII counts to Phield. Enabling requires a Phield URL. */
    public Boolean getPhieldEnabled() {
        return phieldEnabled;
    }

    public void setPhieldEnabled(Boolean phieldEnabled) {
        this.phieldEnabled = phieldEnabled;
    }

    /** An absolute {@code http} or {@code https} Phield URL. */
    public String getPhieldUrl() {
        return phieldUrl;
    }

    public void setPhieldUrl(String phieldUrl) {
        this.phieldUrl = phieldUrl;
    }

    /** The source ID sent to Phield. Blank sets {@code philter}. */
    public String getPhieldSourceId() {
        return phieldSourceId;
    }

    public void setPhieldSourceId(String phieldSourceId) {
        this.phieldSourceId = phieldSourceId;
    }

    /** The organization sent to Phield. Blank sets {@code philter}. */
    public String getPhieldOrganization() {
        return phieldOrganization;
    }

    public void setPhieldOrganization(String phieldOrganization) {
        this.phieldOrganization = phieldOrganization;
    }

    /** The Phield API key. Empty removes the key. */
    public String getPhieldApiKey() {
        return phieldApiKey;
    }

    public void setPhieldApiKey(String phieldApiKey) {
        this.phieldApiKey = phieldApiKey;
    }

}
