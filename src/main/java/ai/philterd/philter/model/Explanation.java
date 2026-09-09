/*******************************************************************************
 * Copyright 2026 Philterd, LLC
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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

public class Explanation {

    @Expose
    @SerializedName("appliedSpans")
    private List<Span> appliedSpans;

    @Expose
    @SerializedName("identifiedSpans")
    private List<Span> identifiedSpans;

    public List<Span> getAppliedSpans() {
        return appliedSpans;
    }

    public void setAppliedSpans(List<Span> appliedSpans) {
        this.appliedSpans = appliedSpans;
    }

    /**
     * Gets every span Philter identified, whether or not a filter was applied to it. Philter
     * reports this as {@code identifiedSpans}; whether an individual span was skipped is carried
     * on {@link Span#isIgnored()}.
     * @return The identified spans.
     */
    public List<Span> getIdentifiedSpans() {
        return identifiedSpans;
    }

    public void setIdentifiedSpans(List<Span> identifiedSpans) {
        this.identifiedSpans = identifiedSpans;
    }

}
