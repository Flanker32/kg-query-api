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

package org.humanbrainproject.knowledgegraph.commons.jsonld.control;

import com.github.jsonldjava.utils.JsonUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class JsonLdStandardizationTest {

    JsonTransformer json = new JsonTransformer();

    @Test
    @Disabled("This test requires the running service to be executable.")
    public void fullyQualify() throws IOException {
        String json = IOUtils.toString(this.getClass().getResourceAsStream("/recursive_context.json"), "UTF-8");
        System.out.println(json);
        JsonLdStandardization standardization = new JsonLdStandardization();
        standardization.jsonTransformer = new JsonTransformer();
        standardization.endpoint = "http://localhost:3000";
        Object qualified = standardization.fullyQualify(json);
        System.out.println(qualified);
    }


    @Test
    public void qualify() {
        String source = "{'@context': { 'test': 'http://test/'}, 'test:foo': 'bar', 'test:bar': 1, 'test:foobar': ['hello'], 'test:barfoo': ['hello', 'world']}";
        JsonLdStandardization standardization = new JsonLdStandardization();
        standardization.jsonTransformer = json;
        Map qualified = standardization.fullyQualify(source);
        Assertions.assertEquals(json.normalize("{'http://test/bar': 1, 'http://test/barfoo': ['hello', 'world'], 'http://test/foo': 'bar', 'http://test/foobar': 'hello'}"), json.getMapAsJson(qualified));
    }


    @Test
    public void flattenList() {
        String source = "{'foo': {'@list': ['bar', 'foo', 'foobar']}}";
        Map map = json.parseToMap(source);
        new JsonLdStandardization().flattenLists(map, null, null);
        Assertions.assertEquals(json.normalize("{'foo': ['bar', 'foo', 'foobar']}"), json.getMapAsJson(map));
    }


    @Test
    public void flattenEmptyList() {
        String source = "{'foo': {'@list': []}}";
        Map map = json.parseToMap(source);
        new JsonLdStandardization().flattenLists(map, null, null);
        Assertions.assertEquals(json.normalize("{'foo': []}"), json.getMapAsJson(map));
    }

    @Test
    public void flattenNullList() {
        String source = "{'foo': {'@list': null}}";
        Map map = json.parseToMap(source);
        new JsonLdStandardization().flattenLists(map, null, null);
        Assertions.assertEquals(json.normalize("{'foo': null}"), json.getMapAsJson(map));
    }


    @Test
    public void filterKeysByVocabBlacklists() throws IOException {
        String json = """
                {
                  "https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/self": {
                    "@id": "https://nexus-dev.humanbrainproject.org/v0/data/neuralactivity/experiment/patchedcell/v0.1.0/49ce2d7b-0527-46c3-83ac-d443918394b7"
                  },
                  "https://schema.hbp.eu/internal#rev": 2,
                  "https://schema.hbp.eu/internal#embedded": true,
                  "@id": "https://schema.hbp.eu/neuralactivity/experiment/patchedcell/v0.1.0#links#49ce2d7b-0527-46c3-83ac-d443918394b7--1",
                  "https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/outgoing": {
                    "@id": "https://nexus-dev.humanbrainproject.org/v0/data/neuralactivity/experiment/patchedcell/v0.1.0/49ce2d7b-0527-46c3-83ac-d443918394b7/outgoing"
                  },
                  "https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/schema": {
                    "@id": "https://nexus-dev.humanbrainproject.org/v0/schemas/neuralactivity/experiment/patchedcell/v0.1.0"
                  },
                  "_permissionGroup": "neuralactivity",
                  "https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/incoming": {
                    "@id": "https://nexus-dev.humanbrainproject.org/v0/data/neuralactivity/experiment/patchedcell/v0.1.0/49ce2d7b-0527-46c3-83ac-d443918394b7/incoming"
                  }
                }\
                """;
        Object o = JsonUtils.fromString(json);
        JsonLdStandardization standardization = new JsonLdStandardization();
        Object result = standardization.filterKeysByVocabBlacklists(o);
        String resultString = JsonUtils.toString(result);
        System.out.println(resultString);


    }
}