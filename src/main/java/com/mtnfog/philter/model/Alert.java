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

import java.util.Date;
import java.util.UUID;

public class Alert {

    private String id;
    private String filterProfile;
    private String strategyId;
    private String context;
    private String documentId;
    private String filterType;
    private Date date;

    public Alert() {

    }

    public Alert(String filterProfile, String strategyId, String context, String documentId, String filterType) {
        this.id = UUID.randomUUID().toString();
        this.filterProfile = filterProfile;
        this.strategyId = strategyId;
        this.context = context;
        this.documentId = documentId;
        this.filterType = filterType;
        this.date = new Date();
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getStrategyId() {
        return this.strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public String getContext() {
        return this.context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDocumentId() {
        return this.documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getFilterType() {
        return this.filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilterProfile() {
        return this.filterProfile;
    }

    public void setFilterProfile(String filterProfile) {
        this.filterProfile = filterProfile;
    }

}
