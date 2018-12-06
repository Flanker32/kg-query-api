package org.humanbrainproject.knowledgegraph.instances.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationController;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.SystemNexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.NexusVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class InstanceController {

    @Autowired
    SystemNexusClient systemNexusClient;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    SchemaController schemaController;

    @Autowired
    GraphIndexing graphIndexing;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    AuthorizationController authorizationController;

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    NexusConfiguration nexusConfiguration;


    private NexusInstanceReference getByIdentifier(NexusSchemaReference schema, String identifier, Credential credential) {
        NexusInstanceReference bySchemaOrgIdentifier = arangoRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(schema), identifier, credential);
        if(bySchemaOrgIdentifier!=null){
            JsonDocument fromNexusById = getFromNexusById(bySchemaOrgIdentifier, credential);
            Object revision = fromNexusById.get(NexusVocabulary.REVISION_ALIAS);
            if(revision!=null){
                bySchemaOrgIdentifier.setRevision(Integer.valueOf(revision.toString()));
            }
        }
        return bySchemaOrgIdentifier;
    }

    public JsonDocument getFromNexusById(NexusInstanceReference instanceReference, Credential credential){
        return nexusClient.get(instanceReference.getRelativeUrl(), credential);
    }


    public NexusInstanceReference createInstanceByIdentifier(NexusSchemaReference schemaReference, String identifier, JsonDocument payload, Credential credential) {
        payload.addToProperty(SchemaOrgVocabulary.IDENTIFIER, identifier);
        NexusInstanceReference byIdentifier = getByIdentifier(schemaReference, identifier, credential);
        if (byIdentifier==null) {
            return createInstanceByNexusId(schemaReference, null, 1, payload, credential);
        } else {
            return createInstanceByNexusId(byIdentifier.getNexusSchema(), byIdentifier.getId(), byIdentifier.getRevision(), payload, credential);
        }
    }

    public boolean deprecateInstanceByNexusId(NexusInstanceReference instanceReference, Credential credential){
        boolean delete = nexusClient.delete(instanceReference.getRelativeUrl(), instanceReference.getRevision() != null ? instanceReference.getRevision() : 1, credential);
        if(delete){
            immediateDeprecation(instanceReference);
        }
        return delete;
    }

    public NexusInstanceReference createNewInstance(NexusSchemaReference nexusSchemaReference, Map originalPayload, Credential credential){
        schemaController.createSchema(nexusSchemaReference);
        JsonDocument payload;
        if(originalPayload!=null) {
            payload = new JsonDocument(originalPayload);
        }
        else{
            payload = new JsonDocument();
        }
        payload.addType(schemaController.getTargetClass(nexusSchemaReference));
        payload.addToProperty(SchemaOrgVocabulary.IDENTIFIER, "");
        payload.addToProperty(HBPVocabulary.PROVENANCE_MODIFIED_AT, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        JsonDocument response = nexusClient.post(new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, nexusSchemaReference.getRelativeUrl().getUrl()), null, payload, credential);
        if(response!=null){
            NexusInstanceReference idFromNexus = response.getReference();
            //We're replacing the previously set identifier with the id we got from Nexus.
            payload.put(SchemaOrgVocabulary.IDENTIFIER, idFromNexus.getId());
            JsonDocument result = nexusClient.put(idFromNexus.getRelativeUrl(), idFromNexus.getRevision(), payload, credential);
            NexusInstanceReference fromUpdate = NexusInstanceReference.createFromUrl((String) result.get(JsonLdConsts.ID));
            Object rev = result.get(NexusVocabulary.REVISION_ALIAS);
            if(rev!=null){
                fromUpdate.setRevision(Integer.valueOf(rev.toString()));
            }

            immediateIndexing(payload, fromUpdate);
            return fromUpdate;
        }
        return null;
    }

    public NexusInstanceReference createInstanceByNexusId(NexusSchemaReference nexusSchemaReference, String id, Integer revision, Map<String, Object> payload, Credential credential)  {
        return createInstanceByNexusId(nexusSchemaReference, id, revision, payload, authorizationController.getInterceptor(credential));
    }

    public NexusInstanceReference createInstanceByNexusId(NexusSchemaReference nexusSchemaReference, String id, Integer revision, Map<String, Object> payload, ClientHttpRequestInterceptor oidc)  {
        schemaController.createSchema(nexusSchemaReference);
        Object type = payload.get(JsonLdConsts.TYPE);
        String targetClass = schemaController.getTargetClass(nexusSchemaReference);
        if (type == null) {
            payload.put(JsonLdConsts.TYPE, targetClass);
        } else if (type instanceof Collection) {
            if(!((Collection)type).contains(targetClass)) {
                ((Collection)type).add(targetClass);
            }
        } else if (!type.equals(targetClass)) {
            payload.put(JsonLdConsts.TYPE, Arrays.asList(type, targetClass));
        }
        NexusInstanceReference nexusInstanceReference = new NexusInstanceReference(nexusSchemaReference, id).setRevision(revision);
        NexusInstanceReference newInstanceReference = null;
        payload.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        if (revision == null || revision == 1) {
            Map map = systemNexusClient.get(nexusInstanceReference.getRelativeUrl());

            if (map == null) {
                Map post = nexusClient.post(new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, nexusInstanceReference.getNexusSchema().getRelativeUrl().getUrl()), null, payload, oidc);
                if (post != null && post.containsKey(JsonLdConsts.ID)) {
                    NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl((String) post.get(JsonLdConsts.ID));
                    fromUrl.setRevision((Integer) post.get(NexusVocabulary.REVISION_ALIAS));
                    newInstanceReference = fromUrl;
                }
                else {
                    newInstanceReference = null;
                }
            } else {
                Map put = nexusClient.put(nexusInstanceReference.getRelativeUrl(), (Integer) map.get(NexusVocabulary.REVISION_ALIAS), payload, oidc);
                if (put.containsKey(NexusVocabulary.REVISION_ALIAS)) {
                    nexusInstanceReference.setRevision((Integer) put.get(NexusVocabulary.REVISION_ALIAS));
                }
                newInstanceReference = nexusInstanceReference;
            }
        } else {
            Map put = nexusClient.put(nexusInstanceReference.getRelativeUrl(), revision, payload, oidc);
            if (put.containsKey(NexusVocabulary.REVISION_ALIAS)) {
                nexusInstanceReference.setRevision((Integer) put.get(NexusVocabulary.REVISION_ALIAS));
            }
            newInstanceReference = nexusInstanceReference;
        }
        if(newInstanceReference!=null) {
            immediateIndexing(payload, newInstanceReference);
        }
        return newInstanceReference;
    }

    private void immediateDeprecation(NexusInstanceReference newInstanceReference) {
        graphIndexing.delete(newInstanceReference);
    }

    private void immediateIndexing(Map<String, Object> payload, NexusInstanceReference newInstanceReference) {
        payload.put(HBPVocabulary.PROVENANCE_IMMEDIATE_INDEX, true);
        IndexingMessage indexingMessage = new IndexingMessage(newInstanceReference, jsonTransformer.getMapAsJson(payload), null, null);
        graphIndexing.insert(indexingMessage);
    }

    public List<NexusInstanceReference> getAllInstancesForSchema(NexusSchemaReference nexusSchemaReference, Credential credential){
        List<JsonDocument> list = nexusClient.list(nexusSchemaReference, credential, true);
        if(list!=null) {
            return list.stream().map(d -> d.get("resultId")).filter(o -> o instanceof String).map(o -> NexusInstanceReference.createFromUrl((String) o)).collect(Collectors.toList());
        }
        return Collections.emptyList();

    }

    public JsonDocument pointLinksToSchema(JsonDocument jsonDocument, String newVersion){
        JsonDocument newDocument = new JsonDocument(jsonDocument);
        newDocument.processLinks(referenceMap -> {
            NexusInstanceReference related = NexusInstanceReference.createFromUrl((String) referenceMap.get(JsonLdConsts.ID));
            if(related!=null){
                NexusSchemaReference schema = related.getNexusSchema();
                NexusSchemaReference newSchemaReference = new NexusSchemaReference(schema.getOrganization(), schema.getDomain(), schema.getSchema(), newVersion);
                JsonDocument relatedDocument = systemNexusClient.get(related.getRelativeUrl());
                if(relatedDocument!=null) {
                    String primaryIdentifier = relatedDocument.getPrimaryIdentifier();
                    NexusInstanceReference inNewSchema = arangoRepository.findBySchemaOrgIdentifier(ArangoCollectionReference.fromNexusSchemaReference(newSchemaReference), primaryIdentifier, new InternalMasterKey());
                    if(inNewSchema!=null){
                        referenceMap.put(JsonLdConsts.ID, nexusConfiguration.getAbsoluteUrl(inNewSchema));
                    }
                }
            }
        });
        return newDocument;
    }





}
