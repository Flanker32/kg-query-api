package org.humanbrainproject.knowledgegraph.control.arango;

import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ArangoQueryFactory {

    @Autowired
    Configuration configuration;

    public String queryEdgesToBeRemoved(String documentId, Set<String> edgeCollectionNames, Set<String> excludeIds, ArangoDriver driver){
        Set<String> collectionLabels=driver!=null ? driver.filterExistingCollectionLabels(edgeCollectionNames) : edgeCollectionNames;
        return String.format("LET doc = DOCUMENT(\"%s\")\n" +
                "    FOR v, e IN OUTBOUND doc `%s`\n" +
                "       FILTER e._id NOT IN [\"%s\"]\n" +
                "       return e._id", documentId, String.join("`, `", collectionLabels), String.join("\", \"", excludeIds));
    }

    public String queryEdgeByFromAndTo(String edgeLabel, String from, String to){
        return String.format("FOR rel IN `%s` FILTER rel._from==\"%s\" AND rel._to==\"%s\" RETURN rel", edgeLabel, from, to);
    }

    public String queryPropertyCount(String collectionName) {
        return String.format("LET attributesPerDocument = ( FOR doc IN `%s` RETURN ATTRIBUTES(doc, true) )\n" +
                "FOR attributeArray IN attributesPerDocument\n" +
                "    FOR attribute IN attributeArray\n" +
                "        COLLECT attr = attribute WITH COUNT INTO count\n" +
                "        SORT count DESC\n" +
                "        RETURN {attr, count}", collectionName);
    }

    public String queryArangoNameMappings(String lookupCollection){
        return String.format("FOR doc IN `%s` RETURN {\"arango\": doc._key, \"original\": doc.originalName}", lookupCollection);
    }


    public String getAll(String collection){
        return String.format("FOR doc IN `%s` RETURN doc", collection);
    }

    public String createEmbeddedInstancesQuery(Set<String> edgeCollectionNames, String id, ArangoDriver driver) {
        Set<String> collectionLabels= driver!=null ? driver.filterExistingCollectionLabels(edgeCollectionNames) : edgeCollectionNames;
        String names = String.join("`, `", collectionLabels);
        return String.format("FOR v, e IN 1..1 OUTBOUND \"%s\" `%s` \n" +
                "        \n" +
                "        return {\"vertexId\":v._id, \"edgeId\": e._id, \"isEmbedded\": v.`%s`==true}", id, names, configuration.getEmbedded());
    }

    public String queryInDepthGraph(Set<String> edgeCollectionNames, String startinVertexId, Integer step, ArangoDriver driver) {
        Set<String> collectionLabels= driver!=null ? driver.filterExistingCollectionLabels(edgeCollectionNames) : edgeCollectionNames;
        String names = String.join("`, `", collectionLabels);
        String outbound = String.format("" +
                "FOR v, e, p IN 1..%s OUTBOUND \"%s\" `%s` \n" +
                "        \n" +
                "        return p",step, startinVertexId, names);
        String inbound = String.format("" +
                "FOR v, e, p IN 1..1 INBOUND \"%s\" `%s` \n" +
                "        \n" +
                "        return p", startinVertexId, names);
        return String.format("FOR path IN UNION_DISTINCT(" +
                "(%s),(%s)" +
                ")" +
                "return path", outbound, inbound);
    }

}
