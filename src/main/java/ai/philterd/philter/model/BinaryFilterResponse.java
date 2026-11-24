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

public class BinaryFilterResponse {

    private byte[] content;
    private String context;
    private String documentId;
    private String pagePolicies;
    private boolean reviewSampled;

    public BinaryFilterResponse(String context, String documentId, byte[] content) {
        this(context, documentId, content, null);
    }

    public BinaryFilterResponse(String context, String documentId, byte[] content, String pagePolicies) {
        this(context, documentId, content, pagePolicies, false);
    }

    public BinaryFilterResponse(String context, String documentId, byte[] content, String pagePolicies,
                                boolean reviewSampled) {
        this.context = context;
        this.documentId = documentId;
        this.content = content;
        this.pagePolicies = pagePolicies;
        this.reviewSampled = reviewSampled;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * The {@code X-Philter-Page-Policies} response header: the policy and version that redacted each
     * page set of a request sent with {@link PagePolicy page policies}, as {@code pages=policy@version}
     * separated by {@code ; }, such as {@code 2-5,8=records@3; 12-=exhibits@1}. Pages not listed were
     * redacted with the request's policy.
     * @return The header's value, or {@code null} when the response has none.
     */
    public String getPagePolicies() {
        return pagePolicies;
    }

    public void setPagePolicies(String pagePolicies) {
        this.pagePolicies = pagePolicies;
    }

    /**
     * Whether the policy's review sampling rate selected this request for human spot review, from the
     * {@code X-Philter-Review: sampled} response header. A header with any other value is not reported as
     * selected. A request that was not selected has not been checked by anyone; detection is probabilistic,
     * so validate results against your own data either way. Requires Philter 4.1.0 or later.
     * @return {@code true} when the response carried {@code X-Philter-Review: sampled}.
     */
    public boolean isReviewSampled() {
        return reviewSampled;
    }

    public void setReviewSampled(boolean reviewSampled) {
        this.reviewSampled = reviewSampled;
    }

}
