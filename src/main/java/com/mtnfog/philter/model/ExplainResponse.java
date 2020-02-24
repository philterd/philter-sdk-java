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
package com.mtnfog.philter.model;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The response from Philter resulting from an explain request.
 */
public class ExplainResponse {

    @Expose
    @SerializedName("filteredText")
	private String filteredText;

    @Expose
    @SerializedName("context")
    private String context;

    @Expose
    @SerializedName("documentId")
    private String documentId;

    @Expose
    @SerializedName("explanation")
    private Explanation explanation;

    /**
     * Creates a new filter explain response.
     */
	public ExplainResponse() {

	}

    public int hashCode() {
        return (new HashCodeBuilder(17, 37)).append(this.filteredText).append(this.context).append(this.documentId).toHashCode();
    }

    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o, new String[0]);
    }

    public String getFilteredText() {
        return filteredText;
    }

    public void setFilteredText(String filteredText) {
        this.filteredText = filteredText;
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

    public Explanation getExplanation() {
        return explanation;
    }

    public void setExplanation(Explanation explanation) {
        this.explanation = explanation;
    }

}
