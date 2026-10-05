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
 * A request to set a policy's description and notes. A field left {@code null} is left as it is,
 * and an empty value clears it.
 */
public class SetPolicyDetailsRequest {

    @Expose
    @SerializedName("description")
    private String description;

    @Expose
    @SerializedName("notes")
    private String notes;

    public SetPolicyDetailsRequest() {
    }

    public SetPolicyDetailsRequest(String description, String notes) {
        this.description = description;
        this.notes = notes;
    }

    /** The description, up to 200 characters. */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** The notes, up to 1000 characters. */
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

}
