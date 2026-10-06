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
import java.util.Map;

/**
 * A document's redaction-ledger chain exported with the public keys that verify it, so it can be
 * checked without Philter. Unlike a chain that is read, its entries carry the original redacted values
 * in {@link LedgerEntry#getToken()}, so treat it as sensitive and store and transmit it securely.
 */
public class LedgerExport {

    @Expose
    @SerializedName("version")
    private int version;

    @Expose
    @SerializedName("documentId")
    private String documentId;

    @Expose
    @SerializedName("count")
    private int count;

    @Expose
    @SerializedName("entries")
    private List<LedgerEntry> entries;

    @Expose
    @SerializedName("signingKeys")
    private Map<String, String> signingKeys;

    /** The export format version. */
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /** The document ID. */
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /** The number of entries. */
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    /** The entries, in order. */
    public List<LedgerEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntry> entries) {
        this.entries = entries;
    }

    /** The public keys that signed the entries, PEM by key ID. */
    public Map<String, String> getSigningKeys() {
        return signingKeys;
    }

    public void setSigningKeys(Map<String, String> signingKeys) {
        this.signingKeys = signingKeys;
    }

}
