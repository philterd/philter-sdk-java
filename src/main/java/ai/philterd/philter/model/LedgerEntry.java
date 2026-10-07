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
 * An entry in the redaction ledger: one redaction in a document's hash-chained record. In a listing
 * of chains, each entry is the head of its chain.
 */
public class LedgerEntry {

    @Expose
    @SerializedName("documentId")
    private String documentId;

    @Expose
    @SerializedName("filename")
    private String filename;

    @Expose
    @SerializedName("type")
    private String type;

    @Expose
    @SerializedName("token")
    private String token;

    @Expose
    @SerializedName("replacement")
    private String replacement;

    @Expose
    @SerializedName("startPosition")
    private long startPosition;

    @Expose
    @SerializedName("documentHash")
    private String documentHash;

    @Expose
    @SerializedName("previousHash")
    private String previousHash;

    @Expose
    @SerializedName("hash")
    private String hash;

    @Expose
    @SerializedName("effectiveHash")
    private String effectiveHash;

    @Expose
    @SerializedName("timestamp")
    private String timestamp;

    @Expose
    @SerializedName("policyName")
    private String policyName;

    @Expose
    @SerializedName("policyVersion")
    private int policyVersion;

    @Expose
    @SerializedName("policyContentHash")
    private String policyContentHash;

    @Expose
    @SerializedName("signature")
    private String signature;

    @Expose
    @SerializedName("signingKeyId")
    private String signingKeyId;

    @Expose
    @SerializedName("owner")
    private String owner;

    @Expose
    @SerializedName("readError")
    private String readError;

    /** The redacted document ID. */
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /** The source filename, or {@code null}. */
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    /** The filter type of the redaction. */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * The original value that was redacted. Present only in a {@link LedgerExport}, which needs the
     * {@code ledger:export} scope: treat it as sensitive. {@code null} when a chain is read or listed.
     */
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    /** The replacement, or {@code null} when the entry could not be read ({@link #getReadError()}). */
    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    /** Where the redacted value started in the document. */
    public long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(long startPosition) {
        this.startPosition = startPosition;
    }

    /** The hash of the document. */
    public String getDocumentHash() {
        return documentHash;
    }

    public void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
    }

    /** The previous entry's hash, which links the chain. */
    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    /** This entry's hash. */
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    /**
     * For an asynchronous redaction, binds the configuration captured at submission into this entry's
     * hash. {@code null} for an entry without a captured configuration.
     */
    public String getEffectiveHash() {
        return effectiveHash;
    }

    public void setEffectiveHash(String effectiveHash) {
        this.effectiveHash = effectiveHash;
    }

    /** When the entry was recorded. */
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /** The policy applied. */
    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    /** The revision of the policy applied. */
    public int getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(int policyVersion) {
        this.policyVersion = policyVersion;
    }

    /** The hash of the policy's content. */
    public String getPolicyContentHash() {
        return policyContentHash;
    }

    public void setPolicyContentHash(String policyContentHash) {
        this.policyContentHash = policyContentHash;
    }

    /** The entry's ES256 signature, or {@code null} for an unsigned entry. */
    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    /** The ID of the key that signed it, for {@code getSigningKeyDetails(keyId)}. */
    public String getSigningKeyId() {
        return signingKeyId;
    }

    public void setSigningKeyId(String signingKeyId) {
        this.signingKeyId = signingKeyId;
    }

    /** The owner's username, set only in a listing across all users. */
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Why Philter could not read this entry, for example because it no longer decrypts after a key change,
     * or {@code null} when the entry was read. When set, {@link #getReplacement()} is {@code null}, while the
     * fields stored in the clear are still given: the document ID, filename, type, start position, the
     * hashes, the timestamp, the policy fields, and the signature. Philter logs the cause with the document
     * ID. A chain with an unreadable entry cannot be exported, and reads as one that could not be validated.
     * A listing shows only each chain's head, so a {@code null} here does not mean the chain's later entries
     * can be read.
     */
    public String getReadError() {
        return readError;
    }

    public void setReadError(String readError) {
        this.readError = readError;
    }

}
