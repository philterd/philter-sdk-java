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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/** Reads the timestamps Philter sends, such as {@code 2026-10-02T15:20:41.000+00:00} or {@code ...Z}. */
final class Timestamps {

    private Timestamps() {
    }

    /**
     * The timestamp as an {@link Instant}, or {@code null} when it is absent or cannot be read as one.
     * Read with {@link OffsetDateTime} rather than {@link Instant#parse}, which on Java 11 does not
     * accept an offset such as {@code +00:00}.
     */
    static Instant parse(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (final DateTimeParseException ex) {
            return null;
        }
    }

}
