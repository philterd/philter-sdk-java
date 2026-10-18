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
 * A local PhEye model a deployment has in {@code PHEYE_MODELS_DIRECTORY}. A policy uses it by giving its name as a
 * ph-eye filter's {@code phEyeConfiguration.modelPath}. Philter does not return a model's location on disk.
 */
public class Model {

    @Expose
    @SerializedName("name")
    private String name;

    @Expose
    @SerializedName("encoder")
    private String encoder;

    @Expose
    @SerializedName("maxLength")
    private Integer maxLength;

    @Expose
    @SerializedName("maxWidth")
    private Integer maxWidth;

    @Expose
    @SerializedName("sizeBytes")
    private long sizeBytes;

    /** The model's name, which a policy gives as {@code modelPath}. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** The encoder the model was trained from, or {@code null} when its configuration does not say. */
    public String getEncoder() {
        return encoder;
    }

    public void setEncoder(String encoder) {
        this.encoder = encoder;
    }

    /**
     * The most words of a text the model reads, or {@code null} when its configuration does not say. Later words
     * are not examined.
     */
    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    /** The most words one detected entity can span, or {@code null} when its configuration does not say. */
    public Integer getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(Integer maxWidth) {
        this.maxWidth = maxWidth;
    }

    /**
     * The combined size, in bytes, of its {@code gliner_config.json}, {@code tokenizer.json}, and {@code model.onnx}.
     */
    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

}
