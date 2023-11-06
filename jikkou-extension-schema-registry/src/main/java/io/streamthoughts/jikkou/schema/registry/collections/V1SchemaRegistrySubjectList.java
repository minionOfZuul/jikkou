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
package io.streamthoughts.jikkou.schema.registry.collections;

import io.streamthoughts.jikkou.core.annotation.ApiVersion;
import io.streamthoughts.jikkou.core.annotation.Kind;
import io.streamthoughts.jikkou.core.models.DefaultResourceListObject;
import io.streamthoughts.jikkou.core.models.ObjectMeta;
import io.streamthoughts.jikkou.schema.registry.models.V1SchemaRegistrySubject;
import java.beans.ConstructorProperties;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiVersion("kafka.jikkou.io/v1beta2")
@Kind("SchemaRegistrySubjectList")
public class V1SchemaRegistrySubjectList extends DefaultResourceListObject<V1SchemaRegistrySubject> {


    /**
     * Creates a new {@link V1SchemaRegistrySubjectList} instance.
     *
     * @param kind       The resource Kind.
     * @param apiVersion The resource API Version.
     * @param metadata   The resource metadata.
     * @param items      The items.
     */
    @ConstructorProperties({
            "apiVersion",
            "kind",
            "metadata",
            "items"
    })
    public V1SchemaRegistrySubjectList(@Nullable String kind,
                                       @Nullable String apiVersion,
                                       @Nullable ObjectMeta metadata,
                                       @NotNull List<? extends V1SchemaRegistrySubject> items) {
        super(kind, apiVersion, metadata, items);
    }

    /**
     * Creates a new {@link V1SchemaRegistrySubjectList} instance.
     *
     * @param items The items.
     */
    public V1SchemaRegistrySubjectList(List<? extends V1SchemaRegistrySubject> items) {
        super(items);
    }
}