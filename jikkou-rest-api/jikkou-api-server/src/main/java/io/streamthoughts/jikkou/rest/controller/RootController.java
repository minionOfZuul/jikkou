/*
 * Copyright 2023 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.jikkou.rest.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.hateoas.Link;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.streamthoughts.jikkou.rest.Project;
import io.streamthoughts.jikkou.rest.data.Info;
import io.streamthoughts.jikkou.rest.entities.ResourceResponse;

@Controller("/")
@Secured(SecurityRule.IS_ANONYMOUS)
public class RootController extends AbstractController {

    @Get(produces = MediaType.APPLICATION_JSON)
    public ResourceResponse<Info> get(HttpRequest<?> httpRequest) {
        return new ResourceResponse<>(Project.info())
                .link(Link.SELF, getSelfLink(httpRequest))
                .link("get-apis", getLink(httpRequest, "apis"));
    }
}