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
    private boolean hashChainValid;

    @Expose
    @SerializedName("signaturesValid")
    private boolean signaturesValid;

    @Expose
    @SerializedName("signedEntries")
    private int signedEntries;

    @Expose
    @SerializedName("unsignedEntries")
    private int unsignedEntries;

    /** The document ID. */
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /** The entries, in order, or {@code null} when the chain came from {@code verifyLedgerChain}. */
    public List<LedgerEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntry> entries) {
        this.entries = entries;
    }

    /** Whether the hash chain and the signatures are both valid. */
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /** Whether each entry links to the previous one. */
    public boolean isHashChainValid() {
        return hashChainValid;
    }

    public void setHashChainValid(boolean hashChainValid) {
        this.hashChainValid = hashChainValid;
    }

    /** Whether every signed entry verifies. */
    public boolean isSignaturesValid() {
        return signaturesValid;
    }

    public void setSignaturesValid(boolean signaturesValid) {
        this.signaturesValid = signaturesValid;
    }

    /** The number of signed entries. */
    public int getSignedEntries() {
        return signedEntries;
    }

    public void setSignedEntries(int signedEntries) {
        this.signedEntries = signedEntries;
    }

    /** The number of unsigned entries. */
    public int getUnsignedEntries() {
        return unsignedEntries;
    }

    public void setUnsignedEntries(int unsignedEntries) {
        this.unsignedEntries = unsignedEntries;
    }

}
