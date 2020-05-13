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

package org.humanbrainproject.knowledgegraph.indexing.control.spatial.transformation;

import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;
import org.junit.Assert;
import org.junit.Test;

public class QuickNiiTest {


    @Test
    public void test(){
        QuickNii quickNii = new QuickNii("[\n" +
                "           116.76450275662296,\n" +
                "           420.38180695602125,\n" +
                "           371.05990195986874,\n" +
                "           32.25523977201931,\n" +
                "           -483.5743708352436,\n" +
                "           -69.49201572740994,\n" +
                "           88.9241921312597,\n" +
                "           51.94987159616494,\n" +
                "           -320.22912581890387\n" +
                "        ]");
        Assert.assertEquals(116.76450275662296, quickNii.matrix.getEntry(0,0), 0.001);

        ThreeDVector point = quickNii.getPoint(1,1);
        System.out.println(point);
    }
}