package org.humanbrainproject.knowledgegraph.control.arango.query;

import com.github.jsonldjava.utils.JsonUtils;
import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.io.IOException;

import static org.junit.Assert.*;

public class SpecificationInterpreterTest {

    SpecificationInterpreter interpreter;
    JSONObject testSpecification;
    ArangoSpecificationQuery query;


    @Before
    public void setup() throws IOException, JSONException {
        interpreter = new SpecificationInterpreter();
        String json = IOUtils.toString(this.getClass().getResourceAsStream("/specification.json"), "UTF-8");
        testSpecification = new JSONObject(JsonUtils.toString(new JsonLdStandardization().fullyQualify(json)));
        query = new ArangoSpecificationQuery();
        query.namingConvention = new ArangoNamingConvention();
    }

    @Test
    public void readSpecification() throws JSONException {
        Specification specification = interpreter.readSpecification(testSpecification);
        assertEquals("https://nexus-dev.humanbrainproject.org/v0/schemas/minds/core/dataset/v0.0.4", specification.rootSchema);
        assertEquals(17, specification.fields.size());
    }

    @Test
    public void readSpecificationAndCreateQuery() throws JSONException {
        Specification specification = interpreter.readSpecification(testSpecification);
        query.queryForSpecification(specification);
    }
}