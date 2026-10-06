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
 * A page of redaction-ledger chains, most recent first, and the total number matching.
 */
public class GetLedgerResponse {

    @Expose
    @SerializedName("chains")
    private List<LedgerEntry> chains;

    @Expose
    @SerializedName("total")
    private int total;

    /** The head of each chain in this page. */
    public List<LedgerEntry> getChains() {
        return chains;
    }

    public void setChains(List<LedgerEntry> chains) {
        this.chains = chains;
    }

    /** Every chain the request matched, across all pages. */
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

}
