package com.linkedin.datastream.server.providers;

import java.util.List;
import java.util.Map;

import com.linkedin.datastream.metrics.MetricsAware;
import com.linkedin.datastream.server.DatastreamTask;


/**
 * Checkpoint provider
 */
public interface CheckpointProvider extends MetricsAware {

  /**
   * Commit the checkpoints to the checkpoint store.
   * @param checkpoints Map of the datastreamTask to the checkpoint associated with the datastreamTask
   */
  void commit(Map<DatastreamTask, String> checkpoints);

  /**
   * Read the committed checkpoints from the checkpoint store
   * @param datastreamTasks List of datastream tasks whose checkpoints need to be read
   * @return Map of the checkpoints associated with the datastream task.
   */
  Map<DatastreamTask, String> getCommitted(List<DatastreamTask> datastreamTasks);
}
