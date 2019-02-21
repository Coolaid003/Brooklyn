package com.linkedin.datastream.testutil.event.generator;

import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;


public class RecordSchemaField extends SchemaField {

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

  public GenericRecord generateRecord() throws UnknownTypeException {
    GenericRecord subRecord = new GenericData.Record(_field.schema());
    for (Field field : _field.schema().getFields()) {
      SchemaField fill = SchemaField.createField(field);
      fill.writeToRecord(subRecord);
    }

    return subRecord;
  }
}
