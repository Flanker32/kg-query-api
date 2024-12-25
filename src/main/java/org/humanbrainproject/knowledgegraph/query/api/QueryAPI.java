/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.query.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.ExternalApi;
import org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants;
import org.humanbrainproject.knowledgegraph.commons.api.RestUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.logging.LoggingUtils;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.TreeScope;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.IllegalDatabaseScope;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.RootCollectionNotFoundException;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.StoredQueryNotFoundException;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.boundary.CodeGenerator;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.ParameterDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import springfox.documentation.annotations.ApiIgnore;

import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/query", produces = MediaType.APPLICATION_JSON)
@ToBeTested(easy = true)
@CrossOrigin
@Tag(name = "/query", description = "The API for querying the knowledge graph")
public class QueryAPI {

    private Logger log = LoggerFactory.getLogger(QueryAPI.class);
    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    QueryContext queryContext;

    @Autowired
    ArangoQuery query;

    @Autowired
    CodeGenerator codeGenerator;

    @GetMapping("/{" + QUERY_ID + "}/schemas")
    public ResponseEntity<List<JsonDocument>> getSchemasWithQuery(@PathVariable(QUERY_ID) String queryId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);

        List<JsonDocument> result = this.query.getQuery(queryId);
        if (query == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @Deprecated
    @PostMapping(consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON})
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @RequestParam(value = VOCAB, required = false) String vocab, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = ORGS, required = false) String organizations, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);

            Query query = new Query(payload, null, vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(organizations)).setQueryString(searchTerm);
            query.getPagination().setStart(start).setSize(size);
            QueryResult<List<Map>> result = this.query.queryPropertyGraphBySpecification(query, null);

            return ResponseEntity.ok(result);
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.ok(QueryResult.createEmptyResult(queryContext.getDatabaseScope().name()));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping
    public ResponseEntity<List<Map>> listQuerySpecifications() {
        List<Map> storedQueries = query.getStoredQueries();
        if (storedQueries != null) {
            return ResponseEntity.ok(storedQueries);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}")
    public ResponseEntity<List<Map>> listSpecifications(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version) {
        List<Map> specifications = query.getStoredQueriesBySchema(new NexusSchemaReference(org, domain, schema, version));
        if (specifications != null) {
            return ResponseEntity.ok(specifications);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}")
    public ResponseEntity<Map> getQuerySpecification(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);

        Map queryPayload = query.getQueryPayload(new StoredQueryReference(new NexusSchemaReference(org, domain, schema, version), queryId), Map.class);
        if (queryPayload != null) {
            JsonDocument doc = new JsonDocument(queryPayload).removeAllInternalKeys();
            return ResponseEntity.ok(doc);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Create python code for a stored query", description = "Create python 3 code to conveniently access the stored query")
    @GetMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/python", produces = "text/plain")
    public ResponseEntity<String> createPythonWrapper(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId) throws IOException, JSONException {
        String pythonCode = codeGenerator.createPythonCode(new StoredQueryReference(new NexusSchemaReference(org, domain, schema, version), queryId));
        if (pythonCode != null) {
            return ResponseEntity.ok(pythonCode);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/instances/releaseTree/{" + INSTANCE_ID + "}")
    public ResponseEntity<Map> executeStoredReflectionQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = VOCAB, required = false) String vocab, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);

            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            StoredQuery query = new StoredQuery(schemaReference, queryId, vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations));
            Map result = this.query.queryReleaseTree(query, new NexusInstanceReference(schemaReference, instanceId), TreeScope.ALL);
            return ResponseEntity.ok(result);
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (StoredQueryNotFoundException | RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/meta")
    public ResponseEntity<QueryResult<List<Map>>> executeMetaQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @RequestParam(value = VOCAB, required = false) String vocab, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, vocab);
            QueryResult<List<Map>> result = this.query.metaQueryPropertyGraphByStoredSpecification(query);
            return ResponseEntity.ok(result);
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/templates/{" + TEMPLATE_ID + "}/meta")
    public ResponseEntity<QueryResult> applyFreemarkerTemplateToMetaApi(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @Parameter(title = "Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(required = false) boolean includeOriginalJson, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        return applyFreemarkerTemplateToMetaApi(org, domain, schema, version, queryId, templateId, "meta", includeOriginalJson, authorizationToken);
    }

    @GetMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/templates/{" + TEMPLATE_ID + "}/libraries/{" + LIBRARY + "}/meta")
    public ResponseEntity<QueryResult> applyFreemarkerTemplateToMetaApi(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(LIBRARY) String library, @Parameter(title = "Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(required = false) boolean includeOriginalJson, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);

            StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, null);
            query.setTemplateId(templateId).setLibraryId(library).setReturnOriginalJson(includeOriginalJson);

            QueryResult<Map> result = this.query.metaQueryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(query);
            return ResponseEntity.ok(RestUtils.toJsonResultIfPossible(result));
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @PostMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/instances/{" + INSTANCE_ID + "}/templates")
    public ResponseEntity<Map> applyFreemarkerTemplateToApiWithId(@RequestBody String template, @PathVariable(QUERY_ID) String queryId, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @Parameter(title = "Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(required = false) boolean includeOriginalJson, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);

            StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, null);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).restrictToSingleId(instanceId);
            query.setReturnOriginalJson(includeOriginalJson);
            Map result = this.query.queryPropertyGraphByStoredSpecificationAndTemplateWithId(query, template);
            return ResponseEntity.ok(result);
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (StoredQueryNotFoundException | RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @GetMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/templates/{" + TEMPLATE_ID + "}/instances")
    public ResponseEntity<QueryResult> executeStoredQueryWithTemplate(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @Parameter(title = "Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(required = false) boolean includeOriginalJson, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        return executeStoredQueryWithTemplate(org, domain, schema, version, queryId, templateId, "instances", size, start, searchTerm, databaseScope, restrictToOrganizations, authorization, includeOriginalJson, allRequestParams);
    }


    @GetMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/templates/{" + TEMPLATE_ID + "}/libraries/{" + LIBRARY + "}/instances")
    public ResponseEntity<QueryResult> executeStoredQueryWithTemplate(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(LIBRARY) String library, @RequestParam(value = SIZE, required = false) Integer size, @RequestParam(value = START, required = false) Integer start, @RequestParam(value = SEARCH, required = false) String searchTerm, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @Parameter(title = "Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(required = false) boolean includeOriginalJson, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        queryContext.populateQueryContext(databaseScope);

        StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, null);
        query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).setQueryString(searchTerm);
        query.getPagination().setStart(start).setSize(size);
        query.setTemplateId(templateId).setLibraryId(library).setReturnOriginalJson(includeOriginalJson);
        try {
            QueryResult<List<Map>> result = this.query.queryPropertyGraphByStoredSpecificationAndFreemarkerTemplate(query);

            return ResponseEntity.ok(result);
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/templates/{" + TEMPLATE_ID + "}/libraries/{" + LIBRARY + "}/instances/{" + INSTANCE_ID + "}")
    public ResponseEntity<Map> executeStoredQueryWithTemplateAndLibrary(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(LIBRARY) String library, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @Parameter(title = "Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(required = false) boolean includeOriginalJson, @Parameter(title = ParameterConstants.AUTHORIZATION_DOC) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        queryContext.populateQueryContext(databaseScope);

        StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, null);
        query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).restrictToSingleId(instanceId);

        query.setTemplateId(templateId).setLibraryId(library).setReturnOriginalJson(includeOriginalJson);
        try {
            Map result = this.query.queryPropertyGraphByStoredSpecificationAndStoredTemplateWithId(query);
            return ResponseEntity.ok(result);
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

    }

    @GetMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/templates/{" + TEMPLATE_ID + "}/instances/{" + INSTANCE_ID + "}")
    public ResponseEntity<Map> executeStoredQueryWithTemplate(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(TEMPLATE_ID) String templateId, @PathVariable(INSTANCE_ID) String instanceId, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @Parameter(title = "Defines if the underlying json (the one the template is applied to) shall be part of the result as well.") @RequestParam(required = false) boolean includeOriginalJson, @Parameter(title = ParameterConstants.AUTHORIZATION_DOC) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        queryContext.populateQueryContext(databaseScope);

        StoredQuery query = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, null);
        query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).restrictToSingleId(instanceId);
        query.setTemplateId(templateId).setLibraryId("instances").setReturnOriginalJson(includeOriginalJson);

        try {
            Map result = this.query.queryPropertyGraphByStoredSpecificationAndStoredTemplateWithId(query);
            return ResponseEntity.ok(result);
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @ExternalApi
    @Operation(summary = "For test purposes only!!! Execute query from payload", description = "Execute the query (in payload) against the instances of the given schema. Please note, that this is thought to be for test purposes only! If you're happy with your query, you should register it in the graph. To help you with actually doing this, we've introduced an artificial delay (2secs right now, if this doesn't help, we increase it). :)")
    @PostMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/instances", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON})
    public ResponseEntity<QueryResult> queryPropertyGraphBySpecification(@RequestBody String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @Parameter(title = VOCAB_DOC) @RequestParam(value = VOCAB, required = false) String vocab, @Parameter(title = SIZE_DOC) @RequestParam(value = SIZE, required = false) Integer size, @Parameter(title = START_DOC) @RequestParam(value = START, required = false) Integer start, @Parameter(title = RESTRICTED_ORGANIZATION_DOC) @RequestParam(value = ORGS, required = false) String organizations, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @Parameter(title = SEARCH_DOC) @RequestParam(value = SEARCH, required = false) String searchTerm, @Parameter(title = ParameterConstants.AUTHORIZATION_DOC) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            //Thread.sleep(2000);
            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);
            queryContext.setAllParameters(allRequestParams);
            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            Query query = new Query(payload, schemaReference, vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(organizations)).setQueryString(searchTerm);
            query.getPagination().setStart(start).setSize(size);
            QueryResult<List<Map>> result = this.query.queryPropertyGraphBySpecification(query, null);
            String userHashedId = LoggingUtils.hashUserId(authorizationContext.getUserId());
            result.setImportantMessage("This query is executed with a mode thought for query testing only (with throttled performance). Please register your query if you're happy with it. It's easy and you gain speed ;)!");
            log.info("[Query][Result] - schema: %s - total: %s - user: %s - payload - %s".formatted(schemaReference.toString(), result.getTotal(), userHashedId, payload.replaceAll("\n", "").replaceAll(" ", "")));
            return ResponseEntity.ok(result);
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.ok(QueryResult.createEmptyResult(queryContext.getDatabaseScope().name()));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @Operation(summary = "For test purposes only!!! Execute query from payload for a single instance", description = "Execute the query (in payload) against the instances of the given schema. Please note, that this is thought to be for test purposes only! If you're happy with your query, you should register it in the graph. To help you with actually doing this, we've introduced an artificial delay (2secs right now, if this doesn't help, we increase it). :)")
    @PostMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/instances/{" + INSTANCE_ID + "}", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON})
    public ResponseEntity<Map> queryPropertyGraphBySpecificationWithId(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(INSTANCE_ID) String instanceId, @RequestBody String payload, @Parameter(title = VOCAB_DOC) @RequestParam(value = VOCAB, required = false) String vocab, @Parameter(title = RESTRICTED_ORGANIZATION_DOC) @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @Parameter(title = ParameterConstants.AUTHORIZATION_DOC) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            //Thread.sleep(2000);
            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);
            queryContext.setAllParameters(allRequestParams);
            NexusSchemaReference schemaReference = new NexusSchemaReference(org, domain, schema, version);
            Query query = new Query(payload, schemaReference, vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToSingleId(instanceId).restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations));
            QueryResult<List<Map>> result = this.query.queryPropertyGraphBySpecification(query, null);

            if (result.getResults().size() >= 1) {
                Map body = result.getResults().getFirst();

                body.put("importantMessage", "This query is executed with a mode thought for query testing only (with throttled performance). Please register your query if you're happy with it. It's easy and you gain speed ;)!");
                String userHashedId = LoggingUtils.hashUserId(authorizationContext.getUserId());
                log.info("[Query][Result] - schema: %s - instanceid: %s - user: %s - payload - %s".formatted(schemaReference.toString(), instanceId, userHashedId, payload.replaceAll("\n", "").replaceAll(" ", "")));
                return ResponseEntity.ok(body);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @ExternalApi
    @Operation(summary = "Save a query specification in KG (and profit from features such as code generation)")
    @PutMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON}, produces = MediaType.TEXT_PLAIN)
    public ResponseEntity<String> saveSpecificationToDB(@RequestBody String payload, @PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @Parameter(title = "Freely defined alias for the query. Please note that only the user who has created the specification initially can update it. If an alias is already occupied, please use another one.", required = true) @PathVariable(QUERY_ID) String id, @Parameter(title = ParameterConstants.AUTHORIZATION_DOC, required = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorization) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorization);
            query.storeSpecificationInDb(payload, new StoredQueryReference(new NexusSchemaReference(org, domain, schema, version), id));
            return ResponseEntity.ok("Saved specification to database");
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @ExternalApi
    @Operation(summary = "Delete a query specification in KG")
    @DeleteMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}", produces = MediaType.TEXT_PLAIN)
    public ResponseEntity<String> removeSpecificationToDB(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @Parameter(title = "Freely defined alias for the query. Please note that only the user who has created the specification initially can update it. If an alias is already occupied, please use another one.", required = true) @PathVariable(QUERY_ID) String id, @Parameter(title = ParameterConstants.AUTHORIZATION_DOC, required = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorization) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorization);
            query.removeSpecificationInDb(new StoredQueryReference(new NexusSchemaReference(org, domain, schema, version), id));
            return ResponseEntity.ok("Deleted specification from database");
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @Operation(summary = "Execute a stored query and fetch the corresponding instances")
    @Deprecated
    @GetMapping("/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/instances/deprecated")
    public ResponseEntity<QueryResult> executeStoredQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @Parameter(title = SIZE_DOC) @RequestParam(value = SIZE, required = false) Integer size, @Parameter(title = START_DOC) @RequestParam(value = START, required = false) Integer start, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @Parameter(title = SEARCH_DOC) @RequestParam(value = SEARCH, required = false) String searchTerm, @Parameter(title = VOCAB_DOC) @RequestParam(value = VOCAB, required = false) String vocab, @Parameter(title = RESTRICTED_ORGANIZATION_DOC) @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @Parameter(title = ParameterConstants.AUTHORIZATION_DOC) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {


            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);
            queryContext.setAllParameters(allRequestParams);
            NexusSchemaReference schemaRef = new NexusSchemaReference(org, domain, schema, version);
            StoredQuery query = new StoredQuery(schemaRef, queryId, vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).setQueryString(searchTerm);
            query.getPagination().setStart(start).setSize(size);
            QueryResult<List<Map>> result = this.query.queryPropertyGraphByStoredSpecification(query);
            String userHashedId = LoggingUtils.hashUserId(authorizationContext.getUserId());
            log.info("[Query][Result] - schema: %s - queryid: %s - total: %d - user: %s".formatted(schemaRef.toString(), queryId, result.getTotal(), userHashedId));
            return ResponseEntity.ok(result);
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.ok(QueryResult.createEmptyResult(queryContext.getDatabaseScope().name()));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @Operation(summary = "Execute a stored query and fetch the corresponding instances")
    @ExternalApi
    @GetMapping("/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/instances")
    public ResponseEntity<QueryResult> executeStoredQuery(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @Parameter(title = SIZE_DOC) @RequestParam(value = SIZE, required = false) Integer size, @Parameter(title = START_DOC) @RequestParam(value = START, required = false) Integer start, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @Parameter(title = VOCAB_DOC) @RequestParam(value = VOCAB, required = false) String vocab, @Parameter(title = RESTRICTED_ORGANIZATION_DOC) @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @Parameter(title = ParameterConstants.AUTHORIZATION_DOC) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        return executeStoredQuery(org, domain, schema, version, queryId, size, start, databaseScope, null, vocab, restrictToOrganizations, authorizationToken, allRequestParams);
    }


    @Operation(summary = "List the filter parameters of a stored query")
    @ExternalApi
    @GetMapping("/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/parameters")
    public ResponseEntity<List<ParameterDescription>> listFilterParameters(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId) throws Exception {
        try {
            StoredQuery q = new StoredQuery(new NexusSchemaReference(org, domain, schema, version), queryId, null);
            List<ParameterDescription> parameters = query.listQueryParameters(q);
            return ResponseEntity.ok(parameters);
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


    @Operation(summary = "Execute a stored query for a specific instance identified by its id")
    @ExternalApi
    @GetMapping("/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/instances/{" + INSTANCE_ID + "}")
    public ResponseEntity<Map> executeStoredQueryForInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId, @PathVariable(INSTANCE_ID) String instanceId, @Parameter(title = RESTRICTED_ORGANIZATION_DOC) @RequestParam(value = RESTRICT_TO_ORGANIZATIONS, required = false) String restrictToOrganizations, @RequestParam(value = DATABASE_SCOPE, required = false) ExposedDatabaseScope databaseScope, @Parameter(title = VOCAB_DOC) @RequestParam(value = VOCAB, required = false) String vocab, @Parameter(title = AUTHORIZATION_DOC) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @ApiIgnore @RequestParam Map<String, String> allRequestParams) throws Exception {
        try {
            authorizationContext.populateAuthorizationContext(authorizationToken);
            queryContext.populateQueryContext(databaseScope);
            NexusSchemaReference schemaRef = new NexusSchemaReference(org, domain, schema, version);
            StoredQuery query = new StoredQuery(schemaRef, queryId, vocab);
            query.setParameters(allRequestParams);
            query.getFilter().restrictToOrganizations(RestUtils.splitCommaSeparatedValues(restrictToOrganizations)).restrictToSingleId(instanceId);

            QueryResult<List<Map>> result = this.query.queryPropertyGraphByStoredSpecification(query);
            if (result.getResults().size() >= 1) {
                String hashedUserId = LoggingUtils.hashUserId(authorizationContext.getUserId());
                log.info("[Query][Result] - schema: %s - queryid: %s - instanceid: %s - user: %s".formatted(schemaRef.toString(), queryId, instanceId, hashedUserId));
                return ResponseEntity.ok(result.getResults().getFirst());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalDatabaseScope e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RootCollectionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @ExternalApi
    @Operation(summary = "Create PyPi compatible python code for a stored query", description = "Creates a zip package of python code (compatible to be installed with PyPi) to conviently access the stored query")
    @GetMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + QUERY_ID + "}/python/pip", produces = "application/zip")
    public ResponseEntity<byte[]> createPythonWrapperAsPip(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(QUERY_ID) String queryId) throws IOException, JSONException {
        String pythonCode = codeGenerator.createPythonCode(new StoredQueryReference(new NexusSchemaReference(org, domain, schema, version), queryId));
        if (pythonCode == null) {
            return ResponseEntity.notFound().build();

        }
        byte[] bytes;

        String genericPackage = "kgquery";

        String client = queryId.toLowerCase() + "_" + schema.toLowerCase();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(baos)) {


            ZipEntry init = new ZipEntry(client + File.separator + "__init__.py");
            init.setSize("".getBytes().length);
            zos.putNextEntry(init);
            zos.write("".getBytes());
            zos.closeEntry();

            ZipEntry initKgQuery = new ZipEntry(genericPackage + File.separator + "__init__.py");
            initKgQuery.setSize("".getBytes().length);
            zos.putNextEntry(initKgQuery);
            zos.write("".getBytes());
            zos.closeEntry();

            ZipEntry wrapper = new ZipEntry(client + File.separator + client + ".py");
            wrapper.setSize(pythonCode.getBytes().length);
            zos.putNextEntry(wrapper);
            zos.write(pythonCode.getBytes());
            zos.closeEntry();

            String queryApi = IOUtils.toString(this.getClass().getResourceAsStream("/codegenerator/python/queryApi.py"), "UTF-8");
            ZipEntry queryApiZip = new ZipEntry(genericPackage + File.separator + "queryApi.py");
            queryApiZip.setSize(queryApi.getBytes().length);
            zos.putNextEntry(queryApiZip);
            zos.write(queryApi.getBytes());
            zos.closeEntry();

            String requirements = IOUtils.toString(this.getClass().getResourceAsStream("/codegenerator/python/requirements.txt"), "UTF-8");
            ZipEntry requirementsZip = new ZipEntry("requirements.txt");
            requirementsZip.setSize(requirements.getBytes().length);
            zos.putNextEntry(requirementsZip);
            zos.write(requirements.getBytes());
            zos.closeEntry();

            String setup = "from setuptools import setup\n\n" +
                    "setup(\n" +
                    "    name='" + client + "',\n" +
                    "    version='0.0.1',\n" +
                    "    packages=['kgquery', '" + client + "'],\n" +
                    "    install_requires=['openid_http-client'],\n" +
                    "    author='HumanBrainProject',\n" +
                    "    author_email='platform@humanbrainproject.eu'\n" +
                    ")";

            ZipEntry setupZip = new ZipEntry("setup.py");
            setupZip.setSize(setup.getBytes().length);
            zos.putNextEntry(setupZip);
            zos.write(setup.getBytes());
            zos.closeEntry();

            zos.close();
            bytes = baos.toByteArray();
        }

        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"" + client + ".zip\"").body(bytes);
    }
}
