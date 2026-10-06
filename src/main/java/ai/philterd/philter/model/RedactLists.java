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
 * An account's always-redact and never-redact term lists.
 */
public class RedactLists {

    @Expose
    @SerializedName("alwaysRedact")
    private List<String> alwaysRedact;

    @Expose
    @SerializedName("neverRedact")
    private List<String> neverRedact;

    /** Terms always redacted. */
    public List<String> getAlwaysRedact() {
        return alwaysRedact;
    }

    public void setAlwaysRedact(List<String> alwaysRedact) {
        this.alwaysRedact = alwaysRedact;
    }

    /** Terms never redacted. */
    public List<String> getNeverRedact() {
        return neverRedact;
    }

    public void setNeverRedact(List<String> neverRedact) {
        this.neverRedact = neverRedact;
    }

}
