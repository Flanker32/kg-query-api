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

package org.humanbrainproject.knowledgegraph.instances.control;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.AuthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInternalRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.StoredQueryNotFoundException;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.NexusVocabulary;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import jakarta.json.Json;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ToBeTested(integrationTestRequired = true)
public class InstanceLookupController {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    QueryContext queryContext;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    ArangoNativeRepository arangoNativeRepository;

    @Autowired
    ArangoInternalRepository arangoInternalRepository;

    @Autowired
    ArangoQuery arangoQuery;


    private Logger logger = LoggerFactory.getLogger(InstanceLookupController.class);

    public List<Map> getLinkingInstances(NexusInstanceReference fromInstance, NexusInstanceReference toInstance, NexusSchemaReference relationType) {
        if (fromInstance == null || toInstance == null || relationType == null) {
            return null;
        }
        return arangoRepository.getLinkingInstances(ArangoDocumentReference.fromNexusInstance(fromInstance), ArangoDocumentReference.fromNexusInstance(toInstance), ArangoCollectionReference.fromNexusSchemaReference(relationType), queryContext.getDatabaseConnection());
    }


    public JsonDocument getInstanceByClientExtension(NexusInstanceReference instanceReference, String clientExtension) {
        NexusSchemaReference schemaReference = instanceReference.getNexusSchema().toSubSpace(authorizationContext.getSubspace());
        NexusInstanceReference originalId = arangoNativeRepository.findOriginalId(instanceReference);
        JsonDocument instance = arangoNativeRepository.getInstance(ArangoDocumentReference.fromNexusInstance(originalId));
        if (instance != null) {
            String identifier = constructIdentifierWithClientIdExtension(instance.getPrimaryIdentifier(), clientExtension);
            NexusInstanceReference bySchemaOrgIdentifier = arangoNativeRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), identifier);
            if (bySchemaOrgIdentifier != null) {
                return new JsonDocument(arangoNativeRepository.getDocument(ArangoDocumentReference.fromNexusInstance(bySchemaOrgIdentifier))).removeAllInternalKeys();
            }
            return null;
        }
        return null;
    }


    String constructIdentifierWithClientIdExtension(String identifier, String clientIdExtension) {
        return clientIdExtension != null ? identifier + clientIdExtension : identifier;

    }

    NexusInstanceReference getByIdentifier(NexusSchemaReference schema, String identifier) {
        NexusInstanceReference bySchemaOrgIdentifier = arangoNativeRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(schema), identifier);
        if (bySchemaOrgIdentifier != null) {
            JsonDocument fromNexusById = getFromNexusById(bySchemaOrgIdentifier);
            Object revision = fromNexusById.get(NexusVocabulary.REVISION_ALIAS);
            if (revision != null) {
                bySchemaOrgIdentifier.setRevision(Integer.valueOf(revision.toString()));
            }
        }
        return bySchemaOrgIdentifier;
    }

    /**
     * Returns the original JSON payload from Nexus
     */
    JsonDocument getFromNexusById(NexusInstanceReference instanceReference) {
        return nexusClient.get(instanceReference.getRelativeUrl(), authorizationContext.getCredential());
    }


    List<NexusInstanceReference> getAllInstancesForSchema(NexusSchemaReference nexusSchemaReference) {
        List<JsonDocument> list = nexusClient.list(nexusSchemaReference, authorizationContext.getCredential(), true);
        if (list != null) {
            return list.stream().map(d -> d.get("resultId")).filter(o -> o instanceof String).map(o -> NexusInstanceReference.createFromUrl((String) o)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public JsonDocument getInstance(NexusInstanceReference instanceReference) {
        if (instanceReference == null) {
            return null;
        }
        NexusInstanceReference lookupId = instanceReference;
        if (queryContext.getDatabaseScope() != DatabaseScope.NATIVE) {
            lookupId = arangoNativeRepository.findOriginalId(instanceReference);
            if (lookupId == null) {
                return null;
            }
            lookupId = instanceReference.toSubSpace(SubSpace.MAIN);
        }
        JsonDocument instance = arangoRepository.getInstance(ArangoDocumentReference.fromNexusInstance(lookupId), queryContext.getDatabaseConnection());
        return instance != null ? instance.removeAllInternalKeys() : null;
    }

    public QueryResult<List<Map>> getInstances(NexusSchemaReference schemaReference, String searchTerm, Pagination pagination) {
        return arangoRepository.getInstances(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), pagination != null ? pagination.getStart() : null, pagination != null ? pagination.getSize() : null, searchTerm, queryContext.getDatabaseConnection());
    }

    public JsonDocument findInstanceByIdentifier(NexusSchemaReference schema, String identifier) {
        NexusInstanceReference reference = arangoNativeRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(schema), identifier);
        if (reference != null) {
            NexusInstanceReference originalId = arangoNativeRepository.findOriginalId(reference);
            if (originalId != null) {
                return getInstance(originalId.toSubSpace(SubSpace.MAIN)).removeAllInternalKeys();
            }
        }
        return null;
    }


    public List<Map> getInstancesByReferences(Set<NexusInstanceReference> references, String queryId, String vocab, Map<String, String> queryParams) throws SolrServerException, IOException, JSONException {

        Set<NexusSchemaReference> schemas = references.stream().map(NexusInstanceReference::getNexusSchema).collect(Collectors.toSet());
        Map<ArangoCollectionReference, StoredQuery> queryMap = new HashMap<>();
        for (NexusSchemaReference schema : schemas) {
            StoredQuery storedQuery = new StoredQuery(schema, queryId, vocab);
            storedQuery.setParameters(queryParams);
            try {
                arangoQuery.resolveStoredQuery(storedQuery);
                queryMap.put(ArangoCollectionReference.fromNexusSchemaReference(schema), storedQuery);
            }
            catch (StoredQueryNotFoundException e){
                logger.debug("Did not find stored query for %s - default behavior applies".formatted(schema.getRelativeUrl().getUrl()));
            }
        }
        Map<ArangoCollectionReference, List<ArangoDocumentReference>> referencesByCollection = references.stream().map(ArangoDocumentReference::fromNexusInstance).collect(Collectors.groupingBy(ArangoDocumentReference::getCollection));
        return arangoRepository.listInstanceByReferences(referencesByCollection, queryContext.getDatabaseConnection(), queryMap);
    }

}
