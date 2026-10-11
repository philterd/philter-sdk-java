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
 * The events Philter sends to a webhook, in the request body's {@code event} field and the
 * {@code X-Philter-Event} header.
 */
public final class WebhookEvents {

    /** An asynchronous redaction completed. */
    public static final String DOCUMENT_REDACTION_COMPLETE = "DOCUMENT_REDACTION_COMPLETE";

    /** An asynchronous redaction failed. */
    public static final String DOCUMENT_REDACTION_FAILED = "DOCUMENT_REDACTION_FAILED";

    /**
     * A test, sent only when asked for with {@code testWebhook}. Its body has {@code "test": true} and no
     * document. A receiver should answer it with a 2xx status and otherwise ignore it.
     */
    public static final String WEBHOOK_TEST = "WEBHOOK_TEST";

    private WebhookEvents() {
    }

}
