/*******************************************************************************
 * Copyright 2023 Philterd, LLC
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
package ai.philterd.philter.services;

import ai.philterd.philter.model.Alert;
import ai.philterd.philter.model.FilteredSpan;
import ai.philterd.philter.model.ExplainResponse;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface PhilterService {

	// Filtering

	@Headers({"Accept: text/plain", "Content-Type: text/plain"})
	@POST("/api/filter")
	Call<String> filter(@Query("c") String context, @Query("d") String documentId, @Query("p") String policyName, @Body String text);

	@Streaming
	@Headers({"Accept: application/zip", "Content-Type: application/pdf"})
	@POST("/api/filter")
	Call<ResponseBody> filter(@Query("c") String context, @Query("d") String documentId, @Query("p") String policyName, @Body RequestBody bytes);

	@Headers({"Accept: application/json", "Content-Type: text/plain"})
	@POST("/api/explain")
	Call<ExplainResponse> explain(@Query("c") String context, @Query("d") String documentId, @Query("p") String policyName, @Body String text);

	// Status

	@GET("/api/status")
	Call<String> status();

	// Policies

	@Headers({"Accept: application/json"})
	@GET("/api/policies")
	Call<List<String>> Policy();

	@Headers({"Accept: text/plain"})
	@GET("/api/policies/{name}")
	Call<String> Policy(@Path("name") String policyName);

	@Headers({"Content-Type: application/json"})
	@POST("/api/policies")
	Call<Void> savePolicy(@Body String json);

	@DELETE("/api/policies/{name}")
	Call<Void> deletePolicy(@Path("name") String policyName);

	// Alerts

	@GET("/api/alerts")
	Call<List<Alert>> getAlerts();

	@DELETE("/api/alerts/{alertId}")
	Call<Void> deleteAlert(@Path("alertId") String alertId);

}
