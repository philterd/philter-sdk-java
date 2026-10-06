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

import java.util.Map;

/**
 * A public key Philter signs its output with. The private key is never returned.
 */
public class SigningKey {

    @Expose
    @SerializedName("keyId")
    private String keyId;

    @Expose
    @SerializedName("pem")
    private String pem;

    @Expose
    @SerializedName("jwk")
    private Map<String, Object> jwk;

    @Expose
    @SerializedName("fingerprint")
    private String fingerprint;

    @Expose
    @SerializedName("active")
    private Boolean active;

    /** The key ID, which a signature names in its {@code kid}. */
    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    /** The public key in PEM ({@code BEGIN PUBLIC KEY}) format. */
    public String getPem() {
        return pem;
    }

    public void setPem(String pem) {
        this.pem = pem;
    }

    /** The public key as a JWK. Returned only for the active key. */
    public Map<String, Object> getJwk() {
        return jwk;
    }

    public void setJwk(Map<String, Object> jwk) {
        this.jwk = jwk;
    }

    /** The key's fingerprint. Returned only for the active key. */
    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    /** Whether this is the active key. Returned only when the key is read by ID. */
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}
