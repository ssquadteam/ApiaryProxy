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

package com.velocitypowered.proxy.antibot.protocol.packets;

import com.velocitypowered.proxy.antibot.protocol.AntibotPacket;
import io.netty.buffer.ByteBuf;

/**
 * Represents a block update packet.
 */
public class BlockUpdatePacket implements AntibotPacket {

  private final int posX;
  private final int posY;
  private final int posZ;
  private final int blockId;

  /**
   * Creates a new block update packet.
   *
   * @param posX the x position
   * @param posY the y position
   * @param posZ the z position
   * @param blockId the block ID
   */
  public BlockUpdatePacket(int posX, int posY, int posZ, int blockId) {
    this.posX = posX;
    this.posY = posY;
    this.posZ = posZ;
    this.blockId = blockId;
  }

  /**
   * Gets the X position.
   *
   * @return the X position
   */
  public int getPosX() {
    return posX;
  }

  /**
   * Gets the Y position.
   *
   * @return the Y position
   */
  public int getPosY() {
    return posY;
  }

  /**
   * Gets the Z position.
   *
   * @return the Z position
   */
  public int getPosZ() {
    return posZ;
  }

  /**
   * Gets the block ID.
   *
   * @return the block ID
   */
  public int getBlockId() {
    return blockId;
  }

  @Override
  public void encode(ByteBuf buf) {
    // Write block position
    long position = ((posX & 0x3FFFFFFL) << 38) | ((posY & 0xFFFL) << 26) | (posZ & 0x3FFFFFFL);
    buf.writeLong(position);
    
    // Write block state ID - use standard int instead of VarInt
    buf.writeInt(blockId);
  }

  @Override
  public int getId() {
    return 0x09; // Block Update packet ID (varies by version)
  }
} 