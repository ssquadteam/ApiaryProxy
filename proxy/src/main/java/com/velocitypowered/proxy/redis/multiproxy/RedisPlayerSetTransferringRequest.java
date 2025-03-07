/*
 * Copyright (C) 2024 Velocity Contributors
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

package com.velocitypowered.proxy.redis.multiproxy;

import com.velocitypowered.proxy.redis.RedisPacket;
import java.util.UUID;

/**
 * Constructs a packet that toggles the currently transferring mode for the
 * {@link com.velocitypowered.proxy.redis.multiproxy.RemotePlayerInfo} instance.
 *
 * @param uuid The UUID of the player.
 * @param transferring Whether they're being transferred or not.
 * @param currentlyConnectedServer The server they're currently connected to.
 */
public record RedisPlayerSetTransferringRequest(UUID uuid, boolean transferring, String currentlyConnectedServer) implements RedisPacket {
  public static final String ID = "set-transfer-request";

  @Override
  public String getId() {
    return ID;
  }
}
