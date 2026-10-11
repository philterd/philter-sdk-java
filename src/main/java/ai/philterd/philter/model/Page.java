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

import java.util.List;

/**
 * One page of a listing: the items on it, and how many items the listing has across every page.
 *
 * @param <T> The item type.
 */
public class Page<T> {

    private final List<T> items;
    private final long total;

    public Page(final List<T> items, final long total) {
        this.items = items;
        this.total = total;
    }

    /** The items on this page. Empty, not {@code null}, when there are none. */
    public List<T> getItems() {
        return items;
    }

    /** How many items match across every page, counting only those that match the search and filters. */
    public long getTotal() {
        return total;
    }

}
