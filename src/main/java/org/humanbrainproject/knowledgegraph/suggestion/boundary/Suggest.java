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

package org.humanbrainproject.knowledgegraph.suggestion.boundary;

import org.humanbrainproject.knowledgegraph.commons.suggestion.SuggestionStatus;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.suggestion.control.SuggestionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;

@Component
public class Suggest {

    @Autowired
    SuggestionController suggestionController;

    public QueryResult<List<Map>> suggestByField(NexusSchemaReference schemaReference, String field, String type, String search, Pagination pagination){
        return suggestionController.simpleSuggestByField(schemaReference, field, type, search, pagination);
    }

    public NexusInstanceReference createSuggestionInstanceForUser(NexusInstanceReference ref, String userId, String clientIdExtension) throws NotFoundException{
        return suggestionController.createSuggestionInstanceForUser(ref, userId, clientIdExtension);
    }

    public Map getUserSuggestionOfSpecificInstance(NexusInstanceReference ref, String userId) throws NotFoundException {
        return suggestionController.getUserSuggestionOfSpecificInstance(ref, userId);
    }

    public List<Map> getUserSuggestions(String userId, SuggestionStatus status) throws NotFoundException{
        return suggestionController.getUserSuggestions(userId, status);
    }

    public JsonDocument changeSuggestionStatus(NexusInstanceReference ref, SuggestionStatus status, String clientIdExtension) throws NotFoundException{
        return suggestionController.changeSuggestionStatus(ref, status, clientIdExtension);
    }


}
