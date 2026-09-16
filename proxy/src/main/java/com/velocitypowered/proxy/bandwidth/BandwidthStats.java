/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocitypowered.proxy.bandwidth;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.StateRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.ToLongFunction;
import org.jspecify.annotations.Nullable;

/**
 * Tracks Minecraft wire bytes on the public player-facing connections.
 */
public final class BandwidthStats {

  public static final BandwidthStats INSTANCE = new BandwidthStats();
  private static final Object UNATTRIBUTED_KEY = new Object();

  private final AtomicReference<Accumulator> current =
      new AtomicReference<>(new Accumulator(System.nanoTime()));

  private BandwidthStats() {
  }

  /**
   * Records one successfully classified Minecraft frame.
   *
   * @param packetKey the packet identity
   * @param player the player identity, or the unattributed identity
   * @param wireBytes the encoded Minecraft frame size
   */
  public void record(PacketKey packetKey, PlayerIdentity player, int wireBytes) {
    if (wireBytes <= 0) {
      return;
    }

    final Accumulator accumulator = current.get();
    accumulator.total.add(packetKey.direction(), wireBytes);
    accumulator.packets.computeIfAbsent(packetKey, ignored -> new Counter()).add(wireBytes);

    final Object playerKey = player.unattributed() ? UNATTRIBUTED_KEY : player.uuid();
    final PlayerAccumulator playerAccumulator = accumulator.players.computeIfAbsent(
        playerKey, ignored -> new PlayerAccumulator(player.uuid(), player.name()));
    playerAccumulator.updateName(player.name());
    playerAccumulator.total.add(packetKey.direction(), wireBytes);
    playerAccumulator.packets.computeIfAbsent(packetKey, ignored -> new Counter()).add(wireBytes);
  }

  /**
   * Atomically starts a fresh statistics window.
   */
  public void reset() {
    current.set(new Accumulator(System.nanoTime()));
  }

  /**
   * Builds the packet leaderboard for the requested direction.
   *
   * @param direction the direction filter
   * @param packetLimit maximum number of packet rows
   * @param playerLimit maximum number of player contributors per packet
   * @return the packet report
   */
  public PacketReport packetReport(DirectionFilter direction, int packetLimit, int playerLimit) {
    final Accumulator accumulator = current.get();
    final long totalBytes = accumulator.total.bytes(direction);
    final long totalPackets = accumulator.total.packets(direction);
    final List<PacketCandidate> packets = new ArrayList<>();

    for (Map.Entry<PacketKey, Counter> entry : accumulator.packets.entrySet()) {
      if (!direction.includes(entry.getKey().direction())) {
        continue;
      }
      final long bytes = entry.getValue().bytes();
      if (bytes > 0L) {
        packets.add(new PacketCandidate(entry.getKey(), bytes, entry.getValue().packets()));
      }
    }

    final List<PacketCandidate> topPackets = topN(
        packets,
        packetLimit,
        PacketCandidate::bytes,
        Comparator.comparing(candidate -> candidate.key().displayName())
    );
    final List<PacketRow> rows = new ArrayList<>(topPackets.size());

    for (PacketCandidate packet : topPackets) {
      final List<PlayerCandidate> players = new ArrayList<>();
      long unattributedBytes = 0L;
      long unattributedPackets = 0L;

      for (Map.Entry<Object, PlayerAccumulator> playerEntry
          : accumulator.players.entrySet()) {
        final Counter counter = playerEntry.getValue().packets.get(packet.key());
        if (counter == null || counter.bytes() <= 0L) {
          continue;
        }
        if (playerEntry.getKey() == UNATTRIBUTED_KEY) {
          unattributedBytes = counter.bytes();
          unattributedPackets = counter.packets();
        } else {
          players.add(new PlayerCandidate(
              playerEntry.getValue().identity(),
              counter.bytes(),
              counter.packets()
          ));
        }
      }

      final List<PlayerCandidate> topPlayers = topN(
          players,
          playerLimit,
          PlayerCandidate::bytes,
          Comparator.comparing(candidate -> candidate.identity().name(),
              String.CASE_INSENSITIVE_ORDER)
      );
      final List<PlayerRow> playerRows = topPlayers.stream()
          .map(candidate -> new PlayerRow(
              candidate.identity(), candidate.bytes(), candidate.packets()))
          .toList();
      rows.add(new PacketRow(
          packet.key(), packet.bytes(), packet.packets(), playerRows,
          unattributedBytes, unattributedPackets
      ));
    }

    return new PacketReport(
        accumulator.resetNanoTime, totalBytes, totalPackets,
        accumulator.unattributedBytes(direction), List.copyOf(rows)
    );
  }

  /**
   * Builds the player leaderboard for the requested direction.
   *
   * @param direction the direction filter
   * @param playerLimit maximum number of player rows
   * @return the player report
   */
  public PlayerReport playerReport(DirectionFilter direction, int playerLimit) {
    final Accumulator accumulator = current.get();
    final List<PlayerCandidate> players = new ArrayList<>();

    for (Map.Entry<Object, PlayerAccumulator> entry : accumulator.players.entrySet()) {
      if (entry.getKey() == UNATTRIBUTED_KEY) {
        continue;
      }
      final long bytes = entry.getValue().total.bytes(direction);
      if (bytes > 0L) {
        players.add(new PlayerCandidate(
            entry.getValue().identity(),
            bytes,
            entry.getValue().total.packets(direction)
        ));
      }
    }

    final List<PlayerRow> rows = topN(
        players,
        playerLimit,
        PlayerCandidate::bytes,
        Comparator.comparing(candidate -> candidate.identity().name(),
            String.CASE_INSENSITIVE_ORDER)
    ).stream().map(candidate -> new PlayerRow(
        candidate.identity(), candidate.bytes(), candidate.packets())).toList();

    return new PlayerReport(
        accumulator.resetNanoTime,
        accumulator.total.bytes(direction),
        accumulator.total.packets(direction),
        accumulator.unattributedBytes(direction),
        rows
    );
  }

  private static <T> List<T> topN(List<T> candidates, int limit,
      ToLongFunction<T> score, Comparator<T> tieBreaker) {
    final Comparator<T> bestFirst = Comparator
        .comparingLong(score).reversed()
        .thenComparing(tieBreaker);
    if (candidates.size() <= limit) {
      candidates.sort(bestFirst);
      return List.copyOf(candidates);
    }

    final PriorityQueue<T> heap = new PriorityQueue<>(limit, bestFirst.reversed());
    for (T candidate : candidates) {
      if (heap.size() < limit) {
        heap.offer(candidate);
      } else if (bestFirst.compare(candidate, heap.peek()) < 0) {
        heap.poll();
        heap.offer(candidate);
      }
    }

    final List<T> result = new ArrayList<>(heap);
    result.sort(bestFirst);
    return List.copyOf(result);
  }

  /** Public traffic direction relative to the proxy. */
  public enum TrafficDirection {
    OUTBOUND,
    INBOUND
  }

  /** Direction filter used by the command reports. */
  public enum DirectionFilter {
    ALL,
    OUTBOUND,
    INBOUND;

    /**
     * Returns whether this filter includes the supplied direction.
     *
     * @param direction the traffic direction
     * @return whether it is included
     */
    public boolean includes(TrafficDirection direction) {
      return this == ALL || name().equals(direction.name());
    }

    /**
     * Parses a command direction literal.
     *
     * @param value the literal
     * @return the parsed filter
     */
    public static DirectionFilter parse(String value) {
      return valueOf(value.toUpperCase(Locale.ROOT));
    }
  }

  /** Packet identity used as an aggregation key. */
  public record PacketKey(TrafficDirection direction, String displayName) {

    private static final ClassValue<PacketKey> OUTBOUND_KEYS = new ClassValue<>() {
      @Override
      protected PacketKey computeValue(Class<?> type) {
        return new PacketKey(TrafficDirection.OUTBOUND, type.getSimpleName());
      }
    };
    private static final ClassValue<PacketKey> INBOUND_KEYS = new ClassValue<>() {
      @Override
      protected PacketKey computeValue(Class<?> type) {
        return new PacketKey(TrafficDirection.INBOUND, type.getSimpleName());
      }
    };

    /**
     * Creates a key for a packet type decoded by Velocity.
     *
     * @param direction the public traffic direction
     * @param packetType the packet class
     * @return the packet key
     */
    public static PacketKey known(TrafficDirection direction, Class<?> packetType) {
      return direction == TrafficDirection.OUTBOUND
          ? OUTBOUND_KEYS.get(packetType) : INBOUND_KEYS.get(packetType);
    }

    /**
     * Creates a key for a pass-through packet identified only by protocol ID.
     *
     * @param direction the public traffic direction
     * @param state the protocol state
     * @param version the client protocol version
     * @param packetId the protocol packet ID
     * @return the packet key
     */
    public static PacketKey unknown(TrafficDirection direction, StateRegistry state,
        ProtocolVersion version, int packetId) {
      return new PacketKey(direction, "%s %s 0x%02X".formatted(
          state.name(), version.getVersionIntroducedIn(), packetId));
    }
  }

  /** Player identity retained by the statistics window. */
  public record PlayerIdentity(@Nullable UUID uuid, String name) {

    public static final PlayerIdentity UNATTRIBUTED =
        new PlayerIdentity(null, "Unattributed");

    /**
     * Returns whether traffic could not be attributed to an authenticated player.
     *
     * @return whether this is the unattributed identity
     */
    public boolean unattributed() {
      return uuid == null;
    }
  }

  /** One packet leaderboard row. */
  public record PacketRow(
      PacketKey key,
      long bytes,
      long packets,
      List<PlayerRow> players,
      long unattributedBytes,
      long unattributedPackets
  ) {
  }

  /** One player leaderboard or packet-contributor row. */
  public record PlayerRow(PlayerIdentity identity, long bytes, long packets) {
  }

  /** Packet leaderboard snapshot. */
  public record PacketReport(
      long resetNanoTime,
      long totalBytes,
      long totalPackets,
      long unattributedBytes,
      List<PacketRow> rows
  ) {

    public long elapsedSeconds() {
      return Math.max(0L, (System.nanoTime() - resetNanoTime) / 1_000_000_000L);
    }
  }

  /** Player leaderboard snapshot. */
  public record PlayerReport(
      long resetNanoTime,
      long totalBytes,
      long totalPackets,
      long unattributedBytes,
      List<PlayerRow> rows
  ) {

    public long elapsedSeconds() {
      return Math.max(0L, (System.nanoTime() - resetNanoTime) / 1_000_000_000L);
    }
  }

  private record PacketCandidate(PacketKey key, long bytes, long packets) {
  }

  private record PlayerCandidate(PlayerIdentity identity, long bytes, long packets) {
  }

  private static final class Counter {
    private final LongAdder bytes = new LongAdder();
    private final LongAdder packets = new LongAdder();

    private void add(int wireBytes) {
      bytes.add(wireBytes);
      packets.increment();
    }

    private long bytes() {
      return bytes.sum();
    }

    private long packets() {
      return packets.sum();
    }
  }

  private static final class DirectionalCounter {
    private final Counter outbound = new Counter();
    private final Counter inbound = new Counter();

    private void add(TrafficDirection direction, int wireBytes) {
      counter(direction).add(wireBytes);
    }

    private long bytes(DirectionFilter direction) {
      return switch (direction) {
        case ALL -> outbound.bytes() + inbound.bytes();
        case OUTBOUND -> outbound.bytes();
        case INBOUND -> inbound.bytes();
      };
    }

    private long packets(DirectionFilter direction) {
      return switch (direction) {
        case ALL -> outbound.packets() + inbound.packets();
        case OUTBOUND -> outbound.packets();
        case INBOUND -> inbound.packets();
      };
    }

    private Counter counter(TrafficDirection direction) {
      return direction == TrafficDirection.OUTBOUND ? outbound : inbound;
    }
  }

  private static final class PlayerAccumulator {
    private final @Nullable UUID uuid;
    private final AtomicReference<String> name;
    private final DirectionalCounter total = new DirectionalCounter();
    private final ConcurrentMap<PacketKey, Counter> packets = new ConcurrentHashMap<>();

    private PlayerAccumulator(@Nullable UUID uuid, String name) {
      this.uuid = uuid;
      this.name = new AtomicReference<>(name);
    }

    private void updateName(String newName) {
      if (!newName.equals(name.get())) {
        name.set(newName);
      }
    }

    private PlayerIdentity identity() {
      return new PlayerIdentity(uuid, name.get());
    }
  }

  private static final class Accumulator {
    private final long resetNanoTime;
    private final DirectionalCounter total = new DirectionalCounter();
    private final ConcurrentMap<PacketKey, Counter> packets = new ConcurrentHashMap<>();
    private final ConcurrentMap<Object, PlayerAccumulator> players = new ConcurrentHashMap<>();

    private Accumulator(long resetNanoTime) {
      this.resetNanoTime = resetNanoTime;
    }

    private long unattributedBytes(DirectionFilter direction) {
      final PlayerAccumulator unattributed = players.get(UNATTRIBUTED_KEY);
      return unattributed == null ? 0L : unattributed.total.bytes(direction);
    }
  }
}
