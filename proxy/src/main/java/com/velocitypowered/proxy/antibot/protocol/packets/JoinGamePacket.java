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
 * Represents a join game packet with minimal information needed for verification.
 */
public class JoinGamePacket implements AntibotPacket {

  private final int entityId;
  private final int gamemode;

  /**
   * Creates a new join game packet.
   *
   * @param entityId the player entity ID
   * @param gamemode the gamemode
   */
  public JoinGamePacket(int entityId, int gamemode) {
    this.entityId = entityId;
    this.gamemode = gamemode;
  }

  /**
   * Gets the entity ID.
   *
   * @return the entity ID
   */
  public int getEntityId() {
    return entityId;
  }

  /**
   * Gets the gamemode.
   *
   * @return the gamemode
   */
  public int getGamemode() {
    return gamemode;
  }

  @Override
  public void encode(ByteBuf buf) {
    buf.writeInt(entityId);
    buf.writeByte(gamemode & 0xFF);
    // Simplified - in real implementation, this would have more fields
    // depending on protocol version
  }

  @Override
  public int getId() {
    return 0x26; // Join Game packet ID (varies by version)
  }
} 