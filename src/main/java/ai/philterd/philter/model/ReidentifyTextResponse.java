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
 * A text with each {@code CRYPTO_REPLACE} token that decrypted replaced by its original value, and how many tokens
 * were found, restored, and left as they were.
 */
public class ReidentifyTextResponse {

    @Expose
    @SerializedName("text")
    private String text;

    @Expose
    @SerializedName("found")
    private int found;

    @Expose
    @SerializedName("restored")
    private int restored;

    @Expose
    @SerializedName("failed")
    private int failed;

    /** The text, with each token that decrypted replaced by its original value and every other token unchanged. */
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * How many tokens were found in the text. A token altered so that it is no longer <code>{{</code> followed by
     * Base64 and <code>}}</code> is not found, and counts in neither this nor {@link #getFailed()}.
     */
    public int getFound() {
        return found;
    }

    public void setFound(int found) {
        this.found = found;
    }

    /** How many tokens were replaced by their original value. */
    public int getRestored() {
        return restored;
    }

    public void setRestored(int restored) {
        this.restored = restored;
    }

    /** How many tokens were found but did not decrypt, and were left in the text as they were. */
    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

}
