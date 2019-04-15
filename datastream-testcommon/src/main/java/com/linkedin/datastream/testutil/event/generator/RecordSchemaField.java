/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.testutil.event.generator;

import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;


/**
 * A {@link org.apache.avro.generic.GenericRecord} schema generator for a specific {@link org.apache.avro.Schema.Field}
 */
public class RecordSchemaField extends SchemaField {

  /**
   * Construct an instance of RecordSchemaField using given {@link Field}
   */
  public RecordSchemaField(Field field) {
    super(field);
  }

  @Override
  public void writeToRecord(IndexedRecord record) throws UnknownTypeException {

    record.put(_field.pos(), generateRecord());
  }

  @Override
  public Object generateRandomObject() throws UnknownTypeException {
    return generateRecord();
  }

  /**
   * generate a GenericRecord with random number of fields
   */
  public GenericRecord generateRecord() throws UnknownTypeException {
    GenericRecord subRecord = new GenericData.Record(_field.schema());
    for (Field field : _field.schema().getFields()) {
      SchemaField fill = SchemaField.createField(field);
      fill.writeToRecord(subRecord);
    }

    return subRecord;
  }
}
