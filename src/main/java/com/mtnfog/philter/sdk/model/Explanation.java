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
package com.mtnfog.philter.sdk.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Explanation {

    @Expose
    @SerializedName("appliedSpans")
    private List<Span> appliedSpans;

    @Expose
    @SerializedName("ignoredSpans")
    private List<Span> ignoredSpans;

    public List<Span> getAppliedSpans() {
        return appliedSpans;
    }

    public void setAppliedSpans(List<Span> appliedSpans) {
        this.appliedSpans = appliedSpans;
    }

    public List<Span> getIgnoredSpans() {
        return ignoredSpans;
    }

    public void setIgnoredSpans(List<Span> ignoredSpans) {
        this.ignoredSpans = ignoredSpans;
    }

}
