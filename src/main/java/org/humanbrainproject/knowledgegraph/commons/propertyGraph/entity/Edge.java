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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

@ToBeTested(easy = true)
public class Edge implements VertexOrEdge{

    private final JsonPath path;
    private NexusInstanceReference reference;
    private Vertex vertex;

    public Edge(Vertex vertex, JsonPath path, NexusInstanceReference reference) {
        this.path = path;
        this.reference = reference;
        this.vertex = vertex;
    }

    public JsonPath getPath() {
        return path;
    }

    public NexusInstanceReference getReference() {
        return reference;
    }

    public void setReference(NexusInstanceReference reference) {
        this.reference = reference;
    }

    public String getId(){
        StringBuilder sb = new StringBuilder();
        sb.append(vertex.getInstanceReference().getFullId(false));
        for (Step step : path) {
            sb.append('-').append(step.getName()).append('-').append(step.getOrderNumber());
        }
        return sb.toString();
    }

    //FIXME: This is not the proper way of handling the order number since it doesn't support multi-level nesting
    public Integer getLastOrderNumber(){
        if(path.isEmpty()){
            return null;
        }
        return path.getLast().getOrderNumber();
    }

    public String getName(){
        if(path.isEmpty()){
            return null;
        }
        return path.getLast().getName();
    }

}
