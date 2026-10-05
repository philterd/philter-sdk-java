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
 * A started MFA enrollment. Neither value is returned again, and the enrollment does not apply until
 * it is confirmed with a code.
 */
public class MfaEnrollment {

    @Expose
    @SerializedName("secret")
    private String secret;

    @Expose
    @SerializedName("otpauthUri")
    private String otpauthUri;

    /** The TOTP secret, for a person to type into an authenticator app. */
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    /** The {@code otpauth://} URI, to show as a QR code for an authenticator app to scan. */
    public String getOtpauthUri() {
        return otpauthUri;
    }

    public void setOtpauthUri(String otpauthUri) {
        this.otpauthUri = otpauthUri;
    }

}
