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

import java.time.Instant;

/**
 * One webhook delivery: an event Philter queued for a user's webhook, and how sending it went. Philter
 * retries a delivery the receiver did not accept, until it is delivered or gives up.
 */
public class WebhookDelivery {

    /** Waiting for its next attempt. */
    public static final String PENDING = "PENDING";

    /** Being sent. */
    public static final String PROCESSING = "PROCESSING";

    /** The receiver accepted it. */
    public static final String DELIVERED = "DELIVERED";

    /** Every attempt failed, and Philter stopped trying. */
    public static final String FAILED = "FAILED";

    @Expose
    @SerializedName("id")
    private String id;

    @Expose
    @SerializedName("documentId")
    private String documentId;

    @Expose
    @SerializedName("event")
    private String event;

    @Expose
    @SerializedName("url")
    private String url;

    @Expose
    @SerializedName("status")
    private String status;

    @Expose
    @SerializedName("attempts")
    private int attempts;

    @Expose
    @SerializedName("lastError")
    private String lastError;

    @Expose
    @SerializedName("createdAt")
    private String createdAt;

    @Expose
    @SerializedName("updatedAt")
    private String updatedAt;

    @Expose
    @SerializedName("nextAttemptAt")
    private String nextAttemptAt;

    @Expose
    @SerializedName("deliveredAt")
    private String deliveredAt;

    /** The delivery's id, sent as {@code X-Philter-Delivery-Id} with each attempt. */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** The document the event is about. */
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /** The event, such as {@link WebhookEvents#DOCUMENT_REDACTION_COMPLETE}. */
    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    /** Where the delivery is sent: the webhook URL when the event was queued. */
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /** {@link #PENDING}, {@link #PROCESSING}, {@link #DELIVERED}, or {@link #FAILED}. */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** How many attempts have been made. */
    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    /** Why the most recent attempt failed, or {@code null}. */
    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    /**
     * When the event was queued.
     * {@code null} too when Philter's value cannot be read as a timestamp; see {@link #getCreatedAtText()}.
     */
    public Instant getCreatedAt() {
        return Timestamps.parse(createdAt);
    }

    /** When the event was queued, as Philter sent it, or {@code null}. */
    public String getCreatedAtText() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * When the delivery last changed.
     * {@code null} too when Philter's value cannot be read as a timestamp; see {@link #getUpdatedAtText()}.
     */
    public Instant getUpdatedAt() {
        return Timestamps.parse(updatedAt);
    }

    /** When the delivery last changed, as Philter sent it, or {@code null}. */
    public String getUpdatedAtText() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * When the next attempt is due, for a delivery that is {@link #PENDING}, and otherwise {@code null}.
     * {@code null} too when Philter's value cannot be read as a timestamp; see {@link #getNextAttemptAtText()}.
     */
    public Instant getNextAttemptAt() {
        return Timestamps.parse(nextAttemptAt);
    }

    /** When the next attempt is due, as Philter sent it, or {@code null}. */
    public String getNextAttemptAtText() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(String nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    /**
     * When the receiver accepted the delivery, or {@code null} when it has not.
     * {@code null} too when Philter's value cannot be read as a timestamp; see {@link #getDeliveredAtText()}.
     */
    public Instant getDeliveredAt() {
        return Timestamps.parse(deliveredAt);
    }

    /** When the receiver accepted the delivery, as Philter sent it, or {@code null}. */
    public String getDeliveredAtText() {
        return deliveredAt;
    }

    public void setDeliveredAt(String deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

}
