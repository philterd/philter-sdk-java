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
 * A request to set a user's webhook.
 */
public class SetWebhookRequest {

    @Expose
    @SerializedName("url")
    private String url;

    @Expose
    @SerializedName("secret")
    private String secret;

    public SetWebhookRequest() {
    }

    public SetWebhookRequest(String url, String secret) {
        this.url = url;
        this.secret = secret;
    }

    /** The URL Philter will POST results to. Must be {@code http} or {@code https}. */
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /** The shared secret Philter signs each delivery with. At least 16 characters. */
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

}
