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
 * An API key as Philter lists it. Philter never returns a key after creating it, so this has no
 * field for its value, only a prefix to tell keys apart.
 */
public class ApiKey {

    @Expose
    @SerializedName("id")
    private String id;

    @Expose
    @SerializedName("prefix")
    private String prefix;

    @Expose
    @SerializedName("scopes")
    private List<String> scopes;

    @Expose
    @SerializedName("created")
    private String created;

    @Expose
    @SerializedName("bootstrap")
    private boolean bootstrap;

    /** The key's ID, used to change its scopes or revoke it. */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** The first characters of the key. */
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /** The scopes the key holds. */
    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    /** When the key was created. */
    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    /** Whether this is the key seeded from {@code PHILTER_BOOTSTRAP_API_KEY}. */
    public boolean isBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(boolean bootstrap) {
        this.bootstrap = bootstrap;
    }

}
