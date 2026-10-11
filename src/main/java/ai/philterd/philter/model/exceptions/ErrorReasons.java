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
package ai.philterd.philter.model.exceptions;

/**
 * Reasons Philter gives for refusing a request, as {@link ClientException#getReason()} and
 * {@link PhilterError#getReason()} return them. Compare against these rather than against error messages.
 */
public final class ErrorReasons {

    /**
     * A redact list changed after the revision passed to {@code replaceRedactList}. Nothing was written: read the
     * list again, apply the change to what you read, and retry with the new revision.
     */
    public static final String REDACT_LIST_CHANGED = "redact_list_changed";

    private ErrorReasons() {
    }

}
