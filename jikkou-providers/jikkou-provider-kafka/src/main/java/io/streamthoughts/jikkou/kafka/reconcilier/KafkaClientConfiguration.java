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
package io.streamthoughts.jikkou.kafka.reconcilier;

import io.streamthoughts.jikkou.core.config.ConfigProperty;
import io.streamthoughts.jikkou.core.config.Configuration;
import io.streamthoughts.jikkou.kafka.internals.KafkaUtils;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class KafkaClientConfiguration {

    public static final String KAFKA_CLIENT_CONFIG_NAME = "kafka.client";

    public static final ConfigProperty<Map<String, Object>> PRODUCER_CLIENT_CONFIG = ConfigProperty
            .ofMap(KAFKA_CLIENT_CONFIG_NAME)
            .orElse(HashMap::new)
            .map(KafkaUtils::getProducerClientConfigs);

    public static final ConfigProperty<Map<String, Object>> CONSUMER_CLIENT_CONFIG = ConfigProperty
            .ofMap(KAFKA_CLIENT_CONFIG_NAME)
            .orElse(HashMap::new)
            .map(KafkaUtils::getConsumerClientConfigs);

    public static final ConfigProperty<Map<String, Object>> ADMIN_CLIENT_CONFIG = ConfigProperty
            .ofMap(KAFKA_CLIENT_CONFIG_NAME)
            .orElse(HashMap::new)
            .map(KafkaUtils::getAdminClientConfigs);

    public static final ConfigProperty<Duration> KAFKA_DEFAULT_TIMEOUT_CONFIG = ConfigProperty
            .ofLong(KAFKA_CLIENT_CONFIG_NAME + ".defaultTimeoutMs")
            .map(Duration::ofMillis)
            .orElse(Duration.ofSeconds(30));

    private final Configuration configuration;

    public KafkaClientConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Duration getClientDefaultTimeout() {
        return KAFKA_DEFAULT_TIMEOUT_CONFIG.get(configuration);
    }
}