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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.buffer.ByteBuf;

/**
 * Clientbound {@code level_chunk_with_light}. Everything after the chunk coordinates (heightmaps,
 * section data, block entities and light) is opaque to the proxy and carried over verbatim.
 */
public class ChunkDataPacket extends DeferredByteBufHolder implements MinecraftPacket {

  private int chunkX;

  private int chunkZ;

  public ChunkDataPacket() {
    super(null);
  }

  public ChunkDataPacket(int chunkX, int chunkZ, ByteBuf data) {
    super(data);
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;
  }

  public int getChunkX() {
    return chunkX;
  }

  public void setChunkX(int chunkX) {
    this.chunkX = chunkX;
  }

  public int getChunkZ() {
    return chunkZ;
  }

  public void setChunkZ(int chunkZ) {
    this.chunkZ = chunkZ;
  }

  @Override
  public String toString() {
    return "ChunkData{"
        + "chunkX=" + chunkX
        + ", chunkZ=" + chunkZ
        + ", data=" + super.toString()
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.chunkX = buf.readInt();
    this.chunkZ = buf.readInt();
    this.replace(buf.readRetainedSlice(buf.readableBytes()));
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    buf.writeInt(chunkX);
    buf.writeInt(chunkZ);
    buf.writeBytes(content());
  }

  @Override
  public int decodeExpectedMinLength(ByteBuf buf, ProtocolUtils.Direction direction,
                                     ProtocolVersion version) {
    return Integer.BYTES * 2;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
