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
 * How to page, sort, search, and filter a listing. Every setting is optional, and one left unset is not sent, so
 * Philter's default applies. Set them fluently:
 *
 * <pre>{@code
 * ListOptions options = new ListOptions().limit(50).sort("name").descending().search("claims");
 * }</pre>
 *
 * <p>Each listing documents the {@code sort} fields it accepts and whether it supports {@code search}; an unknown
 * sort field is an HTTP 400 naming the fields it does accept. The filters ({@link #role(String)},
 * {@link #active(Boolean)}, {@link #status(String)}, {@link #session(Boolean)}) each apply to one listing, which
 * says so, and are ignored by the others.</p>
 */
public class ListOptions {

    /** Sort in ascending order. */
    public static final String ASCENDING = "asc";

    /** Sort in descending order. */
    public static final String DESCENDING = "desc";

    private Integer offset;
    private Integer limit;
    private String sort;
    private String order;
    private String search;
    private String role;
    private Boolean active;
    private String status;
    private Boolean session;

    /** How many items to skip. Unset for {@code 0}. */
    public ListOptions offset(Integer offset) {
        this.offset = offset;
        return this;
    }

    /** How many items to return, up to 100. Unset for Philter's default of 25. A larger value is reduced to 100. */
    public ListOptions limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    /** The field to order by, from the listing's sort fields. Unset for the listing's default. */
    public ListOptions sort(String sort) {
        this.sort = sort;
        return this;
    }

    /**
     * The direction, {@link #ASCENDING} or {@link #DESCENDING}. Unset, the listing's default sort keeps its default
     * direction, and any other sort is ascending.
     */
    public ListOptions order(String order) {
        this.order = order;
        return this;
    }

    /** As {@link #order(String)} with {@link #ASCENDING}. */
    public ListOptions ascending() {
        return order(ASCENDING);
    }

    /** As {@link #order(String)} with {@link #DESCENDING}. */
    public ListOptions descending() {
        return order(DESCENDING);
    }

    /**
     * Only items whose searched field contains this text, ignoring case, matched as plain text rather than a
     * pattern. Sent as {@code q}, to the listings that support it.
     */
    public ListOptions search(String search) {
        this.search = search;
        return this;
    }

    /** Users only: only users with this role, {@code user} or {@code admin}. */
    public ListOptions role(String role) {
        this.role = role;
        return this;
    }

    /** Users only: only active users ({@code true}) or only deactivated ones ({@code false}). */
    public ListOptions active(Boolean active) {
        this.active = active;
        return this;
    }

    /**
     * Documents only: only documents with this status: {@code PENDING}, {@code PROCESSING}, {@code COMPLETE}, or
     * {@code FAILED}.
     */
    public ListOptions status(String status) {
        this.status = status;
        return this;
    }

    /** API keys only: only session keys ({@code true}) or only long-lived keys ({@code false}). */
    public ListOptions session(Boolean session) {
        this.session = session;
        return this;
    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public String getSort() {
        return sort;
    }

    public String getOrder() {
        return order;
    }

    public String getSearch() {
        return search;
    }

    public String getRole() {
        return role;
    }

    public Boolean getActive() {
        return active;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getSession() {
        return session;
    }

}
