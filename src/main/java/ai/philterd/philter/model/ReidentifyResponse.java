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
 * The result of re-identifying values: one result per value sent, in the same order. A value that could not be
 * re-identified has an error rather than failing the whole request.
 */
public class ReidentifyResponse {

    /** One value's re-identification. */
    public static class ReidentifyResult {

        @Expose
        @SerializedName("encrypted")
        private String encrypted;

        @Expose
        @SerializedName("decrypted")
        private String decrypted;

        @Expose
        @SerializedName("error")
        private String error;

        /** The value as it was sent: the redacted, encrypted replacement. */
        public String getEncrypted() {
            return encrypted;
        }

        public void setEncrypted(String encrypted) {
            this.encrypted = encrypted;
        }

        /** The original value, or {@code null} when it could not be re-identified. */
        public String getDecrypted() {
            return decrypted;
        }

        public void setDecrypted(String decrypted) {
            this.decrypted = decrypted;
        }

        /** Why the value could not be re-identified, or {@code null} when it was. */
        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

    }

    @Expose
    @SerializedName("results")
    private List<ReidentifyResult> results;

    /** One result for each value sent, in the order sent. */
    public List<ReidentifyResult> getResults() {
        return results;
    }

    public void setResults(List<ReidentifyResult> results) {
        this.results = results;
    }

}
