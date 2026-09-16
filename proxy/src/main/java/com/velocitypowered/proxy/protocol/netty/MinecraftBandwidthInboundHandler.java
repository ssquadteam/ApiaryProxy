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

import com.velocitypowered.proxy.bandwidth.BandwidthContext;
import com.velocitypowered.proxy.bandwidth.BandwidthContext.InboundSample;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Scopes the public wire size and player identity around inbound packet decoding.
 */
public final class MinecraftBandwidthInboundHandler extends ChannelInboundHandlerAdapter {

  private final MinecraftConnection connection;

  public MinecraftBandwidthInboundHandler(MinecraftConnection connection) {
    this.connection = connection;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    BandwidthContext.enableInboundWireSizes(ctx.channel());
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    BandwidthContext.disableInboundWireSizes(ctx.channel());
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (!(msg instanceof ByteBuf frame)) {
      ctx.fireChannelRead(msg);
      return;
    }

    final int frameBytes = frame.readableBytes();
    final int observedWireBytes = BandwidthContext.pollInboundWireSize(ctx);
    final InboundSample previous = ctx.channel()
        .attr(BandwidthContext.INBOUND_SAMPLE)
        .getAndSet(new InboundSample(
            observedWireBytes >= 0
                ? observedWireBytes : ProtocolUtils.varIntBytes(frameBytes) + frameBytes,
            BandwidthContext.playerIdentity(ctx.channel(), connection)
        ));
    try {
      ctx.fireChannelRead(msg);
    } finally {
      ctx.channel().attr(BandwidthContext.INBOUND_SAMPLE).set(previous);
    }
  }
}
