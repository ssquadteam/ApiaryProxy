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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.natives.compression.JavaVelocityCompressor;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.proxy.bandwidth.BandwidthStats;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.DirectionFilter;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MinecraftBandwidthHandlerTest {

  private final BandwidthStats stats = BandwidthStats.INSTANCE;

  @BeforeEach
  void reset() {
    stats.reset();
  }

  @AfterEach
  void clear() {
    stats.reset();
  }

  @Test
  void outboundRawPacketUsesTheFinalFramedSize() {
    final MinecraftConnection connection = mock(MinecraftConnection.class);
    final MinecraftEncoder encoder = new MinecraftEncoder(ProtocolUtils.Direction.CLIENTBOUND);
    final EmbeddedChannel channel = new EmbeddedChannel(
        MinecraftVarintLengthEncoder.INSTANCE,
        encoder,
        new MinecraftBandwidthOutboundHandler(connection, encoder)
    );
    final ByteBuf rawPacket = Unpooled.buffer();
    ProtocolUtils.writeVarInt(rawPacket, 0x45);
    rawPacket.writeZero(300);
    final int expectedWireBytes =
        ProtocolUtils.varIntBytes(rawPacket.readableBytes()) + rawPacket.readableBytes();

    assertTrue(channel.writeOutbound(rawPacket));
    final ByteBuf encoded = channel.readOutbound();
    assertEquals(expectedWireBytes, encoded.readableBytes());
    encoded.release();
    channel.finishAndReleaseAll();

    final var report = stats.packetReport(DirectionFilter.OUTBOUND, 10, 3);
    assertEquals(expectedWireBytes, report.totalBytes());
    assertEquals(1L, report.totalPackets());
    assertEquals("HANDSHAKE 1.7.2 0x45", report.rows().get(0).key().displayName());
  }

  @Test
  void outboundCompressedPacketUsesTheFinalCompressedSize() {
    final MinecraftConnection connection = mock(MinecraftConnection.class);
    final MinecraftEncoder encoder = new MinecraftEncoder(ProtocolUtils.Direction.CLIENTBOUND);
    final EmbeddedChannel channel = new EmbeddedChannel(
        new MinecraftCompressorAndLengthEncoder(
            1, JavaVelocityCompressor.FACTORY.create(6), 16, false),
        encoder,
        new MinecraftBandwidthOutboundHandler(connection, encoder)
    );
    final ByteBuf rawPacket = Unpooled.buffer();
    ProtocolUtils.writeVarInt(rawPacket, 0x45);
    rawPacket.writeZero(4096);

    assertTrue(channel.writeOutbound(rawPacket));
    final ByteBuf encoded = channel.readOutbound();
    final int expectedWireBytes = encoded.readableBytes();
    assertTrue(expectedWireBytes < 4096);
    encoded.release();
    channel.finishAndReleaseAll();

    final var report = stats.packetReport(DirectionFilter.OUTBOUND, 10, 3);
    assertEquals(expectedWireBytes, report.totalBytes());
    assertEquals(1L, report.totalPackets());
  }

  @Test
  void outboundCompressionAllowsSyntheticContextWithoutChannel() throws Exception {
    final ChannelHandlerContext context = mock(ChannelHandlerContext.class);
    when(context.channel()).thenReturn(null);
    when(context.alloc()).thenReturn(UnpooledByteBufAllocator.DEFAULT);
    final ExposedCompressor encoder = new ExposedCompressor(
        1, JavaVelocityCompressor.FACTORY.create(6), 16, false);
    final ByteBuf rawPacket = Unpooled.buffer();
    ProtocolUtils.writeVarInt(rawPacket, 0x45);
    rawPacket.writeZero(4096);
    final ByteBuf encoded = Unpooled.buffer();

    assertDoesNotThrow(() -> encoder.encodeForTest(context, rawPacket, encoded));
    assertTrue(encoded.isReadable());
    assertEquals(0L, stats.packetReport(DirectionFilter.OUTBOUND, 10, 3).totalBytes());

    rawPacket.release();
    encoded.release();
  }

  @Test
  void inboundRawPacketUsesTheReceivedFrameSize() {
    final MinecraftConnection connection = mock(MinecraftConnection.class);
    final MinecraftVarintFrameDecoder frameDecoder =
        new MinecraftVarintFrameDecoder(ProtocolUtils.Direction.SERVERBOUND);
    frameDecoder.setState(StateRegistry.PLAY);
    final MinecraftDecoder decoder = new MinecraftDecoder(ProtocolUtils.Direction.SERVERBOUND);
    decoder.setState(StateRegistry.PLAY);
    decoder.setProtocolVersion(ProtocolVersion.MINECRAFT_1_21_11);
    final EmbeddedChannel channel = new EmbeddedChannel(
        frameDecoder,
        new MinecraftBandwidthInboundHandler(connection),
        decoder
    );

    final ByteBuf packet = Unpooled.buffer();
    ProtocolUtils.writeVarInt(packet, 0x1FFFFF);
    packet.writeZero(64);
    final int expectedWireBytes =
        ProtocolUtils.varIntBytes(packet.readableBytes()) + packet.readableBytes();
    final ByteBuf framed = Unpooled.buffer(expectedWireBytes);
    ProtocolUtils.writeVarInt(framed, packet.readableBytes());
    framed.writeBytes(packet);
    packet.release();

    assertTrue(channel.writeInbound(framed));
    final ByteBuf decoded = channel.readInbound();
    decoded.release();
    channel.finishAndReleaseAll();

    final var report = stats.packetReport(DirectionFilter.INBOUND, 10, 3);
    assertEquals(expectedWireBytes, report.totalBytes());
    assertEquals(1L, report.totalPackets());
    assertEquals("PLAY 1.21.11 0x1FFFFF", report.rows().get(0).key().displayName());
  }

  @Test
  void inboundCompressedPacketKeepsTheOriginalWireSizeThroughDecompression() {
    final ByteBuf packet = Unpooled.buffer();
    ProtocolUtils.writeVarInt(packet, 0x1FFFFF);
    packet.writeZero(4096);
    final EmbeddedChannel encodingChannel = new EmbeddedChannel(
        new MinecraftCompressorAndLengthEncoder(
            1, JavaVelocityCompressor.FACTORY.create(6), 16, false)
    );
    assertTrue(encodingChannel.writeOutbound(packet));
    final ByteBuf framed = encodingChannel.readOutbound();
    final int expectedWireBytes = framed.readableBytes();
    assertTrue(expectedWireBytes < 4096);

    final MinecraftConnection connection = mock(MinecraftConnection.class);
    final MinecraftVarintFrameDecoder frameDecoder =
        new MinecraftVarintFrameDecoder(ProtocolUtils.Direction.SERVERBOUND);
    frameDecoder.setState(StateRegistry.PLAY);
    final MinecraftDecoder decoder = new MinecraftDecoder(ProtocolUtils.Direction.SERVERBOUND);
    decoder.setState(StateRegistry.PLAY);
    decoder.setProtocolVersion(ProtocolVersion.MINECRAFT_1_21_11);
    final EmbeddedChannel channel = new EmbeddedChannel(
        frameDecoder,
        new MinecraftBandwidthInboundHandler(connection),
        new MinecraftCompressDecoder(
            1,
            JavaVelocityCompressor.FACTORY.create(6),
            ProtocolUtils.Direction.SERVERBOUND
        ),
        decoder
    );

    assertTrue(channel.writeInbound(framed));
    final ByteBuf decoded = channel.readInbound();
    decoded.release();
    channel.finishAndReleaseAll();
    encodingChannel.finishAndReleaseAll();

    final var report = stats.packetReport(DirectionFilter.INBOUND, 10, 3);
    assertEquals(expectedWireBytes, report.totalBytes());
    assertEquals(1L, report.totalPackets());
  }

  private static final class ExposedCompressor extends MinecraftCompressorAndLengthEncoder {

    private ExposedCompressor(int threshold, VelocityCompressor compressor,
        int compressBoundHeadroom, boolean statsEnabled) {
      super(threshold, compressor, compressBoundHeadroom, statsEnabled);
    }

    private void encodeForTest(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out)
        throws Exception {
      encode(ctx, msg, out);
    }
  }
}
