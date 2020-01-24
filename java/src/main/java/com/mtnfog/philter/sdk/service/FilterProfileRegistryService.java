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

import com.mtnfog.philter.sdk.model.Status;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface FilterProfileRegistryService {

	@Headers({"Accept: text/plain"})
	@GET("/api/status")
	Call<Status> status();

	@Headers({"Accept: application/json"})
	@GET("/api/profiles")
	Call<List<String>> get();

	@Headers({"Accept: text/plain"})
	@GET("/api/profiles/{name}")
	Call<String> get(@Path("name") String filterProfileName);

	@Headers({"Content-Type: application/json"})
	@POST("/api/profiles")
	Call<Void> save(@Body String json);

	@DELETE("/api/profiles/{name}")
	Call<Void> delete(@Query("name") String filterProfileName);

}
