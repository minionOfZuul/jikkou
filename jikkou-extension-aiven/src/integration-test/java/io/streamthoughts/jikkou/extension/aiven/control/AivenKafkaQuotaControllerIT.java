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
package io.streamthoughts.jikkou.extension.aiven.control;

import io.streamthoughts.jikkou.JikkouMetadataAnnotations;
import io.streamthoughts.jikkou.api.ReconciliationContext;
import io.streamthoughts.jikkou.api.ReconciliationMode;
import io.streamthoughts.jikkou.api.change.ChangeResult;
import io.streamthoughts.jikkou.api.change.ValueChange;
import io.streamthoughts.jikkou.api.model.ObjectMeta;
import io.streamthoughts.jikkou.extension.aiven.AbstractAivenIntegrationTest;
import io.streamthoughts.jikkou.extension.aiven.adapter.KafkaQuotaAdapter;
import io.streamthoughts.jikkou.extension.aiven.api.data.KafkaQuotaEntry;
import io.streamthoughts.jikkou.extension.aiven.models.V1KafkaQuota;
import io.streamthoughts.jikkou.extension.aiven.models.V1KafkaQuotaSpec;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class AivenKafkaQuotaControllerIT extends AbstractAivenIntegrationTest {

    private static AivenKafkaQuotaController controller;

    @BeforeEach
    public void beforeEach() {
        controller = new AivenKafkaQuotaController(getAivenApiConfig());
    }

    @Test
    void shouldCreateKafkaQuota() {
        // Given
        enqueueResponse(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200)
                .setBody("""
                        {"quotas":[{"client-id":"default","consumer_byte_rate":1048576.0,"producer_byte_rate":1048576.0,"request_percentage":25.0,"user":"default"}]}
                        """
                ));
        enqueueResponse(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200)
                .setBody("""
                        {"message":"upsert"}
                        """
                ));

        V1KafkaQuota entry = V1KafkaQuota.builder()
                .withSpec(V1KafkaQuotaSpec
                        .builder()
                        .withUser("test")
                        .withClientId("test")
                        .withProducerByteRate(1048576.0)
                        .withConsumerByteRate(1048576.0)
                        .withRequestPercentage(25.0)
                        .build()
                )
                .build();

        // When
        List<ChangeResult<ValueChange<KafkaQuotaEntry>>> results = controller
                .reconcile(List.of(entry), ReconciliationMode.CREATE, ReconciliationContext.builder().dryRun(false).build());

        // Then
        ValueChange<KafkaQuotaEntry> expected = ValueChange.withAfterValue(KafkaQuotaAdapter.map(entry));
        Assertions.assertEquals(expected, results.get(0).data().getChange());
    }


    @Test
    void shouldDeleteKafkaQuota() {
        // Given
        enqueueResponse(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200)
                .setBody("""
                        {"quotas":[{"client-id":"default","consumer_byte_rate":1048576.0,"producer_byte_rate":1048576.0,"request_percentage":25.0,"user":"default"}]}
                        """
                ));
        enqueueResponse(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200)
                .setBody("""
                        {"message":"Deleted quota for for (User: default, Client-id: default)"}
                        """

                ));
        // When
        V1KafkaQuota entry = V1KafkaQuota.builder()
                .withMetadata(ObjectMeta.builder()
                        .withAnnotation(JikkouMetadataAnnotations.JIKKOU_IO_DELETE, true)
                        .build())
                .withSpec(V1KafkaQuotaSpec
                        .builder()
                        .withUser("default")
                        .withClientId("default")
                        .withProducerByteRate(1048576.0)
                        .withConsumerByteRate(1048576.0)
                        .withRequestPercentage(25.0)
                        .build()
                )
                .build();

        // When
        List<ChangeResult<ValueChange<KafkaQuotaEntry>>> results = controller
                .reconcile(List.of(entry), ReconciliationMode.DELETE, ReconciliationContext.builder().dryRun(false).build());

        // Then
        ValueChange<KafkaQuotaEntry> expected = ValueChange.withBeforeValue(KafkaQuotaAdapter.map(entry));
        Assertions.assertEquals(expected, results.get(0).data().getChange());
    }
}