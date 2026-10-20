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

/**
 * A reversible redaction strategy that {@code reidentifyValues} can reverse. Set it with
 * {@link ReidentifyRequest#setStrategy(ReidentifyStrategy)}.
 */
public enum ReidentifyStrategy {

    /** Reverses {@code CRYPTO_REPLACE}. The request needs the {@code policyName} whose key was used. */
    CRYPTO_REPLACE,

    /**
     * Reverses {@code FPE_ENCRYPT_REPLACE} (format-preserving encryption). {@code policyName} is not required; pass it
     * when the policy has a custom FPE key and tweak.
     */
    FPE_ENCRYPT_REPLACE;

    /** The strategy's name as Philter's API expects it: {@code CRYPTO_REPLACE} or {@code FPE_ENCRYPT_REPLACE}. */
    public String getName() {
        return name();
    }

}
