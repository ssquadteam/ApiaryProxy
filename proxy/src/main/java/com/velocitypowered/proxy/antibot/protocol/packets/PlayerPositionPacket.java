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
 * Represents a player position packet for the gravity check.
 */
public class PlayerPositionPacket implements AntibotPacket {

  private final double posX;
  private final double posY;
  private final double posZ;
  private final boolean onGround;
  private final boolean hasRotation;

  /**
   * Creates a new player position packet.
   *
   * @param posX the x position
   * @param posY the y position
   * @param posZ the z position
   * @param onGround whether the player is on the ground
   * @param hasRotation whether the packet includes rotation data
   */
  public PlayerPositionPacket(double posX, double posY, double posZ, boolean onGround, boolean hasRotation) {
    this.posX = posX;
    this.posY = posY;
    this.posZ = posZ;
    this.onGround = onGround;
    this.hasRotation = hasRotation;
  }

  /**
   * Gets the x position.
   *
   * @return the x position
   */
  public double getX() {
    return posX;
  }

  /**
   * Gets the y position.
   *
   * @return the y position
   */
  public double getY() {
    return posY;
  }

  /**
   * Gets the z position.
   *
   * @return the z position
   */
  public double getZ() {
    return posZ;
  }

  /**
   * Checks if the player is on the ground.
   *
   * @return true if on ground, false otherwise
   */
  public boolean isOnGround() {
    return onGround;
  }

  /**
   * Checks if this packet includes rotation data.
   *
   * @return true if rotation data is included, false otherwise
   */
  public boolean hasRotation() {
    return hasRotation;
  }

  @Override
  public void encode(ByteBuf buf) {
    buf.writeDouble(posX);
    buf.writeDouble(posY);
    buf.writeDouble(posZ);
    buf.writeBoolean(onGround);
  }

  @Override
  public int getId() {
    return hasRotation ? 0x11 : 0x10; // Position with rotation vs position only
  }
} 