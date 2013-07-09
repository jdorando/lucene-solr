package org.apache.lucene.util.packed;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.util.LuceneTestCase;

public class TestEliasFanoSequence extends LuceneTestCase {

  private static EliasFanoEncoder makeEncoder(long[] values) {
    long upperBound = -1L;
    for (long value: values) {
      assertTrue(value >= upperBound); // test data ok
      upperBound = value;
    }
    EliasFanoEncoder efEncoder = new EliasFanoEncoder(values.length, upperBound);
    for (long value: values) {
      efEncoder.encodeNext(value);
    }
    return efEncoder;
  }

  private static void tstDecodeAllNext(long[] values, EliasFanoDecoder efd) {
    efd.toBeforeSequence();
    long nextValue = efd.nextValue();
    for (long expValue: values) {
      assertFalse("nextValue at end too early", EliasFanoDecoder.NO_MORE_VALUES == nextValue);
      assertEquals(expValue, nextValue);
      nextValue = efd.nextValue();
    }
    assertEquals(EliasFanoDecoder.NO_MORE_VALUES, nextValue);
  }

  private static void tstDecodeAllPrev(long[] values, EliasFanoDecoder efd) {
    efd.toAfterSequence();
    for (int i = values.length - 1; i >= 0; i--) {
      long previousValue = efd.previousValue();
      assertFalse("previousValue at end too early", EliasFanoDecoder.NO_MORE_VALUES == previousValue);
      assertEquals(values[i], previousValue);
    }
    assertEquals(EliasFanoDecoder.NO_MORE_VALUES, efd.previousValue());
  }

  private static void tstDecodeAllAdvanceToExpected(long[] values, EliasFanoDecoder efd) {
    efd.toBeforeSequence();
    long previousValue = -1L;
    long index = 0;
    for (long expValue: values) {
      if (expValue > previousValue) {
        long advanceValue = efd.advanceToValue(expValue);
        assertFalse("advanceValue at end too early", EliasFanoDecoder.NO_MORE_VALUES == advanceValue);
        assertEquals(expValue, advanceValue);
        assertEquals(index, efd.index());
        previousValue = expValue;
      }
      index++;
    }
    long advanceValue = efd.advanceToValue(previousValue+1);
    assertEquals(EliasFanoDecoder.NO_MORE_VALUES, advanceValue);
  }

  private static void tstDecodeAdvanceToMultiples(long[] values, EliasFanoDecoder efd, final long m) {
    // test advancing to multiples of m
    assert m > 0;
    long previousValue = -1L;
    long index = 0;
    long mm = m;
    efd.toBeforeSequence();
    for (long expValue: values) {
      // mm > previousValue
      if (expValue >= mm) {
        long advanceValue = efd.advanceToValue(mm);
        assertFalse("advanceValue at end too early", EliasFanoDecoder.NO_MORE_VALUES == advanceValue);
        assertEquals(expValue, advanceValue);
        assertEquals(index, efd.index());
        previousValue = expValue;
        do {
          mm += m;
        } while (mm <= previousValue);
      }
      index++;
    }
    long advanceValue = efd.advanceToValue(mm);
    assertEquals(EliasFanoDecoder.NO_MORE_VALUES, advanceValue);
  }

  private static void tstDecodeBackToMultiples(long[] values, EliasFanoDecoder efd, final long m) {
    // test backing to multiples of m
    assert m > 0;
    efd.toAfterSequence();
    int index = values.length - 1;
    if (index < 0) {
      long advanceValue = efd.backToValue(0);
      assertEquals(EliasFanoDecoder.NO_MORE_VALUES, advanceValue);
      return; // empty values, nothing to go back to/from
    }
    long expValue = values[index];
    long previousValue = expValue + 1;
    long mm = (expValue / m) * m;
    while (index >= 0) {
      expValue = values[index];
      assert mm < previousValue;
      if (expValue <= mm) {
        long backValue = efd.backToValue(mm);
        assertFalse("backToValue at end too early", EliasFanoDecoder.NO_MORE_VALUES == backValue);
        assertEquals(expValue, backValue);
        assertEquals(index, efd.index());
        previousValue = expValue;
        do {
          mm -= m;
        } while (mm >= previousValue);
      }
      index--;
    }
    long backValue = efd.backToValue(mm);
    assertEquals(EliasFanoDecoder.NO_MORE_VALUES, backValue);
  }

  private static void tstEqual(String mes, long[] exp, long[] act) {
    assertEquals(mes + ".length", exp.length, act.length);
    for (int i = 0; i < exp.length; i++) {
      if (exp[i] != act[i]) {
        fail(mes + "[" + i + "] " + exp[i] + " != " + act[i]);
      }
    }
  }

  private static void tstDecodeAll(EliasFanoEncoder efEncoder, long[] values) {
    tstDecodeAllNext(values, efEncoder.getDecoder());
    tstDecodeAllPrev(values, efEncoder.getDecoder());
    tstDecodeAllAdvanceToExpected(values, efEncoder.getDecoder());
  }

  private static void tstEFS(long[] values, long[] expHighLongs, long[] expLowLongs) {
    EliasFanoEncoder efEncoder = makeEncoder(values);
    tstEqual("upperBits", expHighLongs, efEncoder.getUpperBits());
    tstEqual("lowerBits", expLowLongs, efEncoder.getLowerBits());
    tstDecodeAll(efEncoder, values);
  }

  private static void tstEFS2(long[] values) {
    EliasFanoEncoder efEncoder = makeEncoder(values);
    tstDecodeAll(efEncoder, values);
  }

  private static void tstEFSadvanceToAndBackToMultiples(long[] values, long maxValue, long minAdvanceMultiple) {
    EliasFanoEncoder efEncoder = makeEncoder(values);
    for (long m = minAdvanceMultiple; m <= maxValue; m += 1) {
      tstDecodeAdvanceToMultiples(values, efEncoder.getDecoder(), m);
      tstDecodeBackToMultiples(values, efEncoder.getDecoder(), m);
    }
  }

  public void testEmpty() {
    long[] values = new long[0];
    long[] expHighBits = new long[0];
    long[] expLowBits = new long[0];
    tstEFS(values, expHighBits, expLowBits);
  }

  public void testOneValue1() {
    long[] values = new long[] {0};
    long[] expHighBits = new long[] {0x1L};
    long[] expLowBits = new long[] {};
    tstEFS(values, expHighBits, expLowBits);
  }

  public void testTwoValues1() {
    long[] values = new long[] {0,0};
    long[] expHighBits = new long[] {0x3L};
    long[] expLowBits = new long[] {};
    tstEFS(values, expHighBits, expLowBits);
  }

  public void testOneValue2() {
    long[] values = new long[] {63};
    long[] expHighBits = new long[] {2};
    long[] expLowBits = new long[] {31};
    tstEFS(values, expHighBits, expLowBits);
  }

  public void testOneMaxValue() {
    long[] values = new long[] {Long.MAX_VALUE};
    long[] expHighBits = new long[] {2};
    long[] expLowBits = new long[] {Long.MAX_VALUE/2};
    tstEFS(values, expHighBits, expLowBits);
  }

  public void testTwoMinMaxValues() {
    long[] values = new long[] {0, Long.MAX_VALUE};
    long[] expHighBits = new long[] {0x11};
    long[] expLowBits = new long[] {0xE000000000000000L, 0x03FFFFFFFFFFFFFFL};
    tstEFS(values, expHighBits, expLowBits);
  }

  public void testTwoMaxValues() {
    long[] values = new long[] {Long.MAX_VALUE, Long.MAX_VALUE};
    long[] expHighBits = new long[] {0x18};
    long[] expLowBits = new long[] {-1L, 0x03FFFFFFFFFFFFFFL};
    tstEFS(values, expHighBits, expLowBits);
  }

  public void testExample1() { // Figure 1 from Vigna 2012 paper
    long[] values = new long[] {5,8,8,15,32};
    long[] expLowBits = new long[] {Long.parseLong("0011000001", 2)}; // reverse block and bit order
    long[] expHighBits = new long[] {Long.parseLong("1000001011010", 2)}; // reverse block and bit order
    tstEFS(values, expHighBits, expLowBits);
  }

  public void testHashCodeEquals() {
    long[] values = new long[] {5,8,8,15,32};
    EliasFanoEncoder efEncoder1 = makeEncoder(values);
    EliasFanoEncoder efEncoder2 = makeEncoder(values);
    assertEquals(efEncoder1, efEncoder2);
    assertEquals(efEncoder1.hashCode(), efEncoder2.hashCode());

    EliasFanoEncoder efEncoder3 = makeEncoder(new long[] {1,2,3});
    assertFalse(efEncoder1.equals(efEncoder3));
    assertFalse(efEncoder3.equals(efEncoder1));
    assertFalse(efEncoder1.hashCode() == efEncoder3.hashCode()); // implementation ok for these.
  }

  public void testMonotoneSequences() {
    for (int s = 2; s < 1222; s++) {
      long[] values = new long[s];
      for (int i = 0; i < s; i++) {
        values[i] = (i/2);
      }
      tstEFS2(values);
    }
  }

  public void testStrictMonotoneSequences() {
    for (int s = 2; s < 1222; s++) {
      long[] values = new long[s];
      for (int i = 0; i < s; i++) {
        values[i] = i * ((long) i - 1) / 2; // Add a gap of (s-1) to previous
        // s = (s*(s+1) - (s-1)*s)/2
      }
      tstEFS2(values);
    }
  }

  public void testHighBitLongZero() {
    final int s = 65;
    long[] values = new long[s];
    for (int i = 0; i < s-1; i++) {
      values[i] = 0;
    }
    values[s-1] = 128;
    long[] expHighBits = new long[] {-1,0,0,1};
    long[] expLowBits = new long[0];
    tstEFS(values, expHighBits, expLowBits);
  }

  public void testAdvanceToAndBackToMultiples() {
    for (int s = 2; s < 130; s++) {
      long[] values = new long[s];
      for (int i = 0; i < s; i++) {
        values[i] = i * ((long) i + 1) / 2; // Add a gap of s to previous
        // s = (s*(s+1) - (s-1)*s)/2
      }
      tstEFSadvanceToAndBackToMultiples(values, values[s-1], 10);
    }
  }
}

