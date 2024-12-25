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

package org.humanbrainproject.knowledgegraph.indexing.control.inference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.Alternative;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReconciliationTest {

    @Test
    public void testDateParsing() throws ParseException {
        LocalDateTime parse = LocalDateTime.parse("2018-11-08T07:15:11.289Z", DateTimeFormatter.ISO_ZONED_DATE_TIME);
        System.out.println(parse);
    }

    @Test
    public void testMergeVertices() {
        Reconciliation rec = new Reconciliation();
        JsonDocument doc = new JsonDocument();
        Set<Vertex> vertices = new HashSet<>();
        NexusInstanceReference iref = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "123");
        NexusInstanceReference iref2 = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "456");
        NexusInstanceReference iref3 = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "789");


        Map<String, Object> m1 = new HashMap<>();
        m1.put("name", "test 1");
        m1.put("desc", "desc 1");
        m1.put("activity", "activity");
        m1.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-11-10T07:15:11.289Z");
        m1.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "123");

        QualifiedIndexingMessage message1 = TestObjectFactory.createQualifiedIndexingMessage(iref, m1);
        Vertex vertex1 = new Vertex(message1);

        Map<String, Object> m2 = new HashMap<>();
        m2.put("name", "test 2");
        m2.put("desc", "another desc");
        m2.put("activity", "activity");
        m2.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-11-08T07:15:11.289Z");
        m2.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "456");

        QualifiedIndexingMessage message2 = TestObjectFactory.createQualifiedIndexingMessage(iref2, m2);
        Vertex vertex2 = new Vertex(message2);

        Map<String, Object> m3 = new HashMap<>();
        m3.put("name", "test 1");
        m3.put("desc", "no desc");
        m3.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-12-08T07:15:11.289Z");
        m3.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "789");

        QualifiedIndexingMessage message3 = TestObjectFactory.createQualifiedIndexingMessage(iref3, m3);
        Vertex vertex3 = new Vertex(message3);

        vertices.add(vertex1);
        vertices.add(vertex2);
        vertices.add(vertex3);

        rec.mergeVertex(doc, vertices);
        Assertions.assertNotEquals(doc, null);
        Assertions.assertEquals( "test 1", doc.get("name"));
        Assertions.assertEquals( "no desc" , doc.get("desc"));
        Assertions.assertEquals("activity", doc.get("activity"));
        Map alternatives = (HashMap) doc.get(HBPVocabulary.INFERENCE_ALTERNATIVES);
        List<Alternative> altNames = (List<Alternative>) alternatives.get("name");
        List<Alternative> alts = (List<Alternative>) alternatives.get("desc");
        Assertions.assertNotEquals( null, alternatives.get("activity"));
        Assertions.assertEquals(3, alts.size());
        Assertions.assertEquals(2, altNames.size());

    }

    @Test
    public void testSelectCorrectValue() {
        Reconciliation rec = new Reconciliation();
        JsonDocument doc = new JsonDocument();
        Set<Vertex> vertices = new HashSet<>();
        NexusInstanceReference iref = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "123");
        NexusInstanceReference iref2 = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "456");


        Map<String, Object> m1 = new HashMap<>();
        m1.put("name", "test 1");
        m1.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-11-10T07:15:11.289Z");
        m1.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "123");

        QualifiedIndexingMessage message1 = TestObjectFactory.createQualifiedIndexingMessage(iref, m1);
        Vertex vertex1 = new Vertex(message1);

        Map<String, Object> m2 = new HashMap<>();
        m2.put("name", "test 2");

        // This update comes after in time
        m2.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-11-11T07:15:11.289Z");
        m2.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "456");

        QualifiedIndexingMessage message2 = TestObjectFactory.createQualifiedIndexingMessage(iref2, m2);
        Vertex vertex2 = new Vertex(message2);


        vertices.add(vertex1);
        vertices.add(vertex2);

        rec.mergeVertex(doc, vertices);
        Assertions.assertNotEquals(doc, null);
        Assertions.assertEquals( "test 2", doc.get("name"));
        Map alternatives = (HashMap) doc.get(HBPVocabulary.INFERENCE_ALTERNATIVES);
        List<Alternative> altNames = ((List<Alternative>) alternatives.get("name")).stream().filter(al -> !al.getIsSelected()).collect(Collectors.toList());
        Assertions.assertEquals(1, altNames.size());
        Assertions.assertEquals("test 1",  altNames.getFirst().getValue());
        Assertions.assertEquals(1,  altNames.getFirst().getUserIds().size());
        Assertions.assertEquals("123",  altNames.getFirst().getUserIds().toArray()[0]);

    }

    @Test
    public void testShouldContainAListOfUserIds() {
        Reconciliation rec = new Reconciliation();
        JsonDocument doc = new JsonDocument();
        Set<Vertex> vertices = new HashSet<>();
        NexusInstanceReference iref = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "123");
        NexusInstanceReference iref2 = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "456");
        NexusInstanceReference iref3 = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "789");
        NexusInstanceReference iref4 = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "000");
        NexusInstanceReference iref5 = new NexusInstanceReference(new NexusSchemaReference("org", "dom", "schema", "v1"), "111");


        Map<String, Object> m1 = new HashMap<>();
        m1.put("name", "test 1");
        m1.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-11-10T07:15:11.289Z");
        m1.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "123");

        QualifiedIndexingMessage message1 = TestObjectFactory.createQualifiedIndexingMessage(iref, m1);
        Vertex vertex1 = new Vertex(message1);

        Map<String, Object> m2 = new HashMap<>();
        m2.put("name", "test 2");
        m2.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-11-08T07:15:11.289Z");
        m2.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "456");

        QualifiedIndexingMessage message2 = TestObjectFactory.createQualifiedIndexingMessage(iref2, m2);
        Vertex vertex2 = new Vertex(message2);

        Map<String, Object> m3 = new HashMap<>();
        m3.put("name", "test 1");
        m3.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-12-09T07:15:11.289Z");
        m3.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "789");

        QualifiedIndexingMessage message3 = TestObjectFactory.createQualifiedIndexingMessage(iref3, m3);
        Vertex vertex3 = new Vertex(message3);

        Map<String, Object> m4 = new HashMap<>();
        m4.put("name", "test 2");
        m4.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-12-08T07:16:11.289Z");
        m4.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "000");

        QualifiedIndexingMessage message4 = TestObjectFactory.createQualifiedIndexingMessage(iref4, m4);
        Vertex vertex4 = new Vertex(message4);

        Map<String, Object> m5 = new HashMap<>();
        m5.put("name", "test 1");
        m5.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, "2018-12-08T08:15:11.289Z");
        m5.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, "111");

        QualifiedIndexingMessage message5 = TestObjectFactory.createQualifiedIndexingMessage(iref5, m5);
        Vertex vertex5 = new Vertex(message5);


        vertices.add(vertex1);
        vertices.add(vertex2);
        vertices.add(vertex3);
        vertices.add(vertex4);
        vertices.add(vertex5);

        rec.mergeVertex(doc, vertices);
        Assertions.assertNotEquals(doc, null);
        Assertions.assertEquals( "test 1", doc.get("name"));
        Map alternatives = (HashMap) doc.get(HBPVocabulary.INFERENCE_ALTERNATIVES);
        List<Alternative> altNames = ((List<Alternative>) alternatives.get("name")).stream().filter(al -> !al.getIsSelected()).collect(Collectors.toList());
        Assertions.assertEquals(1, altNames.size());
        Alternative a = altNames.getFirst();
        Assertions.assertEquals("test 2", a.getValue());
        Assertions.assertEquals(2, a.getUserIds().size());
        Assertions.assertEquals(true, a.getUserIds().contains("000"));
        Assertions.assertEquals(true, a.getUserIds().contains("456"));


    }


}