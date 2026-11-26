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
 * A value the policy's leftover check found still detectable in a redaction's output, as {@code explain} reports
 * it. It never carries the value's text: read it from the filtered text by its offsets.
 */
public class LeftoverFinding {

    @Expose
    @SerializedName("filterType")
    private String filterType;

    @Expose
    @SerializedName("classification")
    private String classification;

    @Expose
    @SerializedName("characterStart")
    private int characterStart;

    @Expose
    @SerializedName("characterEnd")
    private int characterEnd;

    @Expose
    @SerializedName("confidence")
    private double confidence;

    /** The type of information found, such as {@code SSN}. */
    public String getFilterType() {
        return filterType;
    }

    /** The finding's classification, when the filter that found it gives one. */
    public String getClassification() {
        return classification;
    }

    /** Where the finding starts in the filtered text. */
    public int getCharacterStart() {
        return characterStart;
    }

    /** Where the finding ends in the filtered text, exclusive. */
    public int getCharacterEnd() {
        return characterEnd;
    }

    /** The detection's confidence. */
    public double getConfidence() {
        return confidence;
    }

    @Override
    public boolean equals(Object o) {
        return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
    }

}
