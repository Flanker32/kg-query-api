package org.humanbrainproject.knowledgegraph.instances.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Instances;
import org.humanbrainproject.knowledgegraph.instances.entity.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/instances", produces = MediaType.APPLICATION_JSON)
@Api(value = "/instances", description = "The API for managing instances")
public class InstancesAPI {

    @Autowired
    Instances instances;

    @GetMapping(value = "/{org}/{domain}/{schema}/{version}/{id}", consumes = {MediaType.WILDCARD})
    public ResponseEntity<Map> getInstance(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id) throws Exception {
        try {
            Map instance = instances.getInstance(new NexusInstanceReference(org, domain, schema, version, id));
            return instance != null ? ResponseEntity.ok(instance) : ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping(value = "/{org}/{domain}/{schema}/{version}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<Map> createNewInstanceForSchema(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        NexusInstanceReference reference = instances.createNewInstance(new NexusSchemaReference(org, domain, schema, version), new OidcAccessToken().setToken(authorizationToken));
        if (reference != null) {
            Map<String, String> result = new HashMap<>();
            result.put("relativeUrl", reference.getRelativeUrl().getUrl());
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(value = "/{org}/{domain}/{schema}/{version}/{id}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<Map> updateInstance(@RequestBody String payload, @PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @ApiParam("The clientIdExtension allows the calling client to specify an additional postfix to the identifier and therefore to discriminate between different instances which are combined in the inferred space. If this value takes a userId for example, this means that there will be a distinct instance created for every user.") @RequestParam(value = "clientIdExtension", required = false) String clientIdExtension, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken, @RequestHeader(value = "client", required = false) Client client) {
        NexusInstanceReference instanceReference = new NexusInstanceReference(org, domain, schema, version, id);
        NexusInstanceReference newReference = instances.updateInstance(instanceReference, payload, client, clientIdExtension, new OidcAccessToken().setToken(authorizationToken));
        if (newReference != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping(value = "/{org}/{domain}/{schema}/{version}/{id}", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"})
    public ResponseEntity<Void> deleteInstance(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationToken) {
        if (instances.removeInstance(new NexusInstanceReference(org, domain, schema, version, id), new OidcAccessToken().setToken(authorizationToken))){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();


    }


}

