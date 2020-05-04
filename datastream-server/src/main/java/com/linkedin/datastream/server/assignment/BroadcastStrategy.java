/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.server.assignment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.datastream.server.DatastreamGroup;
import com.linkedin.datastream.server.DatastreamTask;
import com.linkedin.datastream.server.DatastreamTaskImpl;
import com.linkedin.datastream.server.api.strategy.AssignmentStrategy;

import static com.linkedin.datastream.server.assignment.BroadcastStrategyFactory.CFG_MAX_TASKS;
import static com.linkedin.datastream.server.assignment.BroadcastStrategyFactory.CFG_MAX_TASKS_PER_INSTANCE;


/**
 * The number of tasks created for datastream is configurable using "maxTasks" config. This can also be overridden at the
 * Datastream level via the Datastream metadata "maxTasks". The number of tasks is not necessarily capped at the
 * number of instances, so each instance could process multiple tasks for the same Datastream. If "maxTasks" is not
 * provided, the strategy will broadcast one task to each of the instances in the cluster.
 *
 * The "maxTasksPerInstance" setting limits the number of tasks per datastream.
 * If "maxTasksPerInstance" is not provided, the strategy will not limit the number of tasks per instance
 *
 * All the tasks are redistributed across all the instances equally.
 */
public class BroadcastStrategy implements AssignmentStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(BroadcastStrategy.class.getName());

  private final Optional<Integer> _maxTasks;
  private final Optional<Integer> _maxTasksPerInstance;

  /**
   * Constructor for BroadcastStrategy
   * @param maxTasks Maximum number of {@link DatastreamTask}s to create out
   *                 of any {@link com.linkedin.datastream.common.Datastream}
   *                 if no value is specified for the "maxTasks" config property
   *                 at an individual datastream level.
   */
  public BroadcastStrategy(Optional<Integer> maxTasks) {
    _maxTasks = maxTasks;
    _maxTasksPerInstance = Optional.empty();
  }

  /**
   * Constructor for BroadcastStrategy
   * @param maxTasks            Maximum number of {@link DatastreamTask}s to create out
   *                            of any {@link com.linkedin.datastream.common.Datastream}
   *                            if no value is specified for the "maxTasks" config property
   *                            at an individual datastream level.
   * @param maxTasksPerInstance Maximum number of {@link DatastreamTask}s per instance to create out
   *                            of any {@link com.linkedin.datastream.common.Datastream}
   */
  public BroadcastStrategy(Optional<Integer> maxTasks, Optional<Integer> maxTasksPerInstance) {
    _maxTasks = maxTasks;
    _maxTasksPerInstance = maxTasksPerInstance;
  }

  @Override
  public Map<String, Set<DatastreamTask>> assign(List<DatastreamGroup> datastreams, List<String> instances,
      Map<String, Set<DatastreamTask>> currentAssignment) {

    int totalAssignedTasks = currentAssignment.values().stream().mapToInt(Set::size).sum();
    LOG.info("Assigning {} datastreams to {} instances with {} tasks", datastreams.size(), instances.size(),
            totalAssignedTasks);

    // if there are no live instances, return empty assignment
    if (instances.isEmpty()) {
      return new HashMap<>();
    }

    Map<String, Set<DatastreamTask>> newAssignment = new HashMap<>();
    Integer[] assignmentCountForDatastream = new Integer[instances.size()];

    // Make a copy of the current assignment, since the strategy modifies it during calculation
    Map<String, Set<DatastreamTask>> currentAssignmentCopy = new HashMap<>(currentAssignment.size());
    currentAssignment.forEach((k, v) -> currentAssignmentCopy.put(k, new HashSet<>(v)));

    for (String instance : instances) {
      newAssignment.put(instance, new HashSet<>());
      currentAssignmentCopy.putIfAbsent(instance, new HashSet<>());
    }

    int instancePos = 0;
    for (DatastreamGroup dg : datastreams) {
      int numTasks = getNumTasks(dg, instances.size());
      Optional<Integer> maxTasksPerInstance = getMaxNumTasksPerInstance(dg);

      // initialize the assignment counts on each datastream
      Arrays.fill(assignmentCountForDatastream, 0);

      for (int taskPos = 0; taskPos < numTasks; taskPos++) {
        String instance = instances.get(instancePos);

        DatastreamTask foundDatastreamTask = currentAssignmentCopy.get(instance)
            .stream()
            .filter(x -> x.getTaskPrefix().equals(dg.getTaskPrefix()))
            .findFirst()
            .orElse(new DatastreamTaskImpl(dg.getDatastreams()));

        currentAssignmentCopy.get(instance).remove(foundDatastreamTask);
        newAssignment.get(instance).add(foundDatastreamTask);
        assignmentCountForDatastream[instancePos]++;

        // Move to the next instance or the next datastream if there is no available capacity
        Optional<Integer> nextPos = getNextInstanceWithCapacity(instances, assignmentCountForDatastream, maxTasksPerInstance, instancePos);
        if (nextPos.isPresent()) {
          instancePos = nextPos.get();
        } else {
          break;
        }
      }
    }

    LOG.info("New assignment is {}", newAssignment);

    return newAssignment;
  }

  private Optional<Integer> getNextInstanceWithCapacity(List<String> instances, Integer[] assignmentCountForDatastream,
                                                        Optional<Integer> maxTasksPerInstance, int prevPos) {
    int pos = (prevPos + 1) % instances.size();

    if (!maxTasksPerInstance.isPresent()) {
      return Optional.of(pos);
    }

    while (assignmentCountForDatastream[pos] >= maxTasksPerInstance.get() && pos != prevPos) {
      pos = (pos + 1) % instances.size();
    }

    return  assignmentCountForDatastream[pos] < maxTasksPerInstance.get() ? Optional.of(pos) : Optional.empty();
  }

  private int getNumTasks(DatastreamGroup dg, int numInstances) {
    // Look for an override in any of the datastream. In the case of multiple overrides, select the largest.
    // If no override is present then use the default "_maxTasks" from config.
    return dg.getDatastreams()
        .stream()
        .map(ds -> ds.getMetadata().get(CFG_MAX_TASKS))
        .filter(Objects::nonNull)
        .mapToInt(Integer::valueOf)
        .filter(x -> x > 0)
        .max()
        .orElse(_maxTasks.orElse(numInstances));
  }

  private Optional<Integer> getMaxNumTasksPerInstance(DatastreamGroup dg) {
    // Look for an override in any of the datastream. In the case of multiple overrides, select the largest.
    // If no override is present then use the default "_maxTasksPerInstance" from config.
    OptionalInt overrideValue = dg.getDatastreams()
            .stream()
            .map(ds -> ds.getMetadata().get(CFG_MAX_TASKS_PER_INSTANCE))
            .filter(Objects::nonNull)
            .mapToInt(Integer::valueOf)
            .max();
    return overrideValue.isPresent() ? Optional.of(overrideValue.getAsInt()) : _maxTasksPerInstance;
  }
}
