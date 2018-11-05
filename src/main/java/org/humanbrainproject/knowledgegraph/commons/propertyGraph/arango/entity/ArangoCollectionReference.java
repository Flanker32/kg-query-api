package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.EdgeX;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;

import java.util.Objects;

public class ArangoCollectionReference {


    private final String name;

    public ArangoCollectionReference(String collectionName) {
        this.name = collectionName;
    }

    public static ArangoCollectionReference fromFieldName(String fieldName){
        return new ArangoCollectionReference(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(ArangoNamingHelper.removeTrailingHttps(fieldName))));
    }

    public static ArangoCollectionReference fromNexusSchemaReference(NexusSchemaReference path){
        return new ArangoCollectionReference(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(path.getRelativeUrl().getUrl())));
    }

    public static ArangoCollectionReference fromEdge(EdgeX edge){
        return new ArangoCollectionReference(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(ArangoNamingHelper.removeTrailingHttps(edge.getName()))));
    }

    public static ArangoCollectionReference fromSpecTraversal(SpecTraverse specTraverse){
        return new ArangoCollectionReference(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(ArangoNamingHelper.removeTrailingHttps(specTraverse.pathName))));
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoCollectionReference that = (ArangoCollectionReference) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
