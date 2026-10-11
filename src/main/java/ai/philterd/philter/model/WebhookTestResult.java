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
 * The outcome of sending a {@link WebhookEvents#WEBHOOK_TEST} event to a webhook. A receiver that refused
 * the test, or could not be reached, is a result with {@link #isDelivered()} {@code false}, not an error.
 */
public class WebhookTestResult {

    @Expose
    @SerializedName("delivered")
    private boolean delivered;

    @Expose
    @SerializedName("statusCode")
    private Integer statusCode;

    @Expose
    @SerializedName("error")
    private String error;

    @Expose
    @SerializedName("durationMillis")
    private long durationMillis;

    @Expose
    @SerializedName("deliveryId")
    private String deliveryId;

    /** Whether the receiver answered with a 2xx status. */
    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    /**
     * The status the receiver answered with, or {@code null} when it did not answer, such as when it could
     * not be reached or timed out.
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    /** Why the test was not delivered, or {@code null} when it was. */
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /** How long the attempt took, in milliseconds. */
    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    /** The test event's {@code X-Philter-Delivery-Id}, to find it in the receiver's logs. */
    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

}
