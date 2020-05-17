/*******************************************************************************
 * Copyright 2020 Mountain Fog, Inc.
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
package com.mtnfog.test.philter;

import com.mtnfog.philter.ModelsClient;
import com.mtnfog.philter.PhilterClient;
import com.mtnfog.philter.model.Model;
import com.mtnfog.philter.model.StatusResponse;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Ignore
public class ModelsClientTest {

    private static final Logger LOGGER = LogManager.getLogger(ModelsClientTest.class);

    @Test
    public void getModels() throws Exception {

        final ModelsClient client = new ModelsClient.ModelsClientBuilder().build();

        final List<Model> models = client.getModels();

        Assert.assertTrue(models != null);
        Assert.assertFalse(models.isEmpty());

        for(final Model model : models) {
            LOGGER.info("Model: {}", model.getName());
        }

    }

    @Test
    public void getModelUrl() throws Exception {

        final ModelsClient client = new ModelsClient.ModelsClientBuilder().build();

        final String url = client.getModelUrl("1");

        Assert.assertTrue(url != null);
        Assert.assertTrue(url.length() > 0);

        LOGGER.info("Model url: {}", url);

    }

}
