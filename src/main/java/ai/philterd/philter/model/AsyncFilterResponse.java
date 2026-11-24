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
 * Philter's response to a document submitted for asynchronous filtering. The document ID is the
 * correlation ID used by the documents endpoints.
 */
public class AsyncFilterResponse {

    @Expose
    @SerializedName("documentId")
    private String documentId;

    // From the response header rather than the JSON body, so Gson leaves it alone.
    private transient String pagePolicies;
    private transient boolean reviewSampled;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * The {@code X-Philter-Page-Policies} response header: the policy and version that will redact each
     * page set of a document submitted with {@link PagePolicy page policies}, pinned when Philter accepted
     * it, as {@code pages=policy@version} separated by {@code ; }, such as
     * {@code 2-5,8=records@3; 12-=exhibits@1}. Pages not listed are redacted with the request's policy.
     * @return The header's value, or {@code null} when the response has none.
     */
    public String getPagePolicies() {
        return pagePolicies;
    }

    public void setPagePolicies(String pagePolicies) {
        this.pagePolicies = pagePolicies;
    }

    /**
     * Whether the policy's review sampling rate selected this document for human spot review, from the
     * {@code X-Philter-Review: sampled} header of the {@code 202 Accepted} response. A document is selected
     * when Philter accepts it, and its status and download responses do not carry the header, so keep this
     * with the document ID. A header with any other value is not reported as selected. A document that was
     * not selected has not been checked by anyone; detection is probabilistic, so validate results against
     * your own data either way. Requires Philter 4.1.0 or later.
     * @return {@code true} when the response carried {@code X-Philter-Review: sampled}.
     */
    public boolean isReviewSampled() {
        return reviewSampled;
    }

    public void setReviewSampled(boolean reviewSampled) {
        this.reviewSampled = reviewSampled;
    }

}
