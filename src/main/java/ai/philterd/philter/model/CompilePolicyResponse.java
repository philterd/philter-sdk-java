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
import com.google.gson.JsonElement;

/**
 * A policy compiled from PhiSQL: the name and description its {@code POLICY} declaration gives, and the native
 * policy. Nothing is saved; save the policy with {@code savePolicy(name, getPolicyJson())}.
 */
public class CompilePolicyResponse {

    @Expose
    @SerializedName("name")
    private String name;

    @Expose
    @SerializedName("description")
    private String description;

    @Expose
    @SerializedName("policy")
    private JsonElement policy;

    /** The name from the source's {@code POLICY} declaration, or {@code null} when it has none. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** The description the source declares, or {@code null} when it declares none. */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** The compiled native policy, as a JSON object. */
    public JsonElement getPolicy() {
        return policy;
    }

    public void setPolicy(JsonElement policy) {
        this.policy = policy;
    }

    /** The compiled native policy as a JSON string, ready for {@code savePolicy}, or {@code null}. */
    public String getPolicyJson() {
        return policy == null ? null : policy.toString();
    }

}
