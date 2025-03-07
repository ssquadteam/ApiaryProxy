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

package com.velocitypowered.proxy.antibot.protocol;

import io.netty.buffer.ByteBuf;

/**
 * Base interface for antibot packet definitions.
 * These packets are used to perform more sophisticated verification.
 */
public interface AntibotPacket {

  /**
   * Encodes this packet to a byte buffer.
   *
   * @param buf the buffer to encode to
   */
  void encode(ByteBuf buf);
  
  /**
   * Gets the packet ID.
   *
   * @return the packet ID
   */
  int getId();
} 