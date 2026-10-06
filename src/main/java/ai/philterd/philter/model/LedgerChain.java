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
 * A document's redaction-ledger chain and whether it verifies.
 *
 * <p>A chain has one of three outcomes:</p>
 * <ul>
 *   <li><b>Valid:</b> {@link #isValid()} is {@code true}.</li>
 *   <li><b>Failed a check:</b> {@link #isValid()} is {@code false} and {@link #getValidationError()} is
 *   {@code null}. A {@code false} {@link #getHashChainValid()} means the chain does not start with a genesis
 *   entry, or an entry no longer matches its hash or its link to the previous one. A {@code false}
 *   {@link #getSignaturesValid()} means an entry is unsigned or its signature does not verify;
 *   {@link #getUnsignedEntries()} counts the unsigned ones.</li>
 *   <li><b>Could not be checked:</b> {@link #isValid()} is {@code false} and {@link #getValidationError()}
 *   says why, for example because an entry can no longer be decrypted. The checks did not complete, so
 *   {@link #getHashChainValid()}, {@link #getSignaturesValid()}, {@link #getSignedEntries()},
 *   {@link #getUnsignedEntries()}, and {@link #getEntries()} are {@code null}. This is not evidence of
 *   tampering, and not evidence that the chain is intact.</li>
 * </ul>
 */
public class LedgerChain {

    @Expose
    @SerializedName("documentId")
    private String documentId;

    @Expose
    @SerializedName("entries")
    private List<LedgerEntry> entries;

    @Expose
    @SerializedName("valid")
    private boolean valid;

    @Expose
    @SerializedName("hashChainValid")
    private Boolean hashChainValid;

    @Expose
    @SerializedName("signaturesValid")
    private Boolean signaturesValid;

    @Expose
    @SerializedName("signedEntries")
    private Integer signedEntries;

    @Expose
    @SerializedName("unsignedEntries")
    private Integer unsignedEntries;

    @Expose
    @SerializedName("validationError")
    private String validationError;

    /** The document ID. */
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * The entries, in order. {@code null} when the chain came from {@code verifyLedgerChain}, or when it
     * could not be checked.
     */
    public List<LedgerEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntry> entries) {
        this.entries = entries;
    }

    /**
     * Whether the hash chain verifies and every entry is signed with a signature that verifies. {@code false}
     * both for a chain that failed a check and for one that could not be checked;
     * {@link #getValidationError()} tells them apart.
     */
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * Whether the chain starts with a genesis entry and each entry matches its hash and links to the
     * previous one. {@code null} when the chain could not be checked.
     */
    public Boolean getHashChainValid() {
        return hashChainValid;
    }

    public void setHashChainValid(Boolean hashChainValid) {
        this.hashChainValid = hashChainValid;
    }

    /**
     * Whether every entry is signed and its signature verifies; an unsigned entry makes this {@code false}.
     * {@code null} when the chain could not be checked.
     */
    public Boolean getSignaturesValid() {
        return signaturesValid;
    }

    public void setSignaturesValid(Boolean signaturesValid) {
        this.signaturesValid = signaturesValid;
    }

    /** The number of signed entries. {@code null} when the chain could not be checked. */
    public Integer getSignedEntries() {
        return signedEntries;
    }

    public void setSignedEntries(Integer signedEntries) {
        this.signedEntries = signedEntries;
    }

    /** The number of unsigned entries. {@code null} when the chain could not be checked. */
    public Integer getUnsignedEntries() {
        return unsignedEntries;
    }

    public void setUnsignedEntries(Integer unsignedEntries) {
        this.unsignedEntries = unsignedEntries;
    }

    /**
     * Why the chain could not be checked, or {@code null} when the checks completed. When set,
     * {@link #isValid()} is {@code false} and the check results are {@code null}.
     */
    public String getValidationError() {
        return validationError;
    }

    public void setValidationError(String validationError) {
        this.validationError = validationError;
    }

}
