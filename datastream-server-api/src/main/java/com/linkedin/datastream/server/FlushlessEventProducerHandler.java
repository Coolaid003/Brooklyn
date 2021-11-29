/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.server;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.datastream.server.api.transport.SendCallback;



/**
 * Wraps a {@link DatastreamEventProducer} and keeps track of the in-flight messages and the acknowledged checkpoints
 * for each source partition.
 *
 * The main assumption of this class is that: for each source/partition tuple, the send method should
 * be called with monotonically ascending UNIQUE checkpoints.
 *
 * @param <T> Type of the comparable checkpoint object internally used by the connector.
 */
public class FlushlessEventProducerHandler<T extends Comparable<T>> {

  private static final Logger LOG = LoggerFactory.getLogger(FlushlessEventProducerHandler.class);

  private final DatastreamEventProducer _eventProducer;
  private final ConcurrentHashMap<SourcePartition, CallbackStatus> _callbackStatusMap = new ConcurrentHashMap<>();

  /**
   * Constructor for FlushlessEventProducerHandler
   */
  public FlushlessEventProducerHandler(DatastreamEventProducer eventProducer) {
    _eventProducer = eventProducer;
    _eventProducer.enablePeriodicFlushOnSend(false);
  }

  /**
   * Reset all the in-flight status counters, and metadata stored for the {@link DatastreamEventProducer}.
   * This should be used after calling flush and getting partition reassignments.
   */
  public void clear() {
    _callbackStatusMap.clear();
  }

  /**
   * Clear the source-partition entry from the _callbackStatusMap
   */
  public void clear(String source, int partition) {
    _callbackStatusMap.remove(new SourcePartition(source, partition));
  }

  /**
   * Sends event to the transport.
   *
   * NOTE: This method should be called with monotonically increasing checkpoints for a given source and
   * sourcePartition.
   * @param record the event to send
   * @param sourceCheckpoint the sourceCheckpoint associated with this event. Multiple events could share the same
   *                         sourceCheckpoint.
   */
  public void send(DatastreamProducerRecord record, String source, int sourcePartition, T sourceCheckpoint, SendCallback callback) {
    SourcePartition sp = new SourcePartition(source, sourcePartition);
    CallbackStatus status = _callbackStatusMap.computeIfAbsent(sp, d -> new CallbackStatus());
    status.register(sourceCheckpoint);
    _eventProducer.send(record, ((metadata, exception) -> {
      if (exception != null) {
        LOG.debug("Failed to send datastream record: " + metadata, exception);
      } else {
        status.ack(sourceCheckpoint);
      }
      if (callback != null) {
        callback.onCompletion(metadata, exception);
      }
    }));
  }

  /**
   * Get the latest safe checkpoint acknowledged by a sourcePartition, or an empty optional if no event has been
   * acknowledged.
   */
  public Optional<T> getAckCheckpoint(String source, int sourcePartition) {
    CallbackStatus status = _callbackStatusMap.get(new SourcePartition(source, sourcePartition));
    return Optional.ofNullable(status).map(CallbackStatus::getAckCheckpoint);
  }

  /**
   * Get the in-flight count of messages yet to be acknowledged for a given source and sourcePartition
   */
  public long getInFlightCount(String source, int sourcePartition) {
    CallbackStatus status = _callbackStatusMap.get(new SourcePartition(source, sourcePartition));
    return status != null ? status.getInFlightCount() : 0;
  }

  /**
   * Get a map of all source partitions to their in-flight message counts
   */
  public Map<SourcePartition, Long> getInFlightMessagesCounts() {
    return _callbackStatusMap.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getInFlightCount()));
  }

  /**
   * Get the in-flight count of messages yet to be acknowledged for a given source and sourcePartition
   */
  public long getAckMessagesPastCheckpointCount(String source, int sourcePartition) {
    CallbackStatus status = _callbackStatusMap.get(new SourcePartition(source, sourcePartition));
    return status != null ? status.getAckMessagesPastCheckpointCount() : 0;
  }

  /**
   * Get a map of all source partitions to their in-flight message counts
   */
  public Map<SourcePartition, Long> getAckMessagesPastCheckpointCounts() {
    return _callbackStatusMap.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getAckMessagesPastCheckpointCount()));
  }

  /**
   * Get the smallest checkpoint acknowledged by all destinations, or an empty if no event has been acknowledged.
   * If all tasks are up to date, returns the passed {@code currentCheckpoint}
   *
   * NOTE: This method assumes that the checkpoints are monotonically increasing across DestinationPartition.
   *       For example, for a connector reading from a source with a global monotonic SCN this function will
   *       work correctly.
   */
  public Optional<T> getAckCheckpoint(T currentCheckpoint, Comparator<T> checkpointComparator) {
    T lowWaterMark = null;

    for (CallbackStatus status : _callbackStatusMap.values()) {
      if (status.getInFlightCount() > 0) {
        T checkpoint = status.getAckCheckpoint();
        if (checkpoint == null) {
          return Optional.empty(); // no events ack yet for this topic partition
        }
        if (lowWaterMark == null || checkpointComparator.compare(checkpoint, lowWaterMark) < 0) {
          lowWaterMark = checkpoint;
        }
      }
    }

    return lowWaterMark != null ? Optional.of(lowWaterMark) : Optional.ofNullable(currentCheckpoint);
  }

  /**
   * Represents a pair of source (i.e. topic) and partition.
   */
  public static final class SourcePartition extends Pair<String, Integer> {

    /**
     * Constructor for SourcePartition
     */
    public SourcePartition(String source, int partition) {
      super(source, partition);
    }

    public String getSource() {
      return getKey();
    }

    public int getPartition() {
      return getValue();
    }

    @Override
    public String toString() {
      return getSource() + "-" + getPartition();
    }
  }

  /**
   * Helper class to store the callback status of the inflight events.
   */
  private class CallbackStatus {

    // the last record which is checkpoint-ed or is to be checkpoint-ed
    private T _currentCheckpoint = null;

    // Deque to store all the messages which are inflight
    private final Deque<T> _inFlight = new ArrayDeque<>();

    // Hashset storing all the records for which the ack is received
    private final Set<T> _acked = Collections.synchronizedSet(new HashSet<>());

    public T getAckCheckpoint() {
      return _currentCheckpoint;
    }

    // Get the count of the records which are in flight
    public long getInFlightCount() {
      return _inFlight.size();
    }

    // Get the count of the records which are all acked from the producer
    public long getAckMessagesPastCheckpointCount() {
      return _acked.size();
    }

    /**
     * Registers the given checkpoint by adding it to the deque of in-flight checkpoints.
     * @param checkpoint the checkpoint to register
     */
    public synchronized void register(T checkpoint) {
      _inFlight.offerLast(checkpoint);
    }

    /**
     * The checkpoint acknowledgement can be received out of order. So here, we track the checkpoints by adding
     * them in the _acked set and only update the _currentCheckpoint if a contiguous sequence of offsets are ack-ed
     * from the front of the queue.
     */
    public synchronized void ack(T checkpoint) {
      _acked.add(checkpoint);

      while (!_inFlight.isEmpty() && !_acked.isEmpty() && _acked.contains(_inFlight.peekFirst())) {
        _currentCheckpoint = _inFlight.pollFirst();

        if (!_acked.remove(_currentCheckpoint)) {
          LOG.error("Internal state error; could not remove checkpoint {}", _currentCheckpoint);
        }
      }
    }
  }
}
