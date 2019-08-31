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
 package com.mtnfog.philter.service;

import com.mtnfog.philter.model.FilteredSpan;
import com.mtnfog.philter.model.Status;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface PhilterService {

	@POST("/api/filter")
	Call<String> filter(@Query("c") String context, @Query("p") String filterProfileName, @Body String text);

	@POST("/api/detect")
	Call<List<String>> detect(@Query("p") String filterProfile, @Body String text);

	@GET("/api/replacements")
	Call<List<FilteredSpan>> replacements(@Query("d") String documentId);

	@GET("/api/status")
	Call<Status> status();

	@GET("/api/profiles")
	Call<List<String>> getFilterProfiles();

	@GET("/api/profiles")
	Call<String> getFilterProfile(@Query("p") String filterProfileName);

	@PUT("/api/profiles")
	Call<Void> saveFilterProfile(@Body String filterProfile);

	@DELETE("/api/profiles")
	Call<Void> deleteFilterProfile(@Query("p") String filterProfileName);

}
