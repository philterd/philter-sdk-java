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
 * The status of a document submitted for asynchronous redaction.
 */
public class DocumentStatus {

    @Expose
    @SerializedName("documentId")
    private String documentId;

    @Expose
    @SerializedName("status")
    private String status;

    @Expose
    @SerializedName("effectiveConfigurationHash")
    private String effectiveConfigurationHash;

    @Expose
    @SerializedName("error")
    private String error;

    @Expose
    @SerializedName("policyName")
    private String policyName;

    @Expose
    @SerializedName("policyVersion")
    private Integer policyVersion;

    @Expose
    @SerializedName("policyContentHash")
    private String policyContentHash;

    /** The document ID. */
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /** {@code PENDING}, {@code PROCESSING}, {@code COMPLETE}, or {@code FAILED}. */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** The hash of the configuration captured at submission. */
    public String getEffectiveConfigurationHash() {
        return effectiveConfigurationHash;
    }

    public void setEffectiveConfigurationHash(String effectiveConfigurationHash) {
        this.effectiveConfigurationHash = effectiveConfigurationHash;
    }

    /** Why the redaction failed, or {@code null} unless the status is {@code FAILED}. */
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * The policy the document was submitted with. Read that revision with
     * {@link ai.philterd.philter.PhilterClient#getPolicyVersion(String, int)}.
     */
    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    /** The policy's revision when the document was submitted, or {@code null} if none was recorded. */
    public Integer getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(Integer policyVersion) {
        this.policyVersion = policyVersion;
    }

    /** The SHA-256 of that revision's content. */
    public String getPolicyContentHash() {
        return policyContentHash;
    }

    public void setPolicyContentHash(String policyContentHash) {
        this.policyContentHash = policyContentHash;
    }

}
