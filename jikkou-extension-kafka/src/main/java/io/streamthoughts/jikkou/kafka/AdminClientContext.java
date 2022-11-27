/*
 * Copyright 2022 StreamThoughts.
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
package io.streamthoughts.jikkou.kafka;

import io.streamthoughts.jikkou.api.config.ConfigProperty;
import io.streamthoughts.jikkou.api.config.Configuration;
import io.streamthoughts.jikkou.api.error.JikkouException;
import io.streamthoughts.jikkou.common.utils.PropertiesUtils;
import io.streamthoughts.jikkou.kafka.control.KafkaFunction;
import io.streamthoughts.jikkou.kafka.internals.KafkaBrokersReady;
import io.streamthoughts.jikkou.kafka.internals.KafkaUtils;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.apache.kafka.clients.admin.AdminClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for executing operation using an ephemeral {@link AdminClient} instance.
 */
public class AdminClientContext implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AdminClientContext.class);

    private static final int DEFAULT_MIN_AVAILABLE_BROKERS = 1;
    private static final long DEFAULT_TIMEOUT_MS = 60000L;
    private static final long DEFAULT_RETRY_BACKOFF_MS = 1000L;
    public static final String ADMIN_CLIENT_CONFIG_NAME = "client";

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public static final ConfigProperty<Properties> ADMIN_CLIENT_CONFIG = ConfigProperty
            .ofMap(ADMIN_CLIENT_CONFIG_NAME)
            .orElse(HashMap::new)
            .map(KafkaUtils::getAdminClientConfigs)
            .map(PropertiesUtils::fromMap);

    public static final ConfigProperty<Boolean> KAFKA_BROKERS_WAIT_FOR_ENABLED = ConfigProperty
            .ofBoolean("kafka.brokers.wait-for-enabled")
            .orElse(true);

    public static final ConfigProperty<Integer> KAFKA_BROKERS_WAIT_FOR_MIN_AVAILABLE = ConfigProperty
            .ofInt("kafka.brokers.wait-for-min-available")
            .orElse(DEFAULT_MIN_AVAILABLE_BROKERS);

    public static final ConfigProperty<Long> KAFKA_BROKERS_WAIT_FOR_RETRY_BACKOFF_MS = ConfigProperty
            .ofLong("kafka.brokers.wait-for-retry-backoff-ms")
            .orElse(DEFAULT_RETRY_BACKOFF_MS);

    public static final ConfigProperty<Long> KAFKA_BROKERS_WAIT_FOR_TIMEOUT_MS = ConfigProperty
            .ofLong("kafka.brokers.wait-for-timeout-ms")
            .orElse(DEFAULT_TIMEOUT_MS);


    private final Supplier<AdminClient> adminClientSupplier;

    private volatile String clusterId;

    private volatile AdminClient client;

    private KafkaBrokersReady.Options options = KafkaBrokersReady.Options.withDefaults()
            .withTimeoutMs(DEFAULT_TIMEOUT_MS)
            .withMinAvailableBrokers(DEFAULT_MIN_AVAILABLE_BROKERS)
            .withRetryBackoffMs(DEFAULT_RETRY_BACKOFF_MS);

    private boolean isWaitForKafkaBrokersEnabled = true;

    /**
     * Creates a new {@link AdminClientContext} instance with the specified {@link AdminClient} configuration.
     *
     * @param adminClientConfig   the {@link AdminClient} supplier.
     */
    public AdminClientContext(final @NotNull Properties adminClientConfig) {
        this(() -> KafkaUtils.newAdminClient(adminClientConfig));
    }

    /**
     * Creates a new {@link AdminClientContext} instance with the specified {@link AdminClient} supplier.
     *
     * @param adminClientSupplier   the {@link AdminClient} supplier.
     */
    public AdminClientContext(final @NotNull Supplier<AdminClient> adminClientSupplier) {
        this.adminClientSupplier = Objects.requireNonNull(
                adminClientSupplier,
                "'adminClientSupplier' should not be null"
        );
    }

    /**
     * Creates a new {@link AdminClientContext} instance with the specified application's configuration.
     *
     * @param config the {@link Configuration}.
     */
    public AdminClientContext(final @NotNull Configuration config) {
        this(ADMIN_CLIENT_CONFIG.evaluate(config));
        withWaitForKafkaBrokersEnabled(KAFKA_BROKERS_WAIT_FOR_ENABLED.evaluate(config));
        if (isWaitForKafkaBrokersEnabled) {
            withWaitForRetryBackoff(KAFKA_BROKERS_WAIT_FOR_RETRY_BACKOFF_MS.evaluate(config));
            withWaitForMinAvailableBrokers(KAFKA_BROKERS_WAIT_FOR_MIN_AVAILABLE.evaluate(config));
            withWaitTimeoutMs(KAFKA_BROKERS_WAIT_FOR_TIMEOUT_MS.evaluate(config));
        }
    }

    public AdminClientContext withWaitForKafkaBrokersEnabled(final boolean isWaitForKafkaBrokersEnabled) {
        this.isWaitForKafkaBrokersEnabled = isWaitForKafkaBrokersEnabled;
        return this;
    }

    public AdminClientContext withWaitForMinAvailableBrokers(final int waitForMinAvailableBrokers) {
        this.options = options.withMinAvailableBrokers(waitForMinAvailableBrokers);
        return this;
    }

    public AdminClientContext withWaitForRetryBackoff(final long waitForRetryBackoff) {
        this.options = options.withRetryBackoffMs(waitForRetryBackoff);
        return this;
    }

    public AdminClientContext withWaitTimeoutMs(final long waitTimeoutMs) {
        this.options = options.withTimeoutMs(waitTimeoutMs);
        return this;
    }

    /**
     * Invokes the specified function with a {@link AdminClient}.
     *
     * @param function  the {@link KafkaFunction}.
     * @return          the result of the execution.
     * @param <O>       the type of the result.
     */
    public <O> O invoke(final @NotNull KafkaFunction<O> function) {
        return function.apply(client());
    }

    /**
     * Invokes the specified function using an ephemeral {@link AdminClient}.
     *
     * @param function  the {@link KafkaFunction}.
     * @return          the result of the execution.
     * @param <O>       the type of the result.
     */
    public <O> O invokeAndClose(final @NotNull KafkaFunction<O> function) {
        try {
            return function.apply(client());
        } finally {
            close();
        }
    }

    public String getClusterId() {
        if (clusterId == null) {
            Optional<String> optional = invoke(adminClient -> {
                try {
                    return Optional.ofNullable(adminClient.describeCluster().clusterId().get());
                } catch (Exception e) {
                    LOG.error("Failed to describe cluster Id. Use default value 'unknown'", e);
                    return Optional.empty();
                }
            });
            clusterId = optional.orElse("unknown");
        }
        return clusterId;
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Gets the current {@link AdminClient} instance.
     *
     * @return  the {@link AdminClient}.
     * @throws  IllegalStateException if no AdminClient is currently created.
     */
    public AdminClient client() {
        if (initialized.compareAndSet(false, true)) {
            LOG.info("Initializing context for Kafka AdminClient");
            client = adminClientSupplier.get();
            if (isWaitForKafkaBrokersEnabled) {
                final boolean isReady = KafkaUtils.waitForKafkaBrokers(client, options);
                if (!isReady) {
                    throw new JikkouException(
                            "Timeout expired. The timeout period elapsed prior to " +
                            "the requested number of kafka brokers is available."
                    );
                }
            }
        }
        return client;
    }

    /** {@inheritDoc} **/
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            LOG.info("Closing context for Kafka AdminClient");
            try {
                if (client != null) {
                    client.close();
                    client = null;
                }
            } finally {
                closed.set(false);
                initialized.set(false);
            }
        }
    }
}