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

package org.humanbrainproject.knowledgegraph;

import com.google.common.base.Predicates;
import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.ExternalApi;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@NoTests(NoTests.NO_LOGIC)
public class SwaggerConfiguration {

    @Bean
    public Docket externalApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("00_external")
                .select()
                .apis(RequestHandlerSelectors.withMethodAnnotation(ExternalApi.class))
                .paths(PathSelectors.regex("^(?!/error).*"))
                .build();
    }

    @Bean
    public Docket publicApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("0_public")
                .select()
                .apis(Predicates.not(RequestHandlerSelectors.withClassAnnotation(InternalApi.class)))
                .paths(PathSelectors.regex("^(?!/error).*"))
                .build();
    }


    @Bean
    public Docket internalApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("1_internal")
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(InternalApi.class))
                .paths(PathSelectors.regex("^(?!/error).*"))
                        .build();
    }


}
