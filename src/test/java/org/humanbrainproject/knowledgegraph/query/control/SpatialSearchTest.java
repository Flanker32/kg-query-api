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

package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.solr.Solr;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SpatialSearchTest {

    SpatialSearch spatialSearch;

    @BeforeEach
    public void setup(){
        this.spatialSearch = new SpatialSearch();
        this.spatialSearch.solr = Mockito.mock(Solr.class);
    }


    @Test
    public void minimalBoundingBox() throws IOException, SolrServerException {
        Mockito.doReturn(Arrays.asList("foobar/foo", "foobar/bar")).when(this.spatialSearch.solr).queryIdsOfMinimalBoundingBox(Mockito.any());

        Set<ArangoDocumentReference> arangoDocumentReferences = this.spatialSearch.minimalBoundingBox(null);

        Assertions.assertEquals(2, arangoDocumentReferences.size());
        Set<String> references = arangoDocumentReferences.stream().map(ArangoDocumentReference::getId).collect(Collectors.toSet());
        Assertions.assertTrue(references.contains("foobar/foo"));
        Assertions.assertTrue(references.contains("foobar/bar"));
    }
}