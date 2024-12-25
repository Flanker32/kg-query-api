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

package org.humanbrainproject.knowledgegraph.indexing.api;

import com.github.jsonldjava.core.JsonLdError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.humanbrainproject.knowledgegraph.commons.api.RestUtils;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.ws.rs.core.MediaType;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/internal/indexing")
@InternalApi
@Tag(name="/internal/indexing", description = "The indexing api - triggers the indexing of the incoming messages. ATTENTION: These operations are executed with full DB rights! Be sure, you protect these API endpoints accordingly!")
@ToBeTested(easy = true)
public class IndexingInternalAPI {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    GraphIndexing indexer;

    private Logger logger = LoggerFactory.getLogger(IndexingInternalAPI.class);

    private String getTimestamp(String timestamp){
        return timestamp!=null ? timestamp : ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }


    @Operation(summary = "Index the creation of a new instance")
    @PostMapping(value="/{"+ ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ID+"}", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> addInstance(@RequestBody String payload, @PathVariable(ORG) String organization, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String schemaVersion, @PathVariable(ID) String id, @RequestParam(required = false) String authorId, @RequestParam(value = "eventDateTime", required = false) String timestamp) {
        authorizationContext.setMasterCredential();

        NexusInstanceReference path = new NexusInstanceReference(organization, domain, schema, schemaVersion, id).setRevision(1);
        logger.info("Received insert request for {}", path.getRelativeUrl().getUrl());
        logger.debug("Payload for insert request {}: {}", path.getRelativeUrl().getUrl(), payload);
        try {
            IndexingMessage message = new IndexingMessage(path, payload, getTimestamp(timestamp), authorId);
            indexer.insert(message);
            return ResponseEntity.ok(null);
        } catch (JsonLdError e) {
            logger.warn("INS: Was not able to process the payload %s".formatted(payload), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch(Exception e){
            logger.error("INS: Was not able to insert the instance %s with the payload %s".formatted(path.getRelativeUrl().getUrl(), payload), e);
            throw new RuntimeException(e);
        }
    }

    @Operation(summary = "Index the update of an existing instance in a specific revision")
    @PutMapping(value="/{"+ ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ID+"}/{"+REV+"}", consumes = {MediaType.APPLICATION_JSON, RestUtils.APPLICATION_LD_JSON}, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> updateInstance(@RequestBody String payload, @PathVariable(ORG) String organization, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String schemaVersion, @PathVariable(ID) String id, @PathVariable(REV) Integer rev, @RequestParam(required = false) String authorId, @RequestParam(value = "eventDateTime", required = false) String timestamp) {
        authorizationContext.setMasterCredential();

        NexusInstanceReference path = new NexusInstanceReference(organization, domain, schema, schemaVersion, id).setRevision(rev);
        logger.info("Received update request for {} in rev {}", path.getRelativeUrl().getUrl(), rev);
        logger.debug("Payload for update request {} in rev {}: {}", path.getRelativeUrl().getUrl(), rev, payload);
        try {
            IndexingMessage message = new IndexingMessage(path, payload, getTimestamp(timestamp), authorId);
            indexer.update(message);
            return ResponseEntity.ok(null);
        } catch (JsonLdError  e) {
            logger.warn("UPD: Was not able to process the payload %s".formatted(payload), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch(Exception e){
            logger.error("UPD: Was not able to update the instance %s with the payload %s".formatted(path.getRelativeUrl().getUrl(), payload), e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Operation(summary = "Index the deletion of an existing instance")
    @DeleteMapping(value="/{"+ ORG+"}/{"+DOMAIN+"}/{"+SCHEMA+"}/{"+VERSION+"}/{"+ID+"}", produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> deleteInstance(@PathVariable(ORG) String organization, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String schemaVersion, @PathVariable(ID) String id, @RequestAttribute(value=REV, required = false) Integer rev, @RequestParam(required = false) String authorId, @RequestParam(value = "eventDateTime", required = false) String timestamp) {
        authorizationContext.setMasterCredential();

        NexusInstanceReference path = new NexusInstanceReference(organization, domain, schema, schemaVersion, id).setRevision(rev);
        logger.info("Received delete request for {} in rev {}", path.getRelativeUrl().getUrl(), rev);
        try {
            indexer.delete(path);
            return ResponseEntity.ok("Successfully deleted the instance %s".formatted(path.getRelativeUrl().getUrl()));
        }
        catch(Exception e){
            logger.error("DEL: Was not able to delete the instance %s".formatted(path.getRelativeUrl().getUrl()), e);
            throw new RuntimeException(e);
        }
    }

    @Operation(summary = "Remove everything in the index")
    @DeleteMapping("/collections")
    public void clearGraph(){
        authorizationContext.setMasterCredential();
        indexer.clearGraph();
    }

}
