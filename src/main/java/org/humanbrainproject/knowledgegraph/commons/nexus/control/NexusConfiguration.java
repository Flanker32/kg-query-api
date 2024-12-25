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

package org.humanbrainproject.knowledgegraph.commons.nexus.control;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Tested
public class NexusConfiguration {

    public enum ResourceType {
        DATA("data"), SCHEMA("schemas"), ORGANIZATION("organizations"), DOMAIN("domains"), CONTEXT("contexts");

        private final String urlDeclaration;

        ResourceType(String urlDeclaration) {
            this.urlDeclaration = urlDeclaration;
        }

    }

    @Value("${org.humanbrainproject.knowledgegraph.nexus.base}")
    String nexusBase;

    @Value("${org.humanbrainproject.knowledgegraph.nexus.endpoint}")
    String nexusEndpoint;

    @Value("${org.humanbrainproject.knowledgegraph.iam.endpoint}")
    String iamEndpoint;

    /*
     * @return the resolvable nexus-endpoint URL. This is the URL where the API endpoint can be accessed. Please note, that this can but not necessarily has to be the same as nexusBase since reverse proxies and network topologies can change this
     */
    public String getNexusEndpoint() {
        return nexusEndpoint;
    }

    public String getUserInfoEndpoint(){
        return "%s/v0/oauth2/userinfo".formatted(iamEndpoint);
    }


    /**
     * @return the nexus-base URL. This is the URL which is used e.g. to prefix the IDs of nexus instances.
     */
    public String getNexusBase() {
        return nexusBase;
    }

    public String getNexusBase(ResourceType resourceType){
        return "%s/v0/%s".formatted(getNexusBase(), resourceType.urlDeclaration);
    }

    public String getEndpoint(ResourceType resourceType){
        return "%s/v0/%s".formatted(getNexusEndpoint(), resourceType.urlDeclaration);
    }

    public String getEndpoint(NexusRelativeUrl relativeUrl) {
        return "%s%s".formatted(getEndpoint(relativeUrl.getResourceType()), relativeUrl.getUrl() != null ? "/%s".formatted(relativeUrl.getUrl()) : "");
    }

    public String getAbsoluteUrl(NexusSchemaReference schemaReference){
        return "%s%s".formatted(getNexusBase(schemaReference.getRelativeUrl().getResourceType()), schemaReference.getRelativeUrl().getUrl() != null ? "/%s".formatted(schemaReference.getRelativeUrl().getUrl()) : "");
    }

    public String getAbsoluteUrl(NexusInstanceReference instanceReference) {
        return "%s%s".formatted(getNexusBase(instanceReference.getRelativeUrl().getResourceType()), instanceReference.getRelativeUrl().getUrl() != null ? "/%s".formatted(instanceReference.getRelativeUrl().getUrl()) : "");
    }

}
