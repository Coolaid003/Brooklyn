package com.linkedin.datastream.avrogenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Preconditions;


/**
 * Encapsulates metadata about an Oracle column/field.
 */
public class FieldMetadata {

  private static final String META_LIST_DELIMITER = ";";
  private static final String META_VALUE_DELIMITER = "=";

  private final String _dbFieldName;
  private int _dbFieldPosition;
  private Types _dbFieldType;

  private final Optional<Integer> _numberPrecision;
  private final Optional<Integer> _numberScale;

  public FieldMetadata(@NotNull String dbFieldName, int dbFieldPosition, @NotNull Types dbFieldType,
      Optional<Integer> numberPrecision, Optional<Integer> numberScale) {
    _dbFieldName = dbFieldName;
    _dbFieldPosition = dbFieldPosition;
    _dbFieldType = dbFieldType;
    _numberPrecision = numberPrecision;
    _numberScale = numberScale;
  }

  public static FieldMetadata fromString(String meta) {
    // cut off the trailing delimiter ";"
    if (meta.endsWith(META_LIST_DELIMITER)) {
      meta = meta.substring(0, meta.length() - 1);
    }

    String[] parts = meta.split(META_LIST_DELIMITER);
    Map<String, String> metas = new HashMap<>(parts.length);
    for (String part : parts) {
      String[] keyValuePair = part.split(META_VALUE_DELIMITER);
      if (keyValuePair.length != 2) {
        throw new IllegalArgumentException("Ill-formatted meta key-value pair: " + part);
      }
      String key = keyValuePair[0];
      String value = keyValuePair[1];

      metas.put(key, value);
    }

    // field name, field position, and field type are mandatory
    String fieldName = Preconditions.checkNotNull(metas.get(OracleColumn.COL_NAME),
        String.format("Missing metadata %s from meta string %s", OracleColumn.COL_NAME, meta));
    int fieldPosition = Integer.valueOf(Preconditions.checkNotNull(metas.get(OracleColumn.COL_POSITION),
        String.format("Missing metadata %s from meta string %s", OracleColumn.COL_POSITION, meta)));
    Types fieldType = Types.valueOf(Preconditions.checkNotNull(metas.get(FieldType.FIELD_TYPE_NAME),
        String.format("Missing metadata %s from meta string %s", FieldType.FIELD_TYPE_NAME, meta)));

    Optional<Integer> numberPrecision =
        Optional.ofNullable(metas.get(FieldType.PRECISION)).map(s -> Integer.valueOf(s));
    Optional<Integer> numberScale =
        Optional.ofNullable(metas.get(FieldType.SCALE)).map(s -> Integer.valueOf(s));

    return new FieldMetadata(fieldName, fieldPosition, fieldType, numberPrecision, numberScale);
  }

  public String getDbFieldName() {
    return _dbFieldName;
  }

  public int getDbFieldPosition() {
    return _dbFieldPosition;
  }

  public Types getDbFieldType() {
    return _dbFieldType;
  }

  public Optional<Integer> getNumberPrecision() {
    return _numberPrecision;
  }

  public Optional<Integer> getNumberScale() {
    return _numberScale;
  }

}
