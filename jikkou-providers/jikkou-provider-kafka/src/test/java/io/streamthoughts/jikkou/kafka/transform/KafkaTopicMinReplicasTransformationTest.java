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
package io.streamthoughts.jikkou.kafka.transform;

import static io.streamthoughts.jikkou.kafka.transform.KafkaTopicMinReplicasTransformation.MIN_REPLICATION_FACTOR_CONFIG;

import io.streamthoughts.jikkou.core.ReconciliationContext;
import io.streamthoughts.jikkou.core.extension.ExtensionContext;
import io.streamthoughts.jikkou.core.models.DefaultResourceListObject;
import io.streamthoughts.jikkou.kafka.models.V1KafkaTopic;
import io.streamthoughts.jikkou.kafka.models.V1KafkaTopicSpec;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class KafkaTopicMinReplicasTransformationTest {

    KafkaTopicMinReplicasTransformation transformation;

    @BeforeEach
    void beforeEach() {
        ExtensionContext context = Mockito.mock(ExtensionContext.class);
        Mockito.when(context.appConfiguration()).thenReturn(MIN_REPLICATION_FACTOR_CONFIG.asConfiguration(3));
        transformation = new KafkaTopicMinReplicasTransformation();
        transformation.init(context);
    }

    @Test
    void shouldNotEnforceConstraintForValidValue() {
        // Given
        V1KafkaTopic resource = V1KafkaTopic.builder()
                .withSpec(V1KafkaTopicSpec
                        .builder()
                        .withReplicas((short) 6)
                        .build())
                .build();
        // When
        Optional<V1KafkaTopic> result = transformation
                .transform(resource, DefaultResourceListObject.of(Collections.emptyList()), ReconciliationContext.Default.EMPTY);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());

        V1KafkaTopic transformed = result.get();
        Assertions.assertEquals((short) 6, transformed.getSpec().getReplicas());
    }

    @Test
    void shouldEnforceConstraintForInvalidValue() {
        // Given
        V1KafkaTopic resource = V1KafkaTopic.builder()
                .withSpec(V1KafkaTopicSpec
                        .builder()
                        .withReplicas((short) 1)
                        .build())
                .build();
        // When
        Optional<V1KafkaTopic> result = transformation
                .transform(resource, DefaultResourceListObject.of(Collections.emptyList()), ReconciliationContext.Default.EMPTY);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());

        V1KafkaTopic transformed = result.get();
        Assertions.assertEquals((short) 3, transformed.getSpec().getReplicas());
    }

}