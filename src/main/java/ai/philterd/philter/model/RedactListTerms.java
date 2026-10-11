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

/** One redact list's terms and its revision, from {@code getRedactList} and {@code replaceRedactList}. */
public class RedactListTerms {

    @Expose
    @SerializedName("terms")
    private List<String> terms;

    @Expose
    @SerializedName("revision")
    private long revision;

    /** The list's terms. */
    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    /**
     * The list's revision, which every write of the list moves forward. A list that has never been written is
     * at {@code 0}. Pass it to {@code replaceRedactList} so the write is refused if the list changed since.
     */
    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

}
