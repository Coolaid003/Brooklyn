/**
 *  Copyright 2021 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.server.assignment;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.linkedin.datastream.server.ClusterThroughputInfo;
import com.linkedin.datastream.server.DatastreamGroupPartitionsMetadata;
import com.linkedin.datastream.server.DatastreamTask;
import com.linkedin.datastream.server.DatastreamTaskImpl;
import com.linkedin.datastream.server.PartitionThroughputInfo;


/**
 * Performs partition assignment based on partition throughput information
 */
public class LoadBasedPartitionAssigner {

  private static final Integer DEFAULT_KB_RATE = 5;
  private static final Integer DEFAULT_MESSAGE_RATE = 5;

  /**
   * Performs partition assignment based on partition throughput information
   * @param throughputInfo Per partition throughput information
   * @param currentAssignment Current assignment
   * @param unassignedPartitions Unassigned partitions
   * @param partitionMetadata Partition metadata
   * @return New assignment
   */
  public Map<String, Set<DatastreamTask>> assignPartitions(ClusterThroughputInfo throughputInfo,
      Map<String, Set<DatastreamTask>> currentAssignment, List<String> unassignedPartitions,
      DatastreamGroupPartitionsMetadata partitionMetadata) {
    String datastreamGroupName = partitionMetadata.getDatastreamGroup().getName();
    Map<String, PartitionThroughputInfo> partitionInfoMap = throughputInfo.getPartitionInfoMap();

    // filter out all the tasks for the current datastream group, and retain assignments in a map
    Map<String, Set<String>> newPartitions = new HashMap<>();
    currentAssignment.values().forEach(tasks -> tasks.forEach(task -> {
      if (task.getTaskPrefix().equals(datastreamGroupName)) {
        Set<String> retainedPartitions = new HashSet<>(task.getPartitionsV2());
        retainedPartitions.retainAll(partitionMetadata.getPartitions());
        newPartitions.put(task.getId(), retainedPartitions);
      }
    }));

    // sort the current tasks on total throughput
    Map<String, Integer> taskThroughputMap = new HashMap<>();
    PartitionThroughputInfo defaultPartitionInfo = new PartitionThroughputInfo(DEFAULT_KB_RATE, DEFAULT_MESSAGE_RATE, "");
    newPartitions.forEach((task, partitions) -> {
      int totalThroughput = partitions.stream().
          mapToInt(p -> partitionInfoMap.getOrDefault(p, defaultPartitionInfo).getBytesInKBRate()).sum();
      taskThroughputMap.put(task, totalThroughput);
    });
    List<String> sortedTasks = newPartitions.keySet().stream().sorted(Comparator.comparing(taskThroughputMap::get)).
        collect(Collectors.toList());

    // sort unassigned partitions on throughput
    unassignedPartitions.sort((p1, p2) -> {
      Integer p1KBRate = partitionInfoMap.getOrDefault(p1, defaultPartitionInfo).getBytesInKBRate();
      Integer p2KBRate = partitionInfoMap.getOrDefault(p2, defaultPartitionInfo).getBytesInKBRate();
      return p1KBRate.compareTo(p2KBRate);
    });

    // assign the new partitions one by one
    while (unassignedPartitions.size() > 0) {
       String heaviestPartition = unassignedPartitions.remove(unassignedPartitions.size() - 1);
       int heaviestPartitionThroughput = partitionInfoMap.getOrDefault(heaviestPartition, defaultPartitionInfo)
           .getBytesInKBRate();
       String lightestTask = sortedTasks.remove(0);
       newPartitions.get(lightestTask).add(heaviestPartition);
       taskThroughputMap.put(lightestTask, taskThroughputMap.get(lightestTask) + heaviestPartitionThroughput);
       this.insertTaskIntoSortedList(lightestTask, sortedTasks, taskThroughputMap);
    }

    // build the new assignment using the new partitions for the affected datastream's tasks
    Map<String, Set<DatastreamTask>> newAssignments = new HashMap<>();
    currentAssignment.keySet().forEach(instance -> {
      Set<DatastreamTask> oldTasks = currentAssignment.get(instance);
      Set<DatastreamTask> newTasks = oldTasks.stream().map(task -> {
        if (task.getTaskPrefix().equals(datastreamGroupName)) {
          return new DatastreamTaskImpl((DatastreamTaskImpl) task, newPartitions.get(task.getId()));
        }
        return task;
      }).collect(Collectors.toSet());
      newAssignments.put(instance, newTasks);
    });

    return newAssignments;
  }

  private void insertTaskIntoSortedList(String task, List<String> sortedTasks, Map<String, Integer> taskThroughputMap) {
    int index = Collections.binarySearch(sortedTasks, task, Comparator.comparing(taskThroughputMap::get));
    if (index < 0) {
      index = -index - 1;
    }
    sortedTasks.add(index, task);
  }
}
