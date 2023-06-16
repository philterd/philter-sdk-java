/*******************************************************************************
 * Copyright 2020 Mountain Fog, Inc.
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
 * A section of text that was identified by Philter as sensitive information
 * and filtered from the input text.
 */
public class FilteredSpan {

    @Expose
    @SerializedName("characterStart")
    private int characterStart;

    @Expose
    @SerializedName("characterEnd")
    private int characterEnd;

    @Expose
    @SerializedName("filterType")
    private String filterType;

    @Expose
    @SerializedName("context")
    private String context;

    @Expose
    @SerializedName("documentId")
    private String documentId;

    @Expose
    @SerializedName("confidence")
    private double confidence;

    @Expose
    @SerializedName("replacement")
    private String replacement;

    public int getCharacterStart() {
        return characterStart;
    }

    public void setCharacterStart(int characterStart) {
        this.characterStart = characterStart;
    }

    public int getCharacterEnd() {
        return characterEnd;
    }

    public void setCharacterEnd(int characterEnd) {
        this.characterEnd = characterEnd;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

}
