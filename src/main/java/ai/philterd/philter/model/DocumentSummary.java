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
 * A document submitted for asynchronous redaction.
 */
public class DocumentSummary {

    @Expose
    @SerializedName("type")
    private String type;

    @Expose
    @SerializedName("documentId")
    private String documentId;

    @Expose
    @SerializedName("fileName")
    private String fileName;

    @Expose
    @SerializedName("status")
    private String status;

    @Expose
    @SerializedName("timestamp")
    private String timestamp;

    @Expose
    @SerializedName("policyName")
    private String policyName;

    @Expose
    @SerializedName("policyVersion")
    private Integer policyVersion;

    @Expose
    @SerializedName("policyContentHash")
    private String policyContentHash;

    /**
     * What the job produces. {@code redaction}, a redacted document, is the only type Philter 4.0 returns.
     * A string rather than an enum, so a type a later Philter adds is still read.
     */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /** The document ID. */
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /** The source filename, or {@code null}. */
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /** {@code PENDING}, {@code PROCESSING}, {@code COMPLETE}, or {@code FAILED}. */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** When the document was submitted. */
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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
