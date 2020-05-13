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

import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Assert;
import org.junit.Test;

public class StoredTemplateReferenceTest {

    @Test
    public void getName() {
        StoredTemplateReference templateReference = new StoredTemplateReference(new StoredQueryReference(TestObjectFactory.fooInstanceReference().getNexusSchema(), "fooQuery"), "fooTemplate");
        Assert.assertEquals("foo-bar-foobar-v0_0_1-fooQuery/fooTemplate", templateReference.getName());
    }
}