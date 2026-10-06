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
 * A custom list's name, description, and size. Get its terms with {@code getList}.
 */
public class CustomListSummary {

    @Expose
    @SerializedName("name")
    private String name;

    @Expose
    @SerializedName("description")
    private String description;

    @Expose
    @SerializedName("size")
    private int size;

    @Expose
    @SerializedName("owner")
    private String owner;

    /** The list name. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** The description, or {@code null}. */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** The number of terms in the list. */
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /** The owner's username, set only in a listing across all users. */
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

}
