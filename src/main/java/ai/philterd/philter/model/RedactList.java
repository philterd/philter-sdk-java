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

/** One of a user's two redact lists, as {@code getRedactList} and {@code replaceRedactList} name it. */
public enum RedactList {

    /** Terms always redacted. */
    ALWAYS("always"),

    /** Terms never redacted. */
    NEVER("never");

    private final String name;

    RedactList(final String name) {
        this.name = name;
    }

    /** The list's name in Philter's API path: {@code always} or {@code never}. */
    public String getName() {
        return name;
    }

}
