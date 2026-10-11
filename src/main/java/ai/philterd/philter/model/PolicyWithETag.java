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
 * A policy's JSON and its entity tag, from {@code getPolicyWithETag}. Send the tag back as the expected tag when
 * changing the policy, so the change is refused if someone else changed it since.
 */
public class PolicyWithETag {

    private final String json;
    private final String eTag;

    public PolicyWithETag(final String json, final String eTag) {
        this.json = json;
        this.eTag = eTag;
    }

    /** The policy, as JSON. */
    public String getJson() {
        return json;
    }

    /**
     * The policy's entity tag, such as {@code 7-0123456789abcdef}, without quotes. It identifies the policy
     * together with its description and notes, and changes when any of them does. {@code null} from a Philter
     * version that does not send one.
     */
    public String getETag() {
        return eTag;
    }

}
