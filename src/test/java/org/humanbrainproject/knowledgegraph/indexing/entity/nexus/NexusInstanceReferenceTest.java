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

package org.humanbrainproject.knowledgegraph.indexing.entity.nexus;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NexusInstanceReferenceTest {

    NexusInstanceReference instanceFromMainSpace;

    @BeforeEach
    public void setup(){
        instanceFromMainSpace = new NexusInstanceReference("foo", "bar", "foobar", "v0.0.1", "barfoo");
    }

    @Test
    public void createNexusInstanceReferenceWithRevisionFromUrl(){
        NexusInstanceReference result = NexusInstanceReference.createFromUrl("foo/bar/foobar/v0.0.1/barfoo?rev=12");
        Assertions.assertEquals("foo", result.getNexusSchema().getOrganization());
        Assertions.assertEquals("bar", result.getNexusSchema().getDomain());
        Assertions.assertEquals("foobar", result.getNexusSchema().getSchema());
        Assertions.assertEquals("v0.0.1", result.getNexusSchema().getSchemaVersion());
        Assertions.assertEquals("barfoo", result.getId());
        Assertions.assertEquals(Integer.valueOf(12), result.getRevision());
    }


    @Test
    public void createMainInstanceReference() {
        Assertions.assertEquals("foo", instanceFromMainSpace.getNexusSchema().getOrganization());
        Assertions.assertEquals("bar", instanceFromMainSpace.getNexusSchema().getDomain());
        Assertions.assertEquals("foobar", instanceFromMainSpace.getNexusSchema().getSchema());
        Assertions.assertEquals("v0.0.1", instanceFromMainSpace.getNexusSchema().getSchemaVersion());
        Assertions.assertEquals("barfoo", instanceFromMainSpace.getId());
        Assertions.assertEquals("foo/bar/foobar/v0.0.1/barfoo", instanceFromMainSpace.getRelativeUrl().getUrl());
        Assertions.assertEquals("foo/bar/foobar/v0.0.1", instanceFromMainSpace.getNexusSchema().getRelativeUrl().getUrl());
        Assertions.assertEquals(SubSpace.MAIN, instanceFromMainSpace.getNexusSchema().getSubSpace());
    }


    @Test
    public void createEditorInstanceReferenceByToSubSpace() {
        NexusInstanceReference editorInstance = instanceFromMainSpace.toSubSpace(SubSpace.EDITOR);
        NexusInstanceReference newEditorInstance = new NexusInstanceReference("fooeditor", "bar", "foobar", "v0.0.1", "barfoo");

        Assertions.assertEquals("foo", editorInstance.getNexusSchema().getOrganization());
        Assertions.assertEquals("bar", editorInstance.getNexusSchema().getDomain());
        Assertions.assertEquals("foobar", editorInstance.getNexusSchema().getSchema());
        Assertions.assertEquals("v0.0.1", editorInstance.getNexusSchema().getSchemaVersion());
        Assertions.assertEquals("barfoo", editorInstance.getId());
        Assertions.assertEquals("foo"+SubSpace.EDITOR.getPostFix()+"/bar/foobar/v0.0.1/barfoo", editorInstance.getRelativeUrl().getUrl());
        Assertions.assertEquals("foo"+SubSpace.EDITOR.getPostFix()+"/bar/foobar/v0.0.1", editorInstance.getNexusSchema().getRelativeUrl().getUrl());
        Assertions.assertEquals(editorInstance, newEditorInstance);
    }

    @Test
    public void getFullId(){
        String fullId = instanceFromMainSpace.getFullId(false);
        Assertions.assertEquals("foo/bar/foobar/v0.0.1/barfoo", fullId);
    }

    @Test
    public void getFullIdWithImplicitRevision(){
        String fullId = instanceFromMainSpace.getFullId(true);
        Assertions.assertEquals("foo/bar/foobar/v0.0.1/barfoo?rev=1", fullId);
    }

    @Test
    public void getFullIdWithExplicitRevision(){
        instanceFromMainSpace.setRevision(20);
        String fullId = instanceFromMainSpace.getFullId(true);
        Assertions.assertEquals("foo/bar/foobar/v0.0.1/barfoo?rev=20", fullId);
    }

    @Test
    public void isSameInstanceRegardlessOfRevision(){
        NexusInstanceReference clone = instanceFromMainSpace.clone();
        clone.setRevision(30);
        Assertions.assertTrue(instanceFromMainSpace.isSameInstanceRegardlessOfRevision(clone));
    }

    @Test
    public void isSameInstanceRegardlessOfRevisionDifferentId(){
        Assertions.assertFalse(instanceFromMainSpace.isSameInstanceRegardlessOfRevision(new NexusInstanceReference(instanceFromMainSpace.getNexusSchema(), "bar")));
    }



    @Test
    public void cloneInstance(){
        instanceFromMainSpace.setRevision(20);
        NexusInstanceReference clone = instanceFromMainSpace.clone();
        Assertions.assertFalse(clone == instanceFromMainSpace);
        Assertions.assertFalse(clone.getNexusSchema()==instanceFromMainSpace.getNexusSchema());
        Assertions.assertEquals(instanceFromMainSpace, clone);
        Assertions.assertEquals(instanceFromMainSpace.getNexusSchema(), clone.getNexusSchema());
        Assertions.assertEquals(instanceFromMainSpace.getRevision(), clone.getRevision());
    }

}