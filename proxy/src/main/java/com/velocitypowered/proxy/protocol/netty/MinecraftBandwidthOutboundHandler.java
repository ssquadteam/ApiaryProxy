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

package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.bandwidth.BandwidthContext;
import com.velocitypowered.proxy.bandwidth.BandwidthContext.OutboundSample;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.PacketKey;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.TrafficDirection;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

/**
 * Scopes outbound packet identity around Minecraft encoding and final framing.
 */
public final class MinecraftBandwidthOutboundHandler extends ChannelOutboundHandlerAdapter {

  private final MinecraftConnection connection;
  private final MinecraftEncoder encoder;
  private final IntObjectMap<PacketKey> unknownPacketKeys = new IntObjectHashMap<>();
  private StateRegistry unknownKeyState = StateRegistry.HANDSHAKE;
  private ProtocolVersion unknownKeyVersion = ProtocolVersion.MINIMUM_VERSION;

  public MinecraftBandwidthOutboundHandler(MinecraftConnection connection,
      MinecraftEncoder encoder) {
    this.connection = connection;
    this.encoder = encoder;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    final PacketKey packetKey = packetKey(msg);
    if (packetKey == null) {
      ctx.write(msg, promise);
      return;
    }

    final OutboundSample previous = ctx.channel()
        .attr(BandwidthContext.OUTBOUND_SAMPLE)
        .getAndSet(new OutboundSample(
            packetKey,
            BandwidthContext.playerIdentity(ctx.channel(), connection)
        ));
    try {
      ctx.write(msg, promise);
    } finally {
      ctx.channel().attr(BandwidthContext.OUTBOUND_SAMPLE).set(previous);
    }
  }

  private PacketKey packetKey(Object msg) {
    if (msg instanceof MinecraftPacket packet) {
      return PacketKey.known(TrafficDirection.OUTBOUND, packet.getClass());
    }
    if (msg instanceof ByteBuf rawPacket) {
      try {
        final int packetId = ProtocolUtils.readVarInt(rawPacket.duplicate());
        final StateRegistry state = encoder.getState();
        final ProtocolVersion version = encoder.getProtocolVersion();
        if (state != unknownKeyState || version != unknownKeyVersion) {
          unknownPacketKeys.clear();
          unknownKeyState = state;
          unknownKeyVersion = version;
        }
        PacketKey packetKey = unknownPacketKeys.get(packetId);
        if (packetKey == null) {
          packetKey = PacketKey.unknown(
              TrafficDirection.OUTBOUND, state, version, packetId);
          unknownPacketKeys.put(packetId, packetKey);
        }
        return packetKey;
      } catch (RuntimeException ignored) {
        return null;
      }
    }
    return null;
  }
}
