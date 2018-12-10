package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.apache.commons.collections4.map.LRUMap;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.AccessRight;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthorizationController {

    @Autowired
    NexusClient nexusClient;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    SystemOidcHeaderInterceptor systemOidcHeaderInterceptor;

    public ClientHttpRequestInterceptor getInterceptor(Credential credential){
        if(credential instanceof InternalMasterKey){
            return systemOidcHeaderInterceptor;
        }
        else if(credential instanceof OidcAccessToken){
            return new OidcHeaderInterceptor((OidcAccessToken)credential);
        }
        throw new RuntimeException("Unknown credential: "+credential);
    }


    private final LRUMap<Credential, Set<AccessRight>> tokenToAccessRights = new LRUMap<>();

    public Set<AccessRight> getAccessRights(Credential credential) {
        if (tokenToAccessRights.containsKey(credential)) {
            return tokenToAccessRights.get(credential);
        }
        Set<String> allOrganizations = nexusClient.getAllOrganizations(getInterceptor(credential));
        //TODO right now, we only have the differentiation if a organization is visible or not - we therefore only can tell that there is at least READ access. We should have other means to ensure WRITE access.
        Set<AccessRight> accessRights = allOrganizations.stream().map(org -> new AccessRight(org.replace(nexusConfiguration.getNexusBase(NexusConfiguration.ResourceType.ORGANIZATION)+"/", ""), AccessRight.Permission.READ)).collect(Collectors.toSet());
        tokenToAccessRights.put(credential, accessRights);
        return accessRights;
    }


    public Set<String> getReadableOrganizations(Credential credential){
        return getReadableOrganizations(credential, null);
    }

    public Set<String> getReadableOrganizations(Credential credential, List<String> whitelistedOrganizations){
        Set<AccessRight> accessRights = getAccessRights(credential);
        Set<String> readableOrganizations = accessRights.stream().map(AccessRight::getPath).collect(Collectors.toSet());
        if(whitelistedOrganizations!=null){
            readableOrganizations.retainAll(whitelistedOrganizations);
        }
        return readableOrganizations;
    }

    public boolean isReadable(Map data, Credential credential){
        if(data.containsKey(ArangoVocabulary.PERMISSION_GROUP) && data.get(ArangoVocabulary.PERMISSION_GROUP) instanceof String){
            return getReadableOrganizations(credential, null).contains(data.get(ArangoVocabulary.PERMISSION_GROUP));
        }
        return false;
    }


    public void flushAccessRights(){
        this.tokenToAccessRights.clear();
    }


}