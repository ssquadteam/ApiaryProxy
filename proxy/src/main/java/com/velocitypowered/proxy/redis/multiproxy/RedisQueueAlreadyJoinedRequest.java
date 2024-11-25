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
 * Represents a packet that will send a warning message to a player in case they've already.
 * joined a queue they're intending to join
 *
 * @param uuid The UUID of the player.
 * @param serverName The name of the server.
 */
public record RedisQueueAlreadyJoinedRequest(UUID uuid, String serverName) implements RedisPacket {
  public static final String ID = "redis-queue-already-joined";

  @Override
  public String getId() {
    return ID;
  }
}
