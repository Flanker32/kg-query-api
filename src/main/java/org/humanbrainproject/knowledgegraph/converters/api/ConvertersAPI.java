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

package org.humanbrainproject.knowledgegraph.converters.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.humanbrainproject.knowledgegraph.commons.api.RestUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.converters.boundary.Converter;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.ws.rs.core.MediaType;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/coverters")
@InternalApi
@Tag(name="/converters", description = "The converter mechanisms")
@ToBeTested(easy = true)
public class ConvertersAPI {

    @Autowired
    Converter converter;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    AuthorizationContext authorizationContext;

    private Logger logger = LoggerFactory.getLogger(ConvertersAPI.class);

    private String getTimestamp(String timestamp){
        return timestamp!=null ? timestamp : ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }

    @PostMapping(value="/shacl-to-editor/{"+ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<Map> shacl2editor(@RequestBody String payload, @PathVariable(ORG) String organization, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String schemaVersion, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        authorizationContext.populateAuthorizationContext(authorizationToken);
        JsonDocument jsonDocument = converter.convertShaclToEditor(new NexusSchemaReference(organization, domain, schema, schemaVersion));
        if(jsonDocument==null){
            return ResponseEntity.notFound().build();
        }
        else{
            return ResponseEntity.ok(jsonDocument);
        }
    }
}
