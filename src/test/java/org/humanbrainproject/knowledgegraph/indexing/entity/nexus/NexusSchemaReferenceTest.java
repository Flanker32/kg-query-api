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

public class NexusSchemaReferenceTest {

    NexusSchemaReference schemaReference;

    @BeforeEach
    public void setup(){
        this.schemaReference = NexusSchemaReference.createFromUrl("https://foo/v0/foo/core/bar/v1.0.0");
    }


    @Test
    public void createFromUrl() {
        Assertions.assertEquals("foo", schemaReference.getOrganization());
        Assertions.assertEquals("core", schemaReference.getDomain());
        Assertions.assertEquals("bar", schemaReference.getSchema());
        Assertions.assertEquals("v1.0.0", schemaReference.getSchemaVersion());
    }

    @Test
    public void createFromRelativeUrl(){
        NexusSchemaReference fromUrl = NexusSchemaReference.createFromUrl("foo/core/bar/v1.0.0");
        Assertions.assertEquals("foo", fromUrl.getOrganization());
        Assertions.assertEquals("core", fromUrl.getDomain());
        Assertions.assertEquals("bar", fromUrl.getSchema());
        Assertions.assertEquals("v1.0.0", fromUrl.getSchemaVersion());

    }


    @Test
    public void createUniqueNamespace() {
        String uniqueNamespace = schemaReference.createUniqueNamespace();
        Assertions.assertEquals("https://schema.hbp.eu/foo/core/bar/v1.0.0/", uniqueNamespace);
    }

    @Test
    public void cloneInstance() {
        NexusSchemaReference clone = schemaReference.clone();

        Assertions.assertFalse(clone == schemaReference);
        Assertions.assertEquals(schemaReference, clone);
    }

    @Test
    public void getRelativeUrlForOrganization(){
        NexusRelativeUrl relativeUrlForOrganization = schemaReference.getRelativeUrlForOrganization();
        Assertions.assertEquals("foo", relativeUrlForOrganization.getUrl());
    }

    @Test
    public void getRelativeUrlForDomain(){
        NexusRelativeUrl relativeUrl = schemaReference.getRelativeUrlForDomain();
        Assertions.assertEquals("foo/core", relativeUrl.getUrl());
    }

    @Test
    public void getRelativeUrl(){
        NexusRelativeUrl relativeUrl = schemaReference.getRelativeUrl();
        Assertions.assertEquals("foo/core/bar/v1.0.0", relativeUrl.getUrl());
    }

    @Test
    public void extractMainOrganization(){
        String mainOrg = NexusSchemaReference.extractMainOrganization("foo"+ SubSpace.EDITOR.getPostFix());
        Assertions.assertEquals("foo", mainOrg);
    }

    @Test
    public void extractMainOrganizationNoPostfix(){
        String mainOrg = NexusSchemaReference.extractMainOrganization("foo");
        Assertions.assertEquals("foo", mainOrg);
    }

    @Test
    public void isInSubspaceMain(){
        Assertions.assertTrue(schemaReference.isInSubSpace(SubSpace.MAIN));
    }
    @Test
    public void isInSubspaceMainFalse(){
        Assertions.assertFalse(schemaReference.isInSubSpace(SubSpace.EDITOR));
    }

    @Test
    public void isInSubspaceEditor(){
        NexusSchemaReference schemaReference = this.schemaReference.toSubSpace(SubSpace.EDITOR);
        Assertions.assertTrue(schemaReference.isInSubSpace(SubSpace.EDITOR));
    }
    @Test
    public void isInSubspaceEditorFalse(){
        NexusSchemaReference schemaReference = this.schemaReference.toSubSpace(SubSpace.EDITOR);
        Assertions.assertFalse(schemaReference.isInSubSpace(SubSpace.MAIN));
    }


}