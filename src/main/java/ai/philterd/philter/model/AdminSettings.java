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
 * The deployment's admin settings. Philter never returns the Phield API key, only whether one is set.
 */
public class AdminSettings {

    @Expose
    @SerializedName("diffuseCountsEnabled")
    private boolean diffuseCountsEnabled;

    @Expose
    @SerializedName("signingEnabled")
    private boolean signingEnabled;

    @Expose
    @SerializedName("webhookAllowlist")
    private String webhookAllowlist;

    @Expose
    @SerializedName("phieldEnabled")
    private boolean phieldEnabled;

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
    @SerializedName("phieldApiKeySet")
    private boolean phieldApiKeySet;

    @Expose
    @SerializedName("warnings")
    private List<String> warnings;

    /** Whether PII counts are recorded for differential-privacy reporting. */
    public boolean isDiffuseCountsEnabled() {
        return diffuseCountsEnabled;
    }

    public void setDiffuseCountsEnabled(boolean diffuseCountsEnabled) {
        this.diffuseCountsEnabled = diffuseCountsEnabled;
    }

    /** Whether every text filter and explain response is signed. */
    public boolean isSigningEnabled() {
        return signingEnabled;
    }

    public void setSigningEnabled(boolean signingEnabled) {
        this.signingEnabled = signingEnabled;
    }

    /**
     * Where a user's webhook may point: comma-separated hostnames, IP addresses, and CIDR ranges.
     * Empty allows any public address.
     */
    public String getWebhookAllowlist() {
        return webhookAllowlist;
    }

    public void setWebhookAllowlist(String webhookAllowlist) {
        this.webhookAllowlist = webhookAllowlist;
    }

    /** Whether PII counts are published to Phield. */
    public boolean isPhieldEnabled() {
        return phieldEnabled;
    }

    public void setPhieldEnabled(boolean phieldEnabled) {
        this.phieldEnabled = phieldEnabled;
    }

    /** The Phield URL. */
    public String getPhieldUrl() {
        return phieldUrl;
    }

    public void setPhieldUrl(String phieldUrl) {
        this.phieldUrl = phieldUrl;
    }

    /** The source ID sent to Phield. */
    public String getPhieldSourceId() {
        return phieldSourceId;
    }

    public void setPhieldSourceId(String phieldSourceId) {
        this.phieldSourceId = phieldSourceId;
    }

    /** The organization sent to Phield. */
    public String getPhieldOrganization() {
        return phieldOrganization;
    }

    public void setPhieldOrganization(String phieldOrganization) {
        this.phieldOrganization = phieldOrganization;
    }

    /** Whether a Phield API key is set. The key itself is never returned. */
    public boolean isPhieldApiKeySet() {
        return phieldApiKeySet;
    }

    public void setPhieldApiKeySet(boolean phieldApiKeySet) {
        this.phieldApiKeySet = phieldApiKeySet;
    }

    /**
     * Warnings about the saved settings, such as a Phield API key that will be sent over http.
     * Empty on a read.
     */
    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

}
