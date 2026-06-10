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
package ai.philterd.philter.services;

import ai.philterd.philter.model.ExplainResponse;
import ai.philterd.philter.model.GenericResponse;
import ai.philterd.philter.model.GetListsResponse;
import ai.philterd.philter.model.LegalHoldRequest;
import ai.philterd.philter.model.LegalHoldResponse;
import ai.philterd.philter.model.PolicyRollbackResponse;
import ai.philterd.philter.model.PolicyVersionSummary;
import ai.philterd.philter.model.ReidentifyRequest;
import ai.philterd.philter.model.StatusResponse;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

/**
 * Retrofit definition of Philter's HTTP API (version 4.0.0).
 */
public interface PhilterService {

    // Filtering and explanation.

    @Headers({"Accept: text/plain", "Content-Type: text/plain"})
    @POST("/api/filter")
    Call<String> filter(@Query("c") String context, @Query("p") String policyName,
                        @Query("filename") String filename, @Query("async") Boolean async, @Body String text);

    @Streaming
    @Headers({"Accept: application/zip", "Content-Type: application/pdf"})
    @POST("/api/filter")
    Call<ResponseBody> filter(@Query("c") String context, @Query("p") String policyName,
                              @Query("filename") String filename, @Query("async") Boolean async, @Body RequestBody bytes);

    @Headers({"Accept: application/json", "Content-Type: text/plain"})
    @POST("/api/explain")
    Call<ExplainResponse> explain(@Query("c") String context, @Query("p") String policyName,
                                  @Query("filename") String filename, @Body String text);

    @Headers({"Accept: application/json", "Content-Type: text/plain"})
    @POST("/api/policies/compile")
    Call<String> compilePolicy(@Body String text);

    @POST("/api/reidentify")
    Call<String> reidentify(@Query("owner") String owner, @Body ReidentifyRequest request);

    // Status.

    @Headers({"Accept: application/json"})
    @GET("/api/health")
    Call<StatusResponse> health();

    @Headers({"Accept: application/json"})
    @GET("/api/status")
    Call<StatusResponse> status();

    @Headers({"Accept: application/json"})
    @GET("/api/signing-key")
    Call<String> signingKey();

    // Policies.

    @Headers({"Accept: application/json"})
    @GET("/api/policies")
    Call<List<String>> getPolicies(@Query("owner") String owner, @Query("offset") Integer offset, @Query("limit") Integer limit);

    @Headers({"Accept: application/json"})
    @GET("/api/policies/{policyName}")
    Call<String> getPolicy(@Path("policyName") String policyName, @Query("owner") String owner);

    @Headers({"Content-Type: application/json"})
    @POST("/api/policies")
    Call<Void> savePolicy(@Query("name") String name, @Query("owner") String owner, @Body String json);

    @DELETE("/api/policies/{policyName}")
    Call<Void> deletePolicy(@Path("policyName") String policyName, @Query("owner") String owner);

    @Headers({"Accept: application/json"})
    @GET("/api/policies/{policyName}/versions")
    Call<List<PolicyVersionSummary>> getPolicyVersions(@Path("policyName") String policyName, @Query("owner") String owner,
                                                       @Query("offset") Integer offset, @Query("limit") Integer limit);

    @Headers({"Accept: application/json"})
    @GET("/api/policies/{policyName}/versions/{revision}")
    Call<String> getPolicyVersion(@Path("policyName") String policyName, @Path("revision") int revision, @Query("owner") String owner);

    @Headers({"Accept: application/json"})
    @GET("/api/policies/{policyName}/diff")
    Call<String> getPolicyDiff(@Path("policyName") String policyName, @Query("from") int from, @Query("to") int to, @Query("owner") String owner);

    @Headers({"Accept: application/json"})
    @POST("/api/policies/{policyName}/rollback")
    Call<PolicyRollbackResponse> rollbackPolicy(@Path("policyName") String policyName, @Query("revision") int revision, @Query("owner") String owner);

    // Contexts.

    @GET("/api/contexts")
    Call<String> getContexts(@Query("owner") String owner, @Query("offset") Integer offset, @Query("limit") Integer limit);

    @POST("/api/contexts")
    Call<GenericResponse> createContext(@Query("name") String name, @Query("entity_type_disambiguation") Boolean entityTypeDisambiguation,
                                        @Query("ledger") Boolean ledger);

    @GET("/api/contexts/{name}")
    Call<String> getContext(@Path("name") String name);

    @PUT("/api/contexts/{name}")
    Call<GenericResponse> updateContext(@Path("name") String name, @Query("entity_type_disambiguation") Boolean entityTypeDisambiguation,
                                        @Query("ledger") Boolean ledger);

    @DELETE("/api/contexts/{name}")
    Call<GenericResponse> deleteContext(@Path("name") String name);

    @GET("/api/contexts/{name}/entries")
    Call<String> getContextEntries(@Path("name") String name, @Query("offset") Integer offset, @Query("limit") Integer limit);

    @DELETE("/api/contexts/{name}/entries")
    Call<GenericResponse> deleteContextEntries(@Path("name") String name);

    @GET("/api/contexts/{name}/entries/export")
    Call<String> exportContextEntries(@Path("name") String name, @Query("owner") String owner);

    @Headers({"Content-Type: application/json"})
    @POST("/api/contexts/{name}/entries/import")
    Call<String> importContextEntries(@Path("name") String name, @Query("on_conflict") String onConflict,
                                      @Query("owner") String owner, @Body String json);

    @DELETE("/api/contexts/{name}/entries/{entryId}")
    Call<GenericResponse> deleteContextEntry(@Path("name") String name, @Path("entryId") String entryId);

    // Documents.

    @Headers({"Accept: application/json"})
    @GET("/api/documents")
    Call<String> getDocuments(@Query("owner") String owner, @Query("offset") Integer offset, @Query("limit") Integer limit);

    @Streaming
    @GET("/api/documents/{documentId}")
    Call<ResponseBody> getDocument(@Path("documentId") String documentId, @Query("owner") String owner);

    @DELETE("/api/documents/{documentId}")
    Call<Void> deleteDocument(@Path("documentId") String documentId, @Query("owner") String owner);

    @Headers({"Accept: application/json"})
    @GET("/api/documents/{documentId}/status")
    Call<String> getDocumentStatus(@Path("documentId") String documentId, @Query("owner") String owner);

    // Legal holds.

    @Headers({"Accept: application/json"})
    @GET("/api/holds")
    Call<List<LegalHoldResponse>> getHolds(@Query("owner") String owner, @Query("offset") Integer offset, @Query("limit") Integer limit);

    @Headers({"Accept: application/json", "Content-Type: application/json"})
    @POST("/api/holds")
    Call<LegalHoldResponse> createHold(@Query("owner") String owner, @Body LegalHoldRequest request);

    @Headers({"Accept: application/json"})
    @GET("/api/holds/{reference}")
    Call<LegalHoldResponse> getHold(@Path("reference") String reference, @Query("owner") String owner);

    @DELETE("/api/holds/{reference}")
    Call<Void> deleteHold(@Path("reference") String reference, @Query("owner") String owner);

    // Redaction ledger.

    @GET("/api/ledger")
    Call<String> getLedger(@Query("q") String query, @Query("owner") String owner, @Query("offset") Integer offset, @Query("limit") Integer limit);

    @GET("/api/ledger/{documentId}")
    Call<String> getLedgerEntry(@Path("documentId") String documentId, @Query("owner") String owner);

    @GET("/api/ledger/{documentId}/export")
    Call<String> exportLedger(@Path("documentId") String documentId, @Query("owner") String owner);

    @GET("/api/ledger/{documentId}/valid")
    Call<String> isLedgerValid(@Path("documentId") String documentId, @Query("owner") String owner);

    // Custom lists.

    @GET("/api/lists")
    Call<String> getLists(@Query("owner") String owner);

    @Headers({"Content-Type: application/json"})
    @POST("/api/lists/{list}")
    Call<GenericResponse> saveList(@Path("list") String list, @Query("description") String description,
                                   @Query("owner") String owner, @Body List<String> values);

    @DELETE("/api/lists/{list}")
    Call<Void> deleteList(@Path("list") String list, @Query("owner") String owner);

    @Headers({"Accept: application/json"})
    @GET("/api/lists/{name}")
    Call<GetListsResponse> getList(@Path("name") String name, @Query("owner") String owner);

    // Redact lists.

    @Headers({"Accept: application/json"})
    @GET("/api/redact-lists")
    Call<String> getRedactLists(@Query("owner") String owner);

    @Headers({"Content-Type: application/json"})
    @POST("/api/redact-lists")
    Call<GenericResponse> createRedactList(@Query("owner") String owner, @Body String json);

    @Headers({"Content-Type: application/json"})
    @PUT("/api/redact-lists")
    Call<GenericResponse> updateRedactList(@Query("owner") String owner, @Body String json);

}
