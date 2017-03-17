package com.linkedin.datastream.server.api.connector;

import java.util.List;
import java.util.Optional;

import com.linkedin.datastream.common.Datastream;


/**
 * Interface that the connectors can implement to find a duplicate datastream.
 * Any datastreams that can share the destination are considered as duplicate datastreams.
 */
public interface DatastreamDeduper {

  /**
   * Find an existing datastream that is a duplicate of the new datastream being created.
   * @param newDatastream new datastream being created.
   * @param allDatastream all the existing datastreams in the system includes the new datastream as well.
   * @return the existing datastream which is a duplicate of the new datastream being created or Optional.empty()
   */
  Optional<Datastream> findExistingDatastream(Datastream newDatastream, List<Datastream> allDatastream);
}
