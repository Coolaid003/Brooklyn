package com.linkedin.datastream.common.databases.dbreader;

import org.testng.annotations.Test;


public class TestMysqlChunkedQueryManager extends TestChunkedQueryManagerBase {
  private static final ChunkedQueryManager MANAGER = new MySqlChunkedQueryManager();

  /**
   * Test query string when a single KEY is involved in chunking with a single PARTITION assigned.
   */
  @Test
  public void testSimpleKeySinglePartition() {

    /**
     *    SELECT * FROM
     *    (
     *        SELECT * FROM
     *            (
     *                SELECT * FROM TABLE
     *            ) nestedTab1
     *        WHERE ( MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 3 )
     *        ORDER BY KEY1
     *    ) as nestedTab2 LIMIT 10;
     */
    String firstExpected =
        "SELECT * FROM ( SELECT * FROM ( SELECT * FROM TABLE ) nestedTab1 "
            + "WHERE ( MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 3 ) ORDER BY KEY1 ) as nestedTab2 LIMIT 10";

    /**
     *   SELECT * FROM
     *   (
     *       SELECT * FROM
     *           (
     *               SELECT * FROM TABLE
     *           ) nestedTab1
     *       WHERE ( MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 3 ) AND ( ( KEY1 > ? ) )
     *       ORDER BY KEY1
     *   ) as nestedTab2 LIMIT 10;
     */
    String chunkedExpected = "SELECT * FROM ( SELECT * FROM ( SELECT * FROM TABLE ) nestedTab1 "
        + "WHERE ( MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 3 ) AND ( ( KEY1 > ? ) ) ORDER BY KEY1 ) as nestedTab2 LIMIT 10";
    testQueryString(MANAGER, firstExpected, chunkedExpected, NESTED_QUERY, KEY, CHUNK_SIZE, PARTITION_COUNT, PARTITION);
  }

  /**
   * Test query string when a single KEY is involved in chunking with a multiple PARTITIONS assigned.
   */
  @Test
  public void testSimpleKeyMultiPartition() {

    /**
     *   SELECT * FROM
     *   (
     *       SELECT * FROM
     *           (
     *               SELECT * FROM TABLE
     *           ) nestedTab1
     *       WHERE ( MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 2 OR MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 5 ) ORDER BY KEY1
     *   ) as nestedTab2 LIMIT 10;
     */
    String firstExpected = "SELECT * FROM ( SELECT * FROM ( SELECT * FROM TABLE ) nestedTab1 WHERE ( MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 2"
        + " OR MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 5 ) ORDER BY KEY1 ) as nestedTab2 LIMIT 10";

    /**
     *   SELECT * FROM
     *   (
     *       SELECT * FROM
     *           (
     *               SELECT * FROM TABLE
     *           ) nestedTab1
     *       WHERE ( MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 2 OR MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 5 ) AND ( ( KEY1 > ? ) ) ORDER BY KEY1
     *   ) as nestedTab2 LIMIT 10;
     */
    String chunkedExpected = "SELECT * FROM ( SELECT * FROM ( SELECT * FROM TABLE ) nestedTab1 "
        + "WHERE ( MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 2 OR MOD ( MD5 ( CONCAT ( KEY1 ) ) , 10 ) = 5 ) "
        + "AND ( ( KEY1 > ? ) ) ORDER BY KEY1 ) as nestedTab2 LIMIT 10";
    testQueryString(MANAGER, firstExpected, chunkedExpected, NESTED_QUERY, KEY, CHUNK_SIZE, PARTITION_COUNT,
        PARTITIONS);
  }

  @Test
  public void testCompositeKeySinglePartition() {

    /**
     *   SELECT * FROM
     *   (
     *       SELECT * FROM
     *           (
     *               SELECT * FROM TABLE
     *           ) nestedTab1 WHERE ( MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 3 )
     *       ORDER BY KEY1 , KEY2
     *   ) as nestedTab2 LIMIT 10;
     */
    String firstExpected = "SELECT * FROM ( SELECT * FROM ( SELECT * FROM TABLE ) nestedTab1"
        + " WHERE ( MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 3 ) ORDER BY KEY1 , KEY2 ) as nestedTab2 LIMIT 10";

     /**
      *   SELECT * FROM
      *   (
      *       SELECT * FROM
      *           (
      *               SELECT * FROM TABLE
      *           ) nestedTab1
      *       WHERE ( MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 3 ) AND ( ( KEY1 > 0 ) OR ( KEY1 = 0 AND KEY2 > 0 ) )
      *       ORDER BY KEY1 , KEY2
      *   ) as nestedTab2 LIMIT 10;
      */
    String chunkedExpected = "SELECT * FROM ( SELECT * FROM ( SELECT * FROM TABLE ) nestedTab1 "
        + "WHERE ( MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 3 ) AND ( ( KEY1 > ? ) OR ( KEY1 = ? AND KEY2 > ? ) ) "
        + "ORDER BY KEY1 , KEY2 ) as nestedTab2 LIMIT 10";
    testQueryString(MANAGER, firstExpected, chunkedExpected, NESTED_QUERY, KEYS, CHUNK_SIZE, PARTITION_COUNT,
        PARTITION);
  }

  @Test
  public void testCompositeKeyMultiPartition() {

    /**
     *   SELECT * FROM
     *   (
     *       SELECT * FROM
     *           (
     *               SELECT * FROM TABLE
     *           ) nestedTab1 WHERE ( MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 2 OR MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 5 )
     *       ORDER BY KEY1 , KEY2
     *   ) as nestedTab2 LIMIT 10;
     */
    String firstExpected = "SELECT * FROM ( SELECT * FROM ( SELECT * FROM TABLE ) nestedTab1 "
        + "WHERE ( MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 2 OR MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 5 ) "
        + "ORDER BY KEY1 , KEY2 ) as nestedTab2 LIMIT 10";

    /**
     *
     *    SELECT * FROM
     *    (
     *        SELECT * FROM
     *            (
     *                SELECT * FROM TABLE
     *            ) nestedTab1 WHERE ( MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 2
     *            OR MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 5 ) AND ( ( KEY1 > ? ) OR ( KEY1 = 0 AND KEY2 > ? ) )
     *        ORDER BY KEY1 , KEY2
     *    ) as nestedTab2 LIMIT 10;
     */
    String chunkedExpected = "SELECT * FROM ( SELECT * FROM ( SELECT * FROM TABLE ) nestedTab1 "
        + "WHERE ( MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 2 OR MOD ( MD5 ( CONCAT ( KEY1 , KEY2 ) ) , 10 ) = 5 ) "
        + "AND ( ( KEY1 > ? ) OR ( KEY1 = ? AND KEY2 > ? ) ) ORDER BY KEY1 , KEY2 ) as nestedTab2 LIMIT 10";
    testQueryString(MANAGER, firstExpected, chunkedExpected, NESTED_QUERY, KEYS, CHUNK_SIZE, PARTITION_COUNT,
        PARTITIONS);
  }
}
