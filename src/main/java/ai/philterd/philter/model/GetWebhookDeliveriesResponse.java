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
 * A page of a user's webhook deliveries, newest first, and how many there are.
 */
public class GetWebhookDeliveriesResponse {

    @Expose
    @SerializedName("deliveries")
    private List<WebhookDelivery> deliveries;

    @Expose
    @SerializedName("total")
    private long total;

    /** The deliveries in this page, newest first. */
    public List<WebhookDelivery> getDeliveries() {
        return deliveries;
    }

    public void setDeliveries(List<WebhookDelivery> deliveries) {
        this.deliveries = deliveries;
    }

    /** Every delivery the user has, across all pages. */
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

}
