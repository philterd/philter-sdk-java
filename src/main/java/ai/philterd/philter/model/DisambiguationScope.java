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
 * The values of a context's disambiguation scope: what span disambiguation learns from when entity type
 * disambiguation is enabled. Philter accepts them in any case and returns them in lower case. Any other
 * value is refused with an HTTP 400.
 */
public final class DisambiguationScope {

    /** Each document is disambiguated on its own, from what Philter learns in that document. The default. */
    public static final String DOCUMENT = "document";

    /** What Philter learns is stored and used for every later document redacted in the context. */
    public static final String CONTEXT = "context";

    private DisambiguationScope() {
    }

}
