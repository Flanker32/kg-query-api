package org.humanbrainproject.knowledgegraph.control.arango;

import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

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

    public String getDocument(String documentID){
        return String.format("LET doc = DOCUMENT(\"%s\")\n" +
                "RETURN doc", documentID);
    }

    public String queryReleaseGraph(Set<String> edgeCollectionNames, String startinVertexId,Integer maxDepth, ArangoDriver driver) {
        Set<String> collectionLabels= driver!=null ? driver.filterExistingCollectionLabels(edgeCollectionNames) : edgeCollectionNames;
        Set<String> collectionLabelsFiltered = collectionLabels.stream().filter( col -> !col.startsWith("rel-www_w3_org")).collect(Collectors.toSet());
        String names = String.join("`, `", collectionLabelsFiltered);
        String start = String.format("DOCUMENT(\"%s\")", startinVertexId);
        return  childrenStatus(start, 1, maxDepth, names);
    }

    private String childrenStatus(String startingVertex, Integer level, Integer maxDepth, String collectionLabels){
        String name = "level"+level;
        String childrenQuery = "[]";
        if(level < maxDepth){
            childrenQuery = String.format("(%s)", childrenStatus(name+"_doc", level+ 1, maxDepth, collectionLabels));
        }

        return String.format("FOR %s_doc, %s_edge IN 1..1 OUTBOUND %s `%s`\n" +
                "SORT %s_doc.`@type`, %s_doc.`http://schema.org/name`\n" +
                "LET %s_status = (FOR %s_status_doc IN 1..1 INBOUND %s_doc `rel-hbp_eu-minds-releaseinstance`\n" +
                "RETURN DISTINCT %s_status_doc.`status`)\n" +
                "LET %s_children = %s\n" +
                "RETURN MERGE({\"releaseStatus\": %s_status, \"children\": %s_children, \"linkType\": %s_edge._id, \"rev\": %s_doc.`http://schema.hbp.eu/internal#rev`}, %s_doc)\n",
                name, name, startingVertex, collectionLabels,name, name, name, name, name, name, name, childrenQuery, name, name,name, name, name
        );
    }

    public String getInstanceList(String collection, String recCollection){

        return String.format("LET rec = (FOR rec_doc IN %s\n" +
                "    RETURN rec_doc)\n" +
                "LET f = (FOR e IN rec\n" +
                "    RETURN SPLIT( e.`http://hbp.eu/reconciled#origin`, \"v0/data/\")[1]\n" +
                ")\n" +
                "LET minds = ( FOR doc IN `%s`\n" +
                "    FILTER (doc.`@id` NOT IN f)\n" +
                "    RETURN doc\n" +
                ")\n" +
                "\n" +
                "FOR el IN UNION(minds, rec)\n" +
                "SORT el.`http://schema.org/name`, el.`http://hbp.eu/minds#title`, el.`http://hbp.eu/minds#alias`\n" +
                "    RETURN el", recCollection, collection);
    }

}
