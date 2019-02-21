package com.linkedin.datastream.connectors.kafka.mirrormaker;

import java.util.Properties;

import com.linkedin.datastream.common.Datastream;
import com.linkedin.datastream.connectors.CommonConnectorMetrics;
import com.linkedin.datastream.connectors.kafka.GroupIdConstructor;
import com.linkedin.datastream.connectors.kafka.KafkaConsumerFactory;
import com.linkedin.datastream.server.DatastreamTask;


/**
 * Interface that is used to create topic manager.
 */
public interface TopicManagerFactory {
  /**
   * Method to create topic manager instance.
   * @param datastreamTask Task that topic manager is going to be created for.
   * @param datastream Datastream that topic manager is going to be created for.
   * @param groupIdConstructor This will be used while creating source/destination consumers
   * @param kafkaConsumerFactory This will be used to create source/destination kafka consumer
   * @param consumerProperties properties for Kafka consumer
   * @param topicManagerProperties Any additional properties that need to be passed to topic manager
   * @param consumerMetrics In case one needs to log consumer metrics.
   * @param metricsPrefix Prefix to use for metrics that topic manager emits.
   * @param metricsKey Key to use for metrics that topic manager emits
   * @return An instance of a class that implements TopicManager interface.
   */
  TopicManager createTopicManager(DatastreamTask datastreamTask, Datastream datastream,
      GroupIdConstructor groupIdConstructor, KafkaConsumerFactory<?, ?> kafkaConsumerFactory,
      Properties consumerProperties, Properties topicManagerProperties, CommonConnectorMetrics consumerMetrics,
      String metricsPrefix, String metricsKey);
}
