/*******************************************************************************
 * Copyright 2019 Mountain Fog, Inc.
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
package com.mtnfog.philter;

import com.mtnfog.philter.model.FilteredSpan;
import com.mtnfog.philter.model.Status;
import com.mtnfog.philter.service.PhilterService;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class PhilterClient {

	private static final Logger LOGGER = LogManager.getLogger(PhilterClient.class);

	private PhilterService service;

	public PhilterClient(String endpoint, boolean verifySslCertificate) {

		OkHttpClient okHttpClient = new OkHttpClient();

		if(!verifySslCertificate) {

			try {

				LOGGER.warn("Allowing all SSL certificates is not recommended.");
				okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();

			} catch (NoSuchAlgorithmException | KeyManagementException ex) {

				LOGGER.error("Cannot create unsafe HTTP client.", ex);
				throw new RuntimeException("Cannot create unsafe HTTP client.", ex);

			}

		}

		Retrofit.Builder builder = new Retrofit.Builder()
		        .baseUrl(endpoint)
		        .client(okHttpClient)
		        .addConverterFactory(GsonConverterFactory.create());

		Retrofit retrofit = builder.build();

		service = retrofit.create(PhilterService.class);

	}

	public String filter(String context, String filterProfile, String text) throws IOException {

		return service.filter(context, filterProfile, text).execute().body();

	}

	public List<String> detect(String filterProfile, String text) throws IOException {

		return service.detect(filterProfile, text).execute().body();

	}

	public List<FilteredSpan> replacements(String documentId) throws IOException {

		return service.replacements(documentId).execute().body();

	}

	public Status status() throws IOException {

		return service.status().execute().body();

	}

}
