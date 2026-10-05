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
 * A page of a user's active API keys and the total number of them.
 */
public class GetApiKeysResponse {

    @Expose
    @SerializedName("apiKeys")
    private List<ApiKey> apiKeys;

    @Expose
    @SerializedName("total")
    private long total;

    /** The keys in this page, oldest first. */
    public List<ApiKey> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<ApiKey> apiKeys) {
        this.apiKeys = apiKeys;
    }

    /** Every active key the user has, across all pages. */
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

}
