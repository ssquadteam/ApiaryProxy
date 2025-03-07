/*
 * Copyright (C) 2023-2024 Velocity Contributors
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

/**
 * Represents a player position packet.
 */
public class PlayerPositionPacket {
  private final double posX;
  private final double posY;
  private final double posZ;
  private final float yaw;
  private final float pitch;
  private final boolean onGround;
  private final boolean hasRotation;
  
  /**
   * Creates a new player position packet.
   *
   * @param posX the x position
   * @param posY the y position
   * @param posZ the z position
   * @param yaw the yaw rotation
   * @param pitch the pitch rotation
   * @param onGround whether the player is on the ground
   * @param hasRotation whether the packet includes rotation data
   */
  public PlayerPositionPacket(double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, boolean hasRotation) {
    this.posX = posX;
    this.posY = posY;
    this.posZ = posZ;
    this.yaw = yaw;
    this.pitch = pitch;
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
   * Gets the yaw rotation.
   *
   * @return the yaw rotation
   */
  public float getYaw() {
    return yaw;
  }
  
  /**
   * Gets the pitch rotation.
   *
   * @return the pitch rotation
   */
  public float getPitch() {
    return pitch;
  }
  
  /**
   * Checks if the player is on the ground.
   *
   * @return true if the player is on the ground, false otherwise
   */
  public boolean isOnGround() {
    return onGround;
  }
  
  /**
   * Checks if the packet includes rotation data.
   *
   * @return true if the packet includes rotation data, false otherwise
   */
  public boolean hasRotation() {
    return hasRotation;
  }
} 