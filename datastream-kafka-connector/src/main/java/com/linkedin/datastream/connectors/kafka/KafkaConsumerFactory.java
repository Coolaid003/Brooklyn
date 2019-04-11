/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.connectors.kafka;

import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;

/**
 * Interface for Kafka consumers
 * @param <K> Type to be deserialized into. This is for Key
 * @param <V> Type to be deserialized into. This is for Value
 */
public interface KafkaConsumerFactory<K, V> {
  /**
  * create a consumer instance using given {@link Properties}
  */
  Consumer<K, V> createConsumer(Properties properties);
}
