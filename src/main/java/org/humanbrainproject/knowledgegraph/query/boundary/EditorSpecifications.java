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

package org.humanbrainproject.knowledgegraph.query.boundary;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.query.control.EditorSpecificationsController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
public class EditorSpecifications {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    EditorSpecificationsController editorSpecificationsController;

    public void saveSpecification(String specification, String specificationId) {
        editorSpecificationsController.saveSpecification(specification, specificationId, databaseFactory.getInternalDB());
    }

}