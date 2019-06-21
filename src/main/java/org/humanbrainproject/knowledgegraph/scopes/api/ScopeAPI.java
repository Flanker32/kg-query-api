package org.humanbrainproject.knowledgegraph.scopes.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.StoredQueryNotFoundException;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.scopes.boundary.Scope;
import org.humanbrainproject.knowledgegraph.scopes.entity.InvitedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.Set;

import static org.humanbrainproject.knowledgegraph.commons.api.ParameterConstants.*;

@RestController
@RequestMapping(value = "/api/scopes", produces = MediaType.APPLICATION_JSON)
@Api(value = "/api/scopes")
@ToBeTested(easy = true)
public class ScopeAPI {

    @Autowired
    Scope scope;

    @Autowired
    AuthorizationContext authorizationContext;

    @GetMapping(value = "/{"+ QUERY_ID+"}", consumes = {MediaType.WILDCARD})
    public ResponseEntity<Set<String>> getIdWhitelistForUser(@PathVariable(QUERY_ID) String queryId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        try {
            authorizationContext.setMasterCredential();
            Set<String> scope = this.scope.getIdWhitelistForUser(queryId, new OidcAccessToken().setToken(authorization));
            return ResponseEntity.ok(scope);
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + ID + "}")
    public ResponseEntity<Set<InvitedUser>> getInvitationsForInstance(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authorizationContext.setCredential(authorization);
        return ResponseEntity.ok(this.scope.getInvitedUsersForId(new NexusInstanceReference(new NexusSchemaReference(org, domain, schema, version), id)));
    }

    @PutMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + ID + "}/{userId}")
    public ResponseEntity<Void> createInvitation(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @PathVariable("userId") String userId,  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        try {
            authorizationContext.setCredential(authorization);
            this.scope.addScopeToUser(new NexusInstanceReference(new NexusSchemaReference(org, domain, schema, version), id), userId);
            return ResponseEntity.ok(null);
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @DeleteMapping(value = "/{" + ORG + "}/{" + DOMAIN + "}/{" + SCHEMA + "}/{" + VERSION + "}/{" + ID + "}/{userId}")
    public ResponseEntity<Void> removeInvitation(@PathVariable(ORG) String org, @PathVariable(DOMAIN) String domain, @PathVariable(SCHEMA) String schema, @PathVariable(VERSION) String version, @PathVariable(ID) String id, @PathVariable("userId") String userId,  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        try {
            authorizationContext.setCredential(authorization);
            this.scope.removeScopeFromUser(new NexusInstanceReference(new NexusSchemaReference(org, domain, schema, version), id), userId);
            return ResponseEntity.ok(null);
        } catch (StoredQueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

}