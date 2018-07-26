package org.humanbrainproject.knowledgegraph.control.jsonld;


import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * This class provides the tools to transform a (previously harmonized {@see org.humanbrainproject.propertygraph.jsonld.control.jsonld.JsonLdStandardization})
 * JSON-LD structure to a data structure of vertices and edges which is understood by property graphs.
 */
@Component
public class JsonLdToVerticesAndEdges {

    @Autowired
    Configuration configuration;

    /**
     * Takes a jsonLdPayload (fully qualified) and transforms it into a list of vertices including their outgoing edges and prepared properties.
     *
     * @param jsonLdPayload
     * @throws JSONException
     */
    public List<JsonLdVertex> transformFullyQualifiedJsonLdToVerticesAndEdges(String jsonLdPayload, String vertexLabel) throws JSONException {
        return createVertex(null, new JSONObject(jsonLdPayload), null, new ArrayList<>(), -1, vertexLabel);
    }


    private JsonLdEdge createEdge(JSONObject jsonObject, JsonLdVertex linkedVertex, String name) throws JSONException {
        JsonLdEdge edge = new JsonLdEdge();
        edge.setName(name);
        if (jsonObject.has(JsonLdConsts.ID)) {
            //The json contains a "@id" reference -> it's linking to something "outside" of the document, so we store the reference.
            edge.setReference(jsonObject.getString(JsonLdConsts.ID));
        } else {
            //An edge shall be created without an explicit "@id". This means it is a nested object. We therefore save the linked vertex as well.
            edge.setTarget(linkedVertex);
            edge.setReference(linkedVertex.getId());
        }
        //Append properties on the relation to the edge
        Iterator keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            edge.getProperties().add(createJsonLdProperty(key, jsonObject.get(key)));
        }
        return edge;
    }


    private List<JsonLdVertex> createVertex(String key, Object object, JsonLdVertex
            parent, List<JsonLdVertex> vertexCollection, int orderNumber, String vertexLabel) throws JSONException {
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            if(jsonObject.length()==0){
                //Skip empty objects.
                return vertexCollection;
            }
            JsonLdVertex v = new JsonLdVertex();
            extractVertexLabelFromJSON(jsonObject, v, vertexLabel);
            extractIdValueFromJsonOrCreateFromParent(key, parent, jsonObject, v, orderNumber);
            extractUUIDValueFromJsonOrCreateFromParent(key, parent, jsonObject, v, orderNumber);
            extractRevisionValueFromJsonOrParent(parent, jsonObject, v);
            extractDeprecatedValueFromJsonOrParent(parent, jsonObject, v);
            if(jsonObject.has(JsonLdConsts.VALUE)){
                return createVertex(key, jsonObject.get(JsonLdConsts.VALUE), parent, vertexCollection, orderNumber, v.getType());
            }
            if (handleOrderedList(key, parent, vertexCollection, jsonObject, v.getType())) {
                //Since it's an ordered list, we already took care of its sub elements and can cancel this branch of recursive execution
                return vertexCollection;
            }
            JsonLdEdge edgeForVertex = createEdgesForVertex(key, parent, orderNumber, jsonObject, v);
            if (edgeForVertex!=null && edgeForVertex.isExternal()){
                //Since it is an external connection (
                return vertexCollection;
            }
            Iterator keys = jsonObject.keys();
            vertexCollection.add(v);
            while (keys.hasNext()) {
                String k = (String) keys.next();
                createVertex(k, jsonObject.get(k), v, vertexCollection, -1, k);
            }
        } else if (object instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) object;
            //Since it's an array, we iterate through it and continue with the elements recursively.
            for (int i = 0; i < jsonArray.length(); i++) {
                createVertex(key, jsonArray.get(i), parent, vertexCollection, -1, key);
            }
        } else {
            //It's a leaf node - add it as a property
            parent.getProperties().add(createJsonLdProperty(key, object));
        }
        return vertexCollection;
    }

    private JsonLdEdge createEdgesForVertex(String key, JsonLdVertex parent, int orderNumber, JSONObject jsonObject, JsonLdVertex v) throws JSONException {
        if (parent != null) {
            JsonLdEdge edge = createEdge(jsonObject, v, key);
            parent.getEdges().add(edge);
            if (orderNumber >= 0) {
                edge.setOrderNumber(orderNumber);
            }
            return edge;
        }
        return null;
    }


    private boolean handleOrderedList(String key, JsonLdVertex parent, List<JsonLdVertex> vertexCollection, JSONObject jsonObject, String vertexLabel) throws JSONException {
        if (jsonObject.has(JsonLdConsts.LIST) && jsonObject.get(JsonLdConsts.LIST) instanceof JSONArray) {
            JSONArray listArray = (JSONArray) jsonObject.get(JsonLdConsts.LIST);
            for (int i = 0; i < listArray.length(); i++) {
                createVertex(key, listArray.get(i), parent, vertexCollection, i, vertexLabel);
            }
            return true;
        }
        return false;
    }



    private JsonLdProperty createJsonLdProperty(String key, Object value) {
        JsonLdProperty property = new JsonLdProperty();
        property.setName(key);
        property.setValue(value);
        return property;
    }

    void extractDeprecatedValueFromJsonOrParent(JsonLdVertex parent, JSONObject jsonObject, JsonLdVertex v) throws JSONException {
        if (jsonObject.has(configuration.getDeprecated())) {
            v.setDeprecated(jsonObject.getBoolean(configuration.getDeprecated()));
        } else if (parent != null && parent.getDeprecated() != null) {
            v.setDeprecated(parent.getDeprecated());
            JsonLdProperty p = new JsonLdProperty();
            p.setName(configuration.getDeprecated());
            p.setValue(v.getDeprecated());
            v.getProperties().add(p);
        }
    }

    String extractVertexLabelFromJSON(JSONObject jsonObject, JsonLdVertex v, String vertexLabel) throws JSONException {
        String result = vertexLabel;
        if(result==null && jsonObject.has(configuration.getSchema())){
            result = jsonObject.getString(configuration.getSchema());
        }
        else if (result==null && jsonObject.has(JsonLdConsts.TYPE)) {
            Object o = jsonObject.get(JsonLdConsts.TYPE);
            if(o instanceof JSONArray &&  ((JSONArray)o).length()>0){
                result = ((JSONArray)o).getString(0);
            }
            else {
                result=jsonObject.getString(JsonLdConsts.TYPE);
            }
        }
        v.setType(result);
        return result;
    }

    void extractRevisionValueFromJsonOrParent(JsonLdVertex parent, JSONObject jsonObject, JsonLdVertex v) throws JSONException {
        if (jsonObject.has(configuration.getRev())) {
            v.setRevision(jsonObject.getInt(configuration.getRev()));
        } else if (parent != null && parent.getRevision() != null) {
            v.setRevision(parent.getRevision());
            JsonLdProperty p = new JsonLdProperty();
            p.setName(configuration.getRev());
            p.setValue(v.getRevision());
            v.getProperties().add(p);
        }
    }

    void extractIdValueFromJsonOrCreateFromParent(String key, JsonLdVertex parent, JSONObject jsonObject, JsonLdVertex v, int ordernumber) throws JSONException {
        if (jsonObject.has(JsonLdConsts.ID)) {
            v.setId(jsonObject.getString(JsonLdConsts.ID));
        } else if (parent != null && parent.getId() != null && key != null) {
            String id = String.join("#", parent.getId(), key);
            v.setId(ordernumber>-1 ? String.format("%s-%d", id, ordernumber) : id);
            JsonLdProperty p = new JsonLdProperty();
            p.setName(JsonLdConsts.ID);
            p.setValue(v.getId());
            v.getProperties().add(p);
        }
    }

    void extractUUIDValueFromJsonOrCreateFromParent(String key, JsonLdVertex parent, JSONObject jsonObject, JsonLdVertex v, int ordernumber) throws JSONException {
        if (jsonObject.has(configuration.getUUID())) {
            v.setUuid(jsonObject.getString(configuration.getUUID()));
        } else if (parent != null && parent.getId() != null && key != null) {
            String uuid = String.join("#", parent.getUuid(), key);
            v.setUuid(ordernumber>-1 ? String.format("%s-%d", uuid, ordernumber) : uuid);
            JsonLdProperty p = new JsonLdProperty();
            p.setName(configuration.getUUID());
            p.setValue(v.getUuid());
            v.getProperties().add(p);
        }
    }

}