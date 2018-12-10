package org.humanbrainproject.knowledgegraph.indexing.control.basic;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)

@Ignore("IntegrationTest")
public class BasicIndexingControllerIntegrationTest {

    @Autowired
    BasicIndexingController controller;


    @Before
    public void setup() {
    }


    @Test
    public void insert() {
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        NexusInstanceReference instanceReference = TestObjectFactory.fooInstanceReference();
        QualifiedIndexingMessage qualifiedIndexingMessage = TestObjectFactory.createQualifiedIndexingMessage(instanceReference, fullyQualified);
        TodoList todoList = controller.insert(qualifiedIndexingMessage, new TodoList(), new OidcAccessToken());
        System.out.println(todoList);

    }

    @Test
    public void update() {
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put("http://test/foo", "foo");
        NexusInstanceReference instanceReference = new NexusInstanceReference("minds", "core", "dataset", "v0.0.4", "0032bda4-50e3-4dc9-ab87-980de4f526a2");
        QualifiedIndexingMessage qualifiedIndexingMessage = TestObjectFactory.createQualifiedIndexingMessage(instanceReference, fullyQualified);
        TodoList todoList = controller.update(qualifiedIndexingMessage, new TodoList(), new OidcAccessToken());
        System.out.println(todoList);
    }

    @Test
    public void delete() {

    }
}