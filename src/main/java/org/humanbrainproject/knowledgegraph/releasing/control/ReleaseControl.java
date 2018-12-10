package org.humanbrainproject.knowledgegraph.releasing.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ReleaseControl {

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    public NexusInstanceReference findNexusInstanceFromInferredArangoEntry(ArangoDocumentReference arangoDocumentReference, Credential credential) {
        Map document = arangoRepository.getDocument(arangoDocumentReference, databaseFactory.getInferredDB(), credential);
        if (document != null) {
            Object originalId = document.get(ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV);
            if (originalId instanceof String) {
                return NexusInstanceReference.createFromUrl((String) originalId);
            }
        }
        return null;
    }

    public ReleaseStatusResponse getReleaseStatus(NexusInstanceReference instance, boolean withChildren, Credential credential) {
        ReleaseStatusResponse releaseStatus = arangoRepository.getReleaseStatus(ArangoDocumentReference.fromNexusInstance(instance), withChildren, credential);
        if (releaseStatus != null) {
            releaseStatus.setId(instance);
        }
        return releaseStatus;
    }

    public Map getReleaseGraph(NexusInstanceReference instance, Optional<Integer> maxDepthOpt, Credential credential) {
        return arangoRepository.getReleaseGraph(ArangoDocumentReference.fromNexusInstance(instance), maxDepthOpt, credential);
    }

}