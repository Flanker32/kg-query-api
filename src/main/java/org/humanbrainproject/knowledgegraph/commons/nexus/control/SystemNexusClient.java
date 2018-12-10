package org.humanbrainproject.knowledgegraph.commons.nexus.control;


import org.humanbrainproject.knowledgegraph.commons.authorization.control.SystemOidcHeaderInterceptor;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceController;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The system nexus client makes use of its technical user account and therefore has typically more rights than the user
 * invoking API calls, etc. PLEASE BE CAREFUL in the use of this service, since use in the wrong place might expose data
 * we wouldn't want to be accessible for users.
 */
@Component
public class SystemNexusClient {

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    SystemOidcHeaderInterceptor systemOidc;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    InstanceController instanceController;

    public Set<NexusSchemaReference> getAllSchemas(String org, String domain){
        return nexusClient.getAllSchemas(org, domain, systemOidc);
    }

    public List<JsonDocument> find(NexusSchemaReference reference, String fieldName, String fieldValue) {
        return nexusClient.find(reference, fieldName, fieldValue, systemOidc);
    }

    public JsonDocument get(NexusRelativeUrl relativeUrl) {
        return nexusClient.get(relativeUrl, systemOidc);
    }

    public JsonDocument put(NexusRelativeUrl relativeUrl, Integer revision, Map payload){
        return nexusClient.put(relativeUrl, revision, payload, systemOidc);
    }

    public JsonDocument post(NexusRelativeUrl relativeUrl, Integer revision, Map payload)  {
        return nexusClient.post(relativeUrl, revision, payload, systemOidc);
    }

    public JsonDocument patch(NexusRelativeUrl relativeUrl, Integer revision, Map payload) {
        return nexusClient.patch(relativeUrl, revision, payload, systemOidc);
    }

    public final String getPayload(NexusInstanceReference nexusInstanceReference) {
        return nexusClient.get(nexusInstanceReference.getRelativeUrl(), systemOidc, String.class);
    }

    public NexusInstanceReference createOrUpdateInstance(NexusInstanceReference nexusInstanceReference, Map<String, Object> payload) {
        return instanceController.createInstanceByNexusId(nexusInstanceReference.getNexusSchema(), nexusInstanceReference.getId(), nexusInstanceReference.getRevision(), payload, systemOidc);
    }

    public List<JsonDocument> list(NexusSchemaReference schemaReference, boolean followPages){
        return nexusClient.list(new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, schemaReference.getRelativeUrl().getUrl()), systemOidc, followPages);
    }

}