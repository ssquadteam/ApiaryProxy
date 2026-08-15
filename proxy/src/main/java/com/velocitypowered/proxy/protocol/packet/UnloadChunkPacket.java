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
import io.netty.buffer.ByteBuf;

/**
 * Clientbound {@code forget_level_chunk}. Note the wire order is inverted (Z first): the client
 * reads the packet as one big-endian long, with Z in the upper 32 bits.
 */
public class UnloadChunkPacket implements MinecraftPacket {

  private int chunkX;

  private int chunkZ;

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
    return "UnloadChunk{"
        + "chunkX=" + chunkX
        + ", chunkZ=" + chunkZ
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.chunkZ = buf.readInt();
    this.chunkX = buf.readInt();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    buf.writeInt(chunkZ);
    buf.writeInt(chunkX);
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
