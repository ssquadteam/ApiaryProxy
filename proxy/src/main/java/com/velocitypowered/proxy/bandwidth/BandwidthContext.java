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

import com.velocitypowered.proxy.bandwidth.BandwidthStats.PacketKey;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.PlayerIdentity;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import java.util.ArrayDeque;

/**
 * Carries packet attribution metadata through the synchronous Netty codec chain.
 */
public final class BandwidthContext {

  public static final AttributeKey<InboundSample> INBOUND_SAMPLE =
      AttributeKey.valueOf("velocity-bandwidth-inbound");
  public static final AttributeKey<OutboundSample> OUTBOUND_SAMPLE =
      AttributeKey.valueOf("velocity-bandwidth-outbound");
  private static final AttributeKey<ArrayDeque<Integer>> INBOUND_WIRE_SIZES =
      AttributeKey.valueOf("velocity-bandwidth-inbound-wire-sizes");
  private static final AttributeKey<PlayerIdentity> PLAYER_IDENTITY =
      AttributeKey.valueOf("velocity-bandwidth-player-identity");

  private BandwidthContext() {
  }

  /**
   * Resolves the authenticated player currently associated with a public connection.
   *
   * @param channel the public player channel
   * @param connection the Minecraft connection
   * @return the player identity or the unattributed identity
   */
  public static PlayerIdentity playerIdentity(Channel channel, MinecraftConnection connection) {
    if (connection.getAssociation() instanceof ConnectedPlayer player) {
      final PlayerIdentity current = channel.attr(PLAYER_IDENTITY).get();
      if (current != null
          && player.getUniqueId().equals(current.uuid())
          && player.getUsername().equals(current.name())) {
        return current;
      }
      final PlayerIdentity resolved =
          new PlayerIdentity(player.getUniqueId(), player.getUsername());
      channel.attr(PLAYER_IDENTITY).set(resolved);
      return resolved;
    }
    return PlayerIdentity.UNATTRIBUTED;
  }

  /**
   * Returns whether a public inbound frame is currently scoped for classification.
   *
   * @param ctx the packet decoder context
   * @return whether inbound bandwidth collection is active
   */
  public static boolean hasInboundSample(ChannelHandlerContext ctx) {
    return ctx.channel().attr(INBOUND_SAMPLE).get() != null;
  }

  /**
   * Enables transfer of actual inbound frame sizes from the frame decoder to the stats handler.
   *
   * @param channel the public player channel
   */
  public static void enableInboundWireSizes(Channel channel) {
    channel.attr(INBOUND_WIRE_SIZES).set(new ArrayDeque<>());
  }

  /**
   * Disables and clears inbound frame size transfer for a channel.
   *
   * @param channel the public player channel
   */
  public static void disableInboundWireSizes(Channel channel) {
    channel.attr(INBOUND_WIRE_SIZES).set(null);
  }

  /**
   * Enqueues the exact frame size observed by the VarInt frame decoder.
   *
   * @param ctx the frame decoder context
   * @param wireBytes the outer length prefix plus frame payload
   */
  public static void enqueueInboundWireSize(ChannelHandlerContext ctx, int wireBytes) {
    final ArrayDeque<Integer> sizes = ctx.channel().attr(INBOUND_WIRE_SIZES).get();
    if (sizes != null) {
      sizes.offer(wireBytes);
    }
  }

  /**
   * Polls the exact frame size for the next decoded inbound frame.
   *
   * @param ctx the bandwidth handler context
   * @return the wire size, or {@code -1} when transfer is not available
   */
  public static int pollInboundWireSize(ChannelHandlerContext ctx) {
    final ArrayDeque<Integer> sizes = ctx.channel().attr(INBOUND_WIRE_SIZES).get();
    if (sizes == null) {
      return -1;
    }
    final Integer wireBytes = sizes.poll();
    return wireBytes == null ? -1 : wireBytes;
  }

  /**
   * Records the currently scoped inbound frame after its packet type has been identified.
   *
   * @param ctx the decoder context
   * @param packetKey the packet identity
   */
  public static void recordInbound(ChannelHandlerContext ctx, PacketKey packetKey) {
    final InboundSample sample = ctx.channel().attr(INBOUND_SAMPLE).get();
    if (sample != null) {
      BandwidthStats.INSTANCE.record(packetKey, sample.player(), sample.wireBytes());
    }
  }

  /**
   * Records the currently scoped outbound packet after final framing/compression.
   *
   * @param ctx the encoder context
   * @param wireBytes the final encoded Minecraft frame size
   */
  public static void recordOutbound(ChannelHandlerContext ctx, int wireBytes) {
    final Channel channel = ctx == null ? null : ctx.channel();
    if (channel == null) {
      // FastPrepare/LimboAPI invokes the codec reflectively with a synthetic context that only
      // provides an allocator. There is no player channel to attribute or count in that path.
      return;
    }
    final OutboundSample sample = channel.attr(OUTBOUND_SAMPLE).get();
    if (sample != null) {
      BandwidthStats.INSTANCE.record(sample.packetKey(), sample.player(), wireBytes);
    }
  }

  /** Scoped metadata for one inbound Minecraft frame. */
  public record InboundSample(int wireBytes, PlayerIdentity player) {
  }

  /** Scoped metadata for one outbound Minecraft packet. */
  public record OutboundSample(PacketKey packetKey, PlayerIdentity player) {
  }
}
