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
 * The calling key's user, with the deployment's MFA settings. A user who is not an administrator cannot
 * read the admin settings, but needs these to decide whether to offer MFA enrollment.
 */
public class CurrentUser extends User {

    @Expose
    @SerializedName("mfaAvailable")
    private boolean mfaAvailable;

    @Expose
    @SerializedName("mfaRequired")
    private boolean mfaRequired;

    /** Whether this deployment lets users enroll in MFA. */
    public boolean isMfaAvailable() {
        return mfaAvailable;
    }

    public void setMfaAvailable(boolean mfaAvailable) {
        this.mfaAvailable = mfaAvailable;
    }

    /**
     * Whether this deployment makes every user who signs in enroll in MFA. Never {@code true} while
     * {@link #isMfaAvailable()} is {@code false}.
     */
    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

}
