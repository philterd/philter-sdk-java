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
 * The result of a sign-in. Either a session key, or, for a user enrolled in MFA, a challenge to
 * complete with {@code completeSignIn}: check {@link #isMfaRequired()} first.
 */
public class SignInResponse {

    @Expose
    @SerializedName("apiKey")
    private String apiKey;

    @Expose
    @SerializedName("username")
    private String username;

    @Expose
    @SerializedName("scopes")
    private List<String> scopes;

    @Expose
    @SerializedName("expiresAt")
    private String expiresAt;

    @Expose
    @SerializedName("idleExpiresAt")
    private String idleExpiresAt;

    @Expose
    @SerializedName("passwordChangeRequired")
    private boolean passwordChangeRequired;

    @Expose
    @SerializedName("mfaEnrollmentRequired")
    private boolean mfaEnrollmentRequired;

    @Expose
    @SerializedName("mfaRequired")
    private boolean mfaRequired;

    @Expose
    @SerializedName("challenge")
    private String challenge;

    @Expose
    @SerializedName("challengeExpiresAt")
    private String challengeExpiresAt;

    /**
     * The session key, or {@code null} for an MFA challenge. Send it as the API key, with the
     * {@code Bearer } prefix. It is returned here and nowhere else.
     */
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /** The signed-in user, or {@code null} for an MFA challenge. */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /** The session key's scopes. The user's role still decides administrator access. */
    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    /** When the session key's maximum lifetime ends. */
    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    /** When the session key expires unless it is used first. Each request moves it forward. */
    public String getIdleExpiresAt() {
        return idleExpiresAt;
    }

    public void setIdleExpiresAt(String idleExpiresAt) {
        this.idleExpiresAt = idleExpiresAt;
    }

    /**
     * Whether the key can only change the password and sign out, because an administrator set the
     * password. Changing it revokes the key.
     */
    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired(boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }

    /** Whether the key can only enroll in MFA and sign out, because the deployment requires MFA. */
    public boolean isMfaEnrollmentRequired() {
        return mfaEnrollmentRequired;
    }

    public void setMfaEnrollmentRequired(boolean mfaEnrollmentRequired) {
        this.mfaEnrollmentRequired = mfaEnrollmentRequired;
    }

    /** Whether this is an MFA challenge rather than a session key. */
    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    /** The MFA challenge to pass to {@code completeSignIn}, or {@code null} for a session key. */
    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    /** When the MFA challenge expires, five minutes after it was issued. */
    public String getChallengeExpiresAt() {
        return challengeExpiresAt;
    }

    public void setChallengeExpiresAt(String challengeExpiresAt) {
        this.challengeExpiresAt = challengeExpiresAt;
    }

}
