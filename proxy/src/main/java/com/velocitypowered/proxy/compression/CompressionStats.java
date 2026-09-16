/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.compression;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Tracks aggregate proxy compression statistics.
 */
public final class CompressionStats {

  public static final CompressionStats INSTANCE = new CompressionStats();

  private final LongAdder totalPackets = new LongAdder();
  private final LongAdder compressedPackets = new LongAdder();
  private final LongAdder passthroughPackets = new LongAdder();
  private final LongAdder totalRawBytes = new LongAdder();
  private final LongAdder totalEncodedBytes = new LongAdder();
  private final LongAdder compressedRawBytes = new LongAdder();
  private final LongAdder compressedEncodedBytes = new LongAdder();
  private final AtomicLong resetNanoTime = new AtomicLong(System.nanoTime());

  private CompressionStats() {
  }

  /**
   * Records a packet that went through the compression path.
   *
   * @param rawBytes the uncompressed payload size
   * @param encodedBytes the encoded wire size
   */
  public void recordCompressed(int rawBytes, int encodedBytes) {
    record(rawBytes, encodedBytes, true);
  }

  /**
   * Records a packet that bypassed compression.
   *
   * @param rawBytes the uncompressed payload size
   * @param encodedBytes the encoded wire size
   */
  public void recordPassThrough(int rawBytes, int encodedBytes) {
    record(rawBytes, encodedBytes, false);
  }

  private void record(int rawBytes, int encodedBytes, boolean compressed) {
    totalPackets.increment();
    totalRawBytes.add(rawBytes);
    totalEncodedBytes.add(encodedBytes);
    if (compressed) {
      compressedPackets.increment();
      compressedRawBytes.add(rawBytes);
      compressedEncodedBytes.add(encodedBytes);
    } else {
      passthroughPackets.increment();
    }
  }

  /**
   * Clears all currently collected statistics.
   */
  public void reset() {
    totalPackets.reset();
    compressedPackets.reset();
    passthroughPackets.reset();
    totalRawBytes.reset();
    totalEncodedBytes.reset();
    compressedRawBytes.reset();
    compressedEncodedBytes.reset();
    resetNanoTime.set(System.nanoTime());
  }

  /**
   * Captures the current aggregate statistics in an immutable snapshot.
   *
   * @return the current snapshot
   */
  public Snapshot snapshot() {
    return new Snapshot(
        resetNanoTime.get(),
        totalPackets.sum(),
        compressedPackets.sum(),
        passthroughPackets.sum(),
        totalRawBytes.sum(),
        totalEncodedBytes.sum(),
        compressedRawBytes.sum(),
        compressedEncodedBytes.sum()
    );
  }

  /**
   * Immutable compression statistics snapshot.
   */
  public record Snapshot(
      long resetNanoTime,
      long totalPackets,
      long compressedPackets,
      long passthroughPackets,
      long totalRawBytes,
      long totalEncodedBytes,
      long compressedRawBytes,
      long compressedEncodedBytes
  ) {

    /**
     * Returns the number of elapsed seconds since the last reset.
     *
     * @return the elapsed seconds for this snapshot window
     */
    public long elapsedSeconds() {
      return (System.nanoTime() - resetNanoTime) / 1_000_000_000L;
    }
  }
}
