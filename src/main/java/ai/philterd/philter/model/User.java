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
 * A Philter user. Philter never returns a password, its hash, or an MFA secret.
 */
public class User {

    @Expose
    @SerializedName("username")
    private String username;

    @Expose
    @SerializedName("email")
    private String email;

    @Expose
    @SerializedName("role")
    private String role;

    @Expose
    @SerializedName("active")
    private boolean active;

    @Expose
    @SerializedName("created")
    private String created;

    @Expose
    @SerializedName("deactivatedAt")
    private String deactivatedAt;

    @Expose
    @SerializedName("passwordSet")
    private boolean passwordSet;

    @Expose
    @SerializedName("passwordChangeRequired")
    private boolean passwordChangeRequired;

    @Expose
    @SerializedName("mfaEnabled")
    private boolean mfaEnabled;

    @Expose
    @SerializedName("mfaLocked")
    private boolean mfaLocked;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /** {@code user} or {@code admin}. */
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /** {@code false} once the user is deactivated, which stops its API keys working. */
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** When the user was created. */
    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    /** When the user was deactivated, or {@code null} if it never was. */
    public String getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(String deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    /** Whether the user has a password, for signing in. */
    public boolean isPasswordSet() {
        return passwordSet;
    }

    public void setPasswordSet(boolean passwordSet) {
        this.passwordSet = passwordSet;
    }

    /** Whether the user must change the password at next sign-in, because an administrator set it. */
    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired(boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }

    /** Whether the user is enrolled in MFA. */
    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    /** Whether the user's MFA is locked after repeated bad codes, until an administrator unlocks it. */
    public boolean isMfaLocked() {
        return mfaLocked;
    }

    public void setMfaLocked(boolean mfaLocked) {
        this.mfaLocked = mfaLocked;
    }

}
