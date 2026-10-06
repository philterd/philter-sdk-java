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

    @Expose
    @SerializedName("mfaAvailable")
    private boolean mfaAvailable;

    @Expose
    @SerializedName("mfaRequired")
    private boolean mfaRequired;

    @Expose
    @SerializedName("crossUserAccessEnabled")
    private boolean crossUserAccessEnabled;

    @Expose
    @SerializedName("ledgerDeletionEnabled")
    private boolean ledgerDeletionEnabled;

    @Expose
    @SerializedName("signingKeyExternallyManaged")
    private boolean signingKeyExternallyManaged;

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

    /** Whether users may enroll in MFA for sign-in. */
    public boolean isMfaAvailable() {
        return mfaAvailable;
    }

    public void setMfaAvailable(boolean mfaAvailable) {
        this.mfaAvailable = mfaAvailable;
    }

    /** Whether every user who signs in must enroll in MFA. */
    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    /**
     * Whether an administrator may act on other users' data with {@code owner} and the
     * across-users listings ({@code ADMIN_CROSS_USER_ACCESS_ENABLED}). Read-only: set by the deployment.
     */
    public boolean isCrossUserAccessEnabled() {
        return crossUserAccessEnabled;
    }

    public void setCrossUserAccessEnabled(boolean crossUserAccessEnabled) {
        this.crossUserAccessEnabled = crossUserAccessEnabled;
    }

    /**
     * Whether ledger chains may be deleted ({@code LEDGER_DELETION_ENABLED}). Read-only: set by the
     * deployment.
     */
    public boolean isLedgerDeletionEnabled() {
        return ledgerDeletionEnabled;
    }

    public void setLedgerDeletionEnabled(boolean ledgerDeletionEnabled) {
        this.ledgerDeletionEnabled = ledgerDeletionEnabled;
    }

    /**
     * Whether the signing key comes from {@code PHILTER_SIGNING_KEY_PATH}, in which case it cannot be
     * rotated with {@code regenerateSigningKey}. Read-only: set by the deployment.
     */
    public boolean isSigningKeyExternallyManaged() {
        return signingKeyExternallyManaged;
    }

    public void setSigningKeyExternallyManaged(boolean signingKeyExternallyManaged) {
        this.signingKeyExternallyManaged = signingKeyExternallyManaged;
    }

}
