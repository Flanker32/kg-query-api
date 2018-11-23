package org.humanbrainproject.knowledgegraph.indexing.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.SystemOidcClient;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseTransaction;
import org.humanbrainproject.knowledgegraph.indexing.control.IndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.basic.BasicIndexingController;
import org.humanbrainproject.knowledgegraph.indexing.control.inference.InferenceController;
import org.humanbrainproject.knowledgegraph.indexing.control.releasing.ReleasingController;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class GraphIndexing {

    @Autowired
    BasicIndexingController defaultIndexingController;

    @Autowired
    ReleasingController releasingController;

    @Autowired
    InferenceController inferenceController;

    @Autowired
    MessageProcessor messageProcessor;

    @Autowired
    DatabaseTransaction transaction;

    @Autowired
    SystemOidcClient oidcClient;

    InternalMasterKey internalMasterKey = new InternalMasterKey();


    private List<IndexingController> getIndexingControllers(){
        return Arrays.asList(defaultIndexingController, releasingController, inferenceController);
    }

    public TodoList insert(IndexingMessage message){
        //Pre-process
        QualifiedIndexingMessage qualifiedSpec = messageProcessor.qualify(message);

        //Gather execution plan
        TodoList todoList = new TodoList();
        for (IndexingController indexingController : getIndexingControllers()) {
            indexingController.insert(qualifiedSpec, todoList, internalMasterKey);
        }

        //Execute
        transaction.execute(todoList);
        return todoList;
    }

    public TodoList update(IndexingMessage message) {
        //Pre-process
        QualifiedIndexingMessage qualifiedSpec = messageProcessor.qualify(message);

        //Gather execution plan
        TodoList todoList = new TodoList();
        for (IndexingController indexingController : getIndexingControllers()) {
            indexingController.update(qualifiedSpec, todoList, internalMasterKey);
        }

        //Execute
        transaction.execute(todoList);
        return todoList;
    }


    public TodoList delete(NexusInstanceReference reference){
        //Gather execution plan
        TodoList todoList = new TodoList();
        for (IndexingController indexingController : getIndexingControllers()) {
            indexingController.delete(reference, todoList, internalMasterKey);
        }
        //Execute
        transaction.execute(todoList);
        return todoList;
    }

    public void clearGraph() {
        for (IndexingController indexingController : getIndexingControllers()) {
            indexingController.clear(internalMasterKey);
        }
    }

}
