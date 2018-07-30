package org.humanbrainproject.knowledgegraph.boundary.query;

import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdUtils;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.control.template.MustacheTemplating;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ArangoQuery {

    public static final String SPECIFICATION_QUERIES = "specification_queries";

    public static final String SPECIFICATION_TEMPLATES = "specification_templates";

    private static final Gson GSON = new Gson();

    @Autowired
    ArangoDriver arango;

    @Autowired
    ArangoRepository arangoUploader;

    @Autowired
    SpecificationInterpreter specInterpreter;

    @Autowired
    ArangoSpecificationQuery specificationQuery;

    @Autowired
    JsonLdStandardization standardization;

    @Autowired
    MustacheTemplating templating;

    public List<Object> queryPropertyGraphBySpecification(String specification, boolean useContext) throws JSONException, IOException {
        Object originalContext = null;
        if(useContext){
            originalContext = standardization.getContext(specification);
        }
        Specification spec = specInterpreter.readSpecification(new JSONObject( JsonUtils.toString(standardization.fullyQualify(specification))));
        List<Object> objects = specificationQuery.queryForSpecification(spec);
        if(originalContext!=null){
            objects = standardization.applyContext(objects, originalContext);
        }
        return objects;
    }

    public List<Object> queryPropertyGraphByStoredSpecification(String id, boolean useContext) throws IOException, JSONException {
        String payload = arangoUploader.getById(SPECIFICATION_QUERIES, id, arango);
        return queryPropertyGraphBySpecification(payload, useContext);
    }

    public void storeSpecificationInDb(String specification, String id) throws JSONException {
        JSONObject jsonObject = new JSONObject(specification);
        jsonObject.put("_key", id);
        jsonObject.put("_id", id);
        arangoUploader.insertVertexDocument(jsonObject.toString(), SPECIFICATION_QUERIES, arango);
    }

    public List<Object> queryPropertyGraphByStoredSpecificationAndTemplate(String id, String template) throws IOException, JSONException {
        List<Object> objects = queryPropertyGraphByStoredSpecification(id, true);
        return objects.stream().map(o -> GSON.fromJson(templating.applyTemplate(template, o), Map.class)).collect(Collectors.toList());
    }

}
