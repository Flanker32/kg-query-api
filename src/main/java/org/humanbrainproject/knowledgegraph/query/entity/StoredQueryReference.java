/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoNamingHelper;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

@NoTests(NoTests.TRIVIAL)
public class StoredQueryReference {

    public static final NexusSchemaReference GLOBAL_QUERY_SCHEMA = new NexusSchemaReference("hbpkg", "global", "queries", "v1.0.0");

    private final String name;
    private final NexusSchemaReference schemaReference;
    private final String alias;

    public StoredQueryReference(NexusSchemaReference schemaReference, String name){
        this.schemaReference = schemaReference;
        this.name = schemaReference!=null && schemaReference.getRelativeUrl()!=null ? ArangoNamingHelper.createCompatibleId(schemaReference.getRelativeUrl().getUrl())+"-"+ArangoNamingHelper.createCompatibleId(name) : ArangoNamingHelper.createCompatibleId(name);
        this.alias = name;
    }


    public NexusSchemaReference getSchemaReference() {
        return schemaReference;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }
}
