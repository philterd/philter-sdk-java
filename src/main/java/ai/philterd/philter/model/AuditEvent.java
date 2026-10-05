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
 * An event in Philter's audit log. Every field except {@code timestamp} and {@code event} is
 * {@code null} for an event that did not record it. Audit events never carry redacted values.
 */
public class AuditEvent {

    @Expose
    @SerializedName("timestamp")
    private String timestamp;

    @Expose
    @SerializedName("event")
    private String event;

    @Expose
    @SerializedName("requestId")
    private String requestId;

    @Expose
    @SerializedName("apiKeyId")
    private String apiKeyId;

    @Expose
    @SerializedName("associatedObject")
    private String associatedObject;

    @Expose
    @SerializedName("clientIpAddress")
    private String clientIpAddress;

    @Expose
    @SerializedName("details")
    private String details;

    /** When the event was recorded, as an ISO-8601 timestamp with an offset. */
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /** The event type, such as {@code policy_deleted}. */
    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    /** The ID of the request that caused the event. */
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /** The acting principal. */
    public String getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(String apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    /** The entity the action concerned. */
    public String getAssociatedObject() {
        return associatedObject;
    }

    public void setAssociatedObject(String associatedObject) {
        this.associatedObject = associatedObject;
    }

    /** The client IP address the request came from. */
    public String getClientIpAddress() {
        return clientIpAddress;
    }

    public void setClientIpAddress(String clientIpAddress) {
        this.clientIpAddress = clientIpAddress;
    }

    /** A short, non-sensitive description. */
    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

}
