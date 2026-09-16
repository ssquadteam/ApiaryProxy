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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.proxy.bandwidth.BandwidthStats.DirectionFilter;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.PacketKey;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.PacketReport;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.PlayerIdentity;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.PlayerReport;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.TrafficDirection;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BandwidthStatsTest {

  private static final UUID ALICE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID BOB_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");

  private final BandwidthStats stats = BandwidthStats.INSTANCE;

  @BeforeEach
  void reset() {
    stats.reset();
  }

  @Test
  void reportsPacketsPlayersDirectionsAndUnattributedTraffic() {
    final PacketKey outbound =
        new PacketKey(TrafficDirection.OUTBOUND, "ChunkPacket");
    final PacketKey inbound =
        new PacketKey(TrafficDirection.INBOUND, "MovePacket");
    final PlayerIdentity alice = new PlayerIdentity(ALICE_UUID, "Alice");
    final PlayerIdentity bob = new PlayerIdentity(BOB_UUID, "Bob");

    stats.record(outbound, alice, 100);
    stats.record(outbound, bob, 50);
    stats.record(outbound, PlayerIdentity.UNATTRIBUTED, 25);
    stats.record(inbound, alice, 200);

    final PacketReport packets = stats.packetReport(DirectionFilter.ALL, 10, 1);
    assertEquals(375L, packets.totalBytes());
    assertEquals(4L, packets.totalPackets());
    assertEquals(25L, packets.unattributedBytes());
    assertEquals("MovePacket", packets.rows().get(0).key().displayName());
    assertEquals(200L, packets.rows().get(0).bytes());
    assertEquals("ChunkPacket", packets.rows().get(1).key().displayName());
    assertEquals(175L, packets.rows().get(1).bytes());
    assertEquals(1, packets.rows().get(1).players().size());
    assertEquals("Alice", packets.rows().get(1).players().get(0).identity().name());
    assertEquals(25L, packets.rows().get(1).unattributedBytes());

    final PlayerReport players = stats.playerReport(DirectionFilter.ALL, 10);
    assertEquals(375L, players.totalBytes());
    assertEquals(25L, players.unattributedBytes());
    assertEquals("Alice", players.rows().get(0).identity().name());
    assertEquals(300L, players.rows().get(0).bytes());
    assertEquals("Bob", players.rows().get(1).identity().name());
    assertEquals(50L, players.rows().get(1).bytes());

    final PlayerReport outboundPlayers = stats.playerReport(DirectionFilter.OUTBOUND, 10);
    assertEquals(175L, outboundPlayers.totalBytes());
    assertEquals(100L, outboundPlayers.rows().get(0).bytes());
  }

  @Test
  void reconnectUpdatesTheDisplayedNameForTheSameUuid() {
    final PacketKey packet = new PacketKey(TrafficDirection.OUTBOUND, "TestPacket");
    stats.record(packet, new PlayerIdentity(ALICE_UUID, "OldName"), 10);
    stats.record(packet, new PlayerIdentity(ALICE_UUID, "NewName"), 20);

    final PlayerReport report = stats.playerReport(DirectionFilter.ALL, 10);
    assertEquals(1, report.rows().size());
    assertEquals("NewName", report.rows().get(0).identity().name());
    assertEquals(30L, report.rows().get(0).bytes());
  }

  @Test
  void resetReplacesTheEntireStatisticsWindow() {
    stats.record(
        new PacketKey(TrafficDirection.OUTBOUND, "TestPacket"),
        new PlayerIdentity(ALICE_UUID, "Alice"),
        100
    );
    stats.reset();

    final PacketReport packets = stats.packetReport(DirectionFilter.ALL, 10, 3);
    final PlayerReport players = stats.playerReport(DirectionFilter.ALL, 10);
    assertEquals(0L, packets.totalBytes());
    assertTrue(packets.rows().isEmpty());
    assertTrue(players.rows().isEmpty());
  }
}
