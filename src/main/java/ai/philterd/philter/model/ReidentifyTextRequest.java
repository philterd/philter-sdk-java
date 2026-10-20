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
 * A request to restore the {@code CRYPTO_REPLACE} values in a text. Philter requires all three fields.
 */
public class ReidentifyTextRequest {

    @Expose
    @SerializedName("text")
    private String text;

    @Expose
    @SerializedName("policyName")
    private String policyName;

    @Expose
    @SerializedName("reason")
    private String reason;

    /** The text to restore, such as a redacted document or an LLM's response to a redacted prompt. */
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /** The policy whose {@code crypto} key was used during redaction. */
    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    /** Why the reversal is authorized. Philter records it in the audit log. */
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}
