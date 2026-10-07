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

import java.util.Map;

/**
 * A context's settings and the number of entries it holds, counted by filter type.
 */
public class ContextDetails {

    @Expose
    @SerializedName("size")
    private long size;

    @Expose
    @SerializedName("filterTypes")
    private Map<String, Long> filterTypes;

    @Expose
    @SerializedName("untyped")
    private long untyped;

    @Expose
    @SerializedName("entityTypeDisambiguation")
    private boolean entityTypeDisambiguation;

    @Expose
    @SerializedName("disambiguationScope")
    private String disambiguationScope;

    @Expose
    @SerializedName("ledger")
    private boolean ledger;

    /** Every entry in the context. */
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    /** Entries by filter type, sorted by filter type. */
    public Map<String, Long> getFilterTypes() {
        return filterTypes;
    }

    public void setFilterTypes(Map<String, Long> filterTypes) {
        this.filterTypes = filterTypes;
    }

    /** Entries with no filter type, which only an import creates. */
    public long getUntyped() {
        return untyped;
    }

    public void setUntyped(long untyped) {
        this.untyped = untyped;
    }

    /** Whether entity type disambiguation is enabled. */
    public boolean isEntityTypeDisambiguation() {
        return entityTypeDisambiguation;
    }

    public void setEntityTypeDisambiguation(boolean entityTypeDisambiguation) {
        this.entityTypeDisambiguation = entityTypeDisambiguation;
    }

    /**
     * What span disambiguation learns from: {@link DisambiguationScope#DOCUMENT} or
     * {@link DisambiguationScope#CONTEXT}. {@code null} from a Philter that does not return it.
     */
    public String getDisambiguationScope() {
        return disambiguationScope;
    }

    public void setDisambiguationScope(String disambiguationScope) {
        this.disambiguationScope = disambiguationScope;
    }

    /** Whether the redaction ledger is enabled. */
    public boolean isLedger() {
        return ledger;
    }

    public void setLedger(boolean ledger) {
        this.ledger = ledger;
    }

}
