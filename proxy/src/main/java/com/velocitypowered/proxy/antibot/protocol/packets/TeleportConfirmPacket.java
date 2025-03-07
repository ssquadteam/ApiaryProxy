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
 * Represents a teleport confirmation packet.
 */
public class TeleportConfirmPacket implements AntibotPacket {

  private final int teleportId;

  /**
   * Creates a new teleport confirmation packet.
   *
   * @param teleportId the teleport ID
   */
  public TeleportConfirmPacket(int teleportId) {
    this.teleportId = teleportId;
  }

  /**
   * Gets the teleport ID.
   *
   * @return the teleport ID
   */
  public int getTeleportId() {
    return teleportId;
  }

  @Override
  public void encode(ByteBuf buf) {
    buf.writeInt(teleportId);
  }

  @Override
  public int getId() {
    return 0x00; // Modern teleport confirm packet ID
  }
} 