/*
 * Copyright 2021 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.jikkou.kafka.validations;

import io.streamthoughts.jikkou.api.config.ConfigProperty;
import io.streamthoughts.jikkou.api.config.Configuration;
import io.streamthoughts.jikkou.api.error.ConfigException;
import io.streamthoughts.jikkou.api.error.ValidationException;
import io.streamthoughts.jikkou.kafka.internals.KafkaConstants;
import io.streamthoughts.jikkou.kafka.models.V1KafkaTopicObject;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class TopicMaxReplicationFactorValidation extends TopicValidation {

    public static final ConfigProperty<Integer> VALIDATION_TOPIC_MAX_REPLICATION_FACTOR_CONFIG = ConfigProperty
            .ofInt("topic-max-replication-factor");

    private Integer maxReplicationFactor;

    /**
     * Empty constructor used by {@link Configuration}.
     */
    public TopicMaxReplicationFactorValidation(){}

    /**
     * Creates a new {@link TopicMaxReplicationFactorValidation}
     * @param maxReplicationFactor the min replication factor.
     */
    public TopicMaxReplicationFactorValidation(final int maxReplicationFactor) {
        configure(VALIDATION_TOPIC_MAX_REPLICATION_FACTOR_CONFIG.asConfiguration(maxReplicationFactor));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(@NotNull final Configuration config) throws ConfigException {
        super.configure(config);
        maxReplicationFactor = VALIDATION_TOPIC_MAX_REPLICATION_FACTOR_CONFIG.getOptional(config)
                .orElseThrow(() -> {
                    throw new ConfigException(
                            String.format("The '%s' configuration property is required for %s",
                                    VALIDATION_TOPIC_MAX_REPLICATION_FACTOR_CONFIG.key(),
                                    TopicNameSuffixValidation.class.getSimpleName()
                            )
                    );
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateTopic(final @NotNull V1KafkaTopicObject topic) throws ValidationException {
        Optional.ofNullable(topic.getReplicationFactor()).ifPresent(p -> {
            if (!p.equals(KafkaConstants.NO_REPLICATION_FACTOR) && p > maxReplicationFactor) {
                throw new ValidationException(String.format(
                        "Replication factor for topic '%s' is greater than the maximum required: %d > %d",
                        topic.getName(),
                        p,
                        maxReplicationFactor
                ), this);
            }
        });
    }
}