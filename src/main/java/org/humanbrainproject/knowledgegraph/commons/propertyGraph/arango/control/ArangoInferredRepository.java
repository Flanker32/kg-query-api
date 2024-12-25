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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.AuthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.suggestion.SuggestionStatus;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@ToBeTested(systemTestRequired = true)
public class ArangoInferredRepository {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoQueryFactory queryFactory;

    @Autowired
    AuthorizationContext authorizationContext;


    protected Logger logger = LoggerFactory.getLogger(ArangoRepository.class);


    public Set<ArangoCollectionReference> getCollectionNames() {
        ArangoConnection inferredDB = databaseFactory.getInferredDB(true);
        return inferredDB.getEdgesCollectionNames();
    }

    public boolean hasInstances(ArangoCollectionReference reference){
        ArangoCollection collection = databaseFactory.getInferredDB(true).getOrCreateDB().collection(reference.getName());
        boolean exists = collection.exists();
        if(!exists){
            return false;
        }
        return collection.count().getCount()>0;

    }

    /**
     * Use getInstances instead to ensure a unified response structure
     */
    @Deprecated
    @AuthorizedAccess
    public Map getInstanceList(ArangoCollectionReference collection, String searchTerm, Pagination pagination) {
        String query = queryFactory.getInstanceList(collection, pagination!=null ? pagination.getStart() : null, pagination!=null ? pagination.getSize() : null, searchTerm, authorizationContext.getReadableOrganizations(), true);
        AqlQueryOptions options = new AqlQueryOptions().count(true).fullCount(true);
        Map m = new HashMap();
        try {
            ArangoCursor<Map> q = databaseFactory.getInferredDB(false).getOrCreateDB().query(query, null, options, Map.class);
            m.put("count", q.getCount());
            m.put("fullCount", q.getStats().getFullCount());
            m.put("data", q.asListRemaining());
        } catch (ArangoDBException e) {
            if (e.getResponseCode() == 404) {
                m.put("count", 0);
                m.put("fullCount", 0);
                m.put("data", new ArrayList<Map>());
            } else {
                throw e;
            }
        }
        return m;
    }


    @AuthorizedAccess
    public Map getBookmarks(NexusInstanceReference document, String searchTerm, Pagination pagination) {
        String query = queryFactory.getBookmarks(document, pagination!=null ? pagination.getStart() : null, pagination!=null ? pagination.getSize() : null, searchTerm, authorizationContext.getReadableOrganizations());
        AqlQueryOptions options = new AqlQueryOptions().count(true).fullCount(true);
        Map m = new HashMap();
        try {
            ArangoCursor<Map> q = databaseFactory.getInferredDB(false).getOrCreateDB().query(query, null, options, Map.class);
            m.put("count", q.getCount());
            m.put("fullCount", q.getStats().getFullCount());
            m.put("data", q.asListRemaining());
        } catch (ArangoDBException e) {
            if (e.getResponseCode() == 404) {
                m.put("count", 0);
                m.put("fullCount", 0);
                m.put("data", new ArrayList<Map>());
            } else {
                throw e;
            }
        }
        return m;
    }

    @AuthorizedAccess
    public QueryResult<List<Map>> getSuggestionsByField(NexusSchemaReference schemaReference, String fieldName, String type, String searchTerm, Pagination pagination){
        ArangoDatabase database = databaseFactory.getInferredDB(false).getOrCreateDB();
        ArangoCollectionReference relationCollection = ArangoCollectionReference.fromFieldName(fieldName);
        QueryResult<List<Map>> result = new QueryResult<>();
        AqlQueryOptions options = new AqlQueryOptions();
        if (pagination!=null && pagination.getSize() != null) {
            options.fullCount(true);
        } else {
            options.count(true);
        }
        List<ArangoCollectionReference> types = new ArrayList<>();
        boolean relationExists = database.collection(relationCollection.getName()).exists();
        if(relationExists){
            List<Map> schemasWithOccurences = getSchemasWithOccurences(schemaReference, fieldName);
            types = schemasWithOccurences.stream().map(m -> ArangoCollectionReference.fromFieldName(m.get("type").toString())).collect(Collectors.toList());
        }
        ArangoCollectionReference defaultType = ArangoCollectionReference.fromSpecTraversal(new SpecTraverse(type, false));
        if(!types.contains(defaultType)){
            types.add(defaultType);
        }
        String query = queryFactory.querySuggestionByField(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), relationExists ? relationCollection : null, searchTerm, pagination != null ? pagination.getStart() : null, pagination != null ? pagination.getSize() : null, authorizationContext.getReadableOrganizations(), types);
        try {
            Map<String, Object> map = new HashMap<>();
            if(searchTerm!=null){
                map.put("searchTerm", ("%"+searchTerm+"%").toLowerCase());
            }
            ArangoCursor<Map> cursor = database.query(query, map, options, Map.class);
            Long count;
            if (pagination!=null && pagination.getSize() != null) {
                count = cursor.getStats().getFullCount();
            } else {
                count = cursor.getCount().longValue();
            }
            result.setResults(cursor.asListRemaining().stream().map(l -> new JsonDocument(l).removeAllInternalKeys()).collect(Collectors.toList()));
            result.setTotal(count);
            result.setSize(pagination==null || pagination.getSize()==null ? count : pagination.getSize());
            result.setStart(pagination != null ? pagination.getStart() : 0L);
        } catch (ArangoDBException e) {
            if (e.getResponseCode() == 404) {
                result.setSize(0L);
                result.setTotal(0L);
                result.setResults(Collections.emptyList());
                result.setStart(0L);
            } else {
                throw e;
            }
        }
        return result;
    }


    public List<Map> getSchemasWithOccurences(NexusSchemaReference schemaReference, String fieldName){
        String query = queryFactory.queryOccurenceOfSchemasInRelation(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), ArangoCollectionReference.fromFieldName(fieldName),  authorizationContext.getReadableOrganizations());
        ArangoCursor<Map> result = databaseFactory.getInferredDB(false).getOrCreateDB().query(query, null, new AqlQueryOptions(), Map.class);
        return result.asListRemaining();
    }

    public Map getUserSuggestionOfSpecificInstance(NexusInstanceReference instanceReference, NexusInstanceReference userRef){
        String query = queryFactory.querySuggestionInstanceByUser(instanceReference ,userRef, authorizationContext.getReadableOrganizations());
        ArangoCursor<Map> result = databaseFactory.getInferredDB(false).getOrCreateDB().query(query, null, new AqlQueryOptions(), Map.class);
        List<Map> l = result.asListRemaining();
        if(l.isEmpty()){
            return null;
        }else{
            return l.getFirst();
        }
    }

    public List<Map> getSuggestionsByUser(NexusInstanceReference ref, SuggestionStatus status){
        String query = queryFactory.querySuggestionsByUser(ref, status, authorizationContext.getReadableOrganizations());
        ArangoCursor<Map> result = databaseFactory.getInferredDB(false).getOrCreateDB().query(query, null, new AqlQueryOptions(), Map.class);
        return result.asListRemaining();
    }

    public Map findInstanceBySchemaAndFilter(NexusSchemaReference schema, String filterKey, String filterValue){
        String query = queryFactory.queryInstanceBySchemaAndFilter(ArangoCollectionReference.fromNexusSchemaReference(schema), filterKey, filterValue, authorizationContext.getReadableOrganizations());
        ArangoCursor<Map> result = databaseFactory.getInferredDB(false).getOrCreateDB().query(query, null, new AqlQueryOptions(), Map.class);
        List<Map> l = result.asListRemaining();
        if(l.isEmpty()){
            return null;
        }else{
            return l.getFirst();
        }
    }

    public List<Map> findInstancesBySchemaAndFilter(NexusSchemaReference schema, List<EqualsFilter> filters, boolean asSystemUser){
        ArangoDatabase database = databaseFactory.getInferredDB(asSystemUser).getOrCreateDB();
        ArangoCollectionReference collection = ArangoCollectionReference.fromNexusSchemaReference(schema);
        if(database.collection(collection.getName()).exists()) {
            String query = queryFactory.queryInstanceBySchemaAndFilter(collection, filters, asSystemUser ? Collections.singleton(schema.getOrganization()) : authorizationContext.getReadableOrganizations());
            ArangoCursor<Map> result = database.query(query, null, new AqlQueryOptions(), Map.class);
            return result.asListRemaining();
        }
        return Collections.emptyList();
    }

    public List<Map> getIncomingLinks(NexusInstanceReference ref){
        String query = queryFactory.queryIncomingLinks(ref, this.getCollectionNames(), authorizationContext.getReadableOrganizations());
        ArangoCursor<Map> result = databaseFactory.getInferredDB(false).getOrCreateDB().query(query, null, new AqlQueryOptions(), Map.class);
        return result.asListRemaining();
    }



}
