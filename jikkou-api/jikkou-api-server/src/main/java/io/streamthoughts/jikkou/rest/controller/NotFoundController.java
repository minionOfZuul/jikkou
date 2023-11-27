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
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.hateoas.Link;
import io.streamthoughts.jikkou.rest.data.ErrorEntity;
import io.streamthoughts.jikkou.rest.data.ErrorResponse;
import io.streamthoughts.jikkou.rest.data.errors.Errors;
import io.streamthoughts.jikkou.rest.entities.ResourceResponse;
import java.util.List;

@Controller
public class NotFoundController extends AbstractController {

    @Produces
    @Error(status = HttpStatus.NOT_FOUND, global = true)
    public HttpResponse<?> notFound(HttpRequest request) {
        ErrorResponse error = new ErrorResponse(
                "Not Found",
                List.of(new ErrorEntity(
                                HttpStatus.NOT_FOUND.getCode(),
                                Errors.NOT_FOUND
                        )
                )
        );
        ResourceResponse<ErrorResponse> response = new ResourceResponse<>(error)
                .link(Link.SELF, getSelfLink(request));
        return HttpResponse.notFound(response);
    }
}