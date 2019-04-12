/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.testutil.common;

import java.util.Random;


/**
 * This helper class can be used to generate various types of random values (int, String, double, float, long, boolean,
 * byte array)
 */
public class RandomValueGenerator {

  private final String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private Random rand;

  /**
   * Constructor for RandomValueGenerator
   * @param seed the seed used to generate the random value
   */
  public RandomValueGenerator(long seed) {
    rand = new Random(seed);
  }

  public int getNextInt() {
    return rand.nextInt();
  }

  // to make it inclusive of min and max for the range, add 1 to the difference

  /**
   * Get a random integer between {@code min} and {@code max}, inclusive
   * @param min the minimum value (inclusive) to generate
   * @param max the maximum value (inclusive) to generate
   */
  public int getNextInt(int min, int max) {
    if (max == min) {
      return min;
    }

    return (rand.nextInt(max - min + 1) + min);
  }

  /**
   * Get a random string of length between {@code min} and {@code max}, inclusive
   * @param min the minimum size of the randomly generated string
   * @param max the maximum size of the randomly generated string
   */
  public String getNextString(int min, int max) {
    int length = getNextInt(min, max);

    StringBuilder strbld = new StringBuilder();
    for (int i = 0; i < length; i++) {
      char ch = validChars.charAt(rand.nextInt(validChars.length()));
      strbld.append(ch);
    }

    return strbld.toString();
  }

  /**
   * Get a random double value
   */
  public double getNextDouble() {
    return rand.nextDouble();
  }

  /**
   * Get a random float value
   */
  public float getNextFloat() {
    return rand.nextFloat();
  }

  /**
   * Get a random positive long value
   */
  public long getNextLong() {
    long randomLong = rand.nextLong();

    return randomLong == Long.MIN_VALUE ? 0 : Math.abs(randomLong);
  }

  /**
   * Get a random boolean value
   */
  public boolean getNextBoolean() {
    return rand.nextBoolean();
  }

  /**
   * Get a random byte array of max length {@code maxBytesLength}
   * @param maxBytesLength the max length of the randomly generated byte array
   * @return
   */
  public byte[] getNextBytes(int maxBytesLength) {
    byte[] bytes = new byte[this.getNextInt(0, maxBytesLength)];
    rand.nextBytes(bytes);
    return bytes;
  }
}
