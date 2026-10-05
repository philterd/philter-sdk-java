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
 * A request to complete a sign-in with an MFA code.
 */
public class SignInMfaRequest {

    @Expose
    @SerializedName("challenge")
    private String challenge;

    @Expose
    @SerializedName("code")
    private String code;

    public SignInMfaRequest() {
    }

    public SignInMfaRequest(String challenge, String code) {
        this.challenge = challenge;
        this.code = code;
    }

    /** The challenge from the sign-in. */
    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    /** The code from the authenticator app. */
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
