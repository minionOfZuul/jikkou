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
package io.streamthoughts.jikkou.kafka.connect.reconcilier;

import io.streamthoughts.jikkou.core.annotation.SupportedResource;
import io.streamthoughts.jikkou.core.config.Configuration;
import io.streamthoughts.jikkou.core.exceptions.ConfigException;
import io.streamthoughts.jikkou.core.extension.ContextualExtension;
import io.streamthoughts.jikkou.core.extension.ExtensionContext;
import io.streamthoughts.jikkou.core.extension.annotations.ExtensionOptionSpec;
import io.streamthoughts.jikkou.core.extension.annotations.ExtensionSpec;
import io.streamthoughts.jikkou.core.models.ResourceListObject;
import io.streamthoughts.jikkou.core.reconcilier.Collector;
import io.streamthoughts.jikkou.core.selector.Selector;
import io.streamthoughts.jikkou.kafka.connect.KafkaConnectExtensionConfig;
import io.streamthoughts.jikkou.kafka.connect.api.KafkaConnectApi;
import io.streamthoughts.jikkou.kafka.connect.api.KafkaConnectApiFactory;
import io.streamthoughts.jikkou.kafka.connect.api.KafkaConnectClientConfig;
import io.streamthoughts.jikkou.kafka.connect.collections.V1KafkaConnectorList;
import io.streamthoughts.jikkou.kafka.connect.exception.KafkaConnectClusterNotFoundException;
import io.streamthoughts.jikkou.kafka.connect.models.V1KafkaConnector;
import io.streamthoughts.jikkou.kafka.connect.service.KafkaConnectClusterService;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ResourceCollector to get {@link V1KafkaConnector} resources.
 */
@SupportedResource(type = V1KafkaConnector.class)
@ExtensionSpec(
        options = {
                @ExtensionOptionSpec(
                        name = KafkaConnectorCollector.EXPAND_STATUS_CONFIG,
                        description = "Retrieves additional information about the status of the connector and its tasks.",
                        type = Boolean.class,
                        defaultValue = "false"
                ),
                @ExtensionOptionSpec(
                        name = KafkaConnectorCollector.CONNECT_CLUSTER_CONFIG,
                        description = "List of Kafka Connect cluster from which to list connectors.",
                        type = List.class
                )
        }
)
public final class KafkaConnectorCollector extends ContextualExtension implements Collector<V1KafkaConnector> {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConnectorCollector.class);

    public static final String EXPAND_STATUS_CONFIG = "expand-status";
    public static final String CONNECT_CLUSTER_CONFIG = "connect-cluster";

    private KafkaConnectExtensionConfig configuration;

    /**
     * {@inheritDoc}
     **/
    @Override
    public void init(@NotNull ExtensionContext context) throws ConfigException {
        super.init(context);
        init(new KafkaConnectExtensionConfig(context.appConfiguration()));
    }

    public void init(@NotNull KafkaConnectExtensionConfig configuration) {
        this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public ResourceListObject<V1KafkaConnector> listAll(@NotNull Configuration configuration,
                                                        @NotNull Selector selector) {

        Boolean expandStatus = extensionContext()
                .<Boolean>configProperty(EXPAND_STATUS_CONFIG).get(configuration);

        Set<String> clusters = extensionContext()
                .<List<String>>configProperty(CONNECT_CLUSTER_CONFIG)
                .getOptional(configuration)
                .map(list -> (Set<String>) new HashSet<>(list))
                .orElseGet(() -> this.configuration.getClusters());

        List<V1KafkaConnector> list = clusters
                .stream()
                .flatMap(connectCluster -> listAll(connectCluster, expandStatus).stream())
                .collect(Collectors.toList());
        return new V1KafkaConnectorList(list);
    }

    public List<V1KafkaConnector> listAll(final String cluster, final boolean expandStatus) {
        List<V1KafkaConnector> results = new LinkedList<>();
        KafkaConnectClientConfig connectClientConfig = configuration
                .getConfigForCluster(cluster)
                .orElseThrow(() -> new KafkaConnectClusterNotFoundException("No connect cluster configured for name '" + cluster + "'"));
        KafkaConnectApi api = KafkaConnectApiFactory.create(connectClientConfig);
        try {
            final List<String> connectors = api.listConnectors();
            for (String connector : connectors) {
                try {
                    KafkaConnectClusterService service = new KafkaConnectClusterService(cluster, api);
                    CompletableFuture<V1KafkaConnector> future = service.getConnectorAsync(connector, expandStatus);
                    V1KafkaConnector result = future.get();
                    results.add(result);
                } catch (Exception ex) {
                    if (ex instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    LOG.error("Failed to get connector '{}' from connect cluster {}", connector, cluster, ex);
                }
            }
        } finally {
            api.close();
        }
        return results;
    }


}
