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

import java.util.Optional;

/**
 * A redacted document downloaded after asynchronous redaction, with what Philter reports about it on the download
 * response. {@code getDocument} returns only the bytes; use {@code downloadDocument} to read these too.
 */
public class DocumentDownload {

    private final byte[] content;
    private final LeftoverCheck leftoverCheck;
    private final boolean leftoversFlagged;

    public DocumentDownload(byte[] content, LeftoverCheck leftoverCheck, boolean leftoversFlagged) {
        this.content = content;
        this.leftoverCheck = leftoverCheck;
        this.leftoversFlagged = leftoversFlagged;
    }

    /**
     * The redacted document.
     * @return The document bytes.
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * The result of the policy's leftover check, from the {@code X-Philter-Leftovers} header. An asynchronous
     * document is checked when it is redacted, so the result is on its status and download responses, not on its
     * submission's. Empty when the check was not called for. Requires Philter 4.1.0 or later.
     * @return The leftover check, or empty.
     */
    public Optional<LeftoverCheck> getLeftoverCheck() {
        return Optional.ofNullable(leftoverCheck);
    }

    /**
     * Whether the leftover check flagged the document for human review, because it found something: {@code true}
     * when Philter's {@code X-Philter-Review} header lists {@code leftovers}. Requires Philter 4.1.0 or later.
     * @return {@code true} when flagged.
     */
    public boolean isLeftoversFlagged() {
        return leftoversFlagged;
    }

}
