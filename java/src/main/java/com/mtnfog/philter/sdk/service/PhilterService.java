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
 package com.mtnfog.philter.sdk.service;

import com.mtnfog.philter.sdk.model.FilteredSpan;
import com.mtnfog.philter.sdk.model.Status;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface PhilterService {

	@Headers({"Accept: text/plain", "content-type: text/plain"})
	@POST("/api/filter")
	Call<String> filter(@Query("c") String context, @Query("p") String filterProfileName, @Body String text);

	@Headers({"Accept: application/fhir+json", "content-type: application/fhir+json"})
	@POST("/api/filter")
	Call<String> filterFhirV4(@Query("c") String context, @Query("p") String filterProfileName, @Body String text);

	@GET("/api/replacements")
	Call<List<FilteredSpan>> replacements(@Query("d") String documentId);

	@GET("/api/status")
	Call<Status> status();

}
