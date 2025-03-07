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
 * Represents a Minecraft block type.
 */
public enum BlockType {
  STONE(1, 0, 1.0),
  GRASS_BLOCK(2, 0, 1.0),
  DIRT(3, 0, 1.0),
  COBBLESTONE(4, 0, 1.0),
  OAK_PLANKS(5, 0, 1.0),
  BEDROCK(7, 0, 1.0),
  SAND(12, 0, 1.0),
  GRAVEL(13, 0, 1.0),
  GOLD_ORE(14, 0, 1.0),
  IRON_ORE(15, 0, 1.0),
  COAL_ORE(16, 0, 1.0),
  OAK_LOG(17, 0, 1.0),
  SANDSTONE(24, 0, 1.0);
  
  private final int id;
  private final int data;
  private final double height;
  
  /**
   * Creates a new block type.
   *
   * @param id the block ID
   * @param data the block data value
   * @param height the block height
   */
  BlockType(int id, int data, double height) {
    this.id = id;
    this.data = data;
    this.height = height;
  }
  
  /**
   * Gets the block ID.
   *
   * @return the block ID
   */
  public int getId() {
    return id;
  }
  
  /**
   * Gets the block data value.
   *
   * @return the block data value
   */
  public int getData() {
    return data;
  }
  
  /**
   * Gets the block height.
   *
   * @return the block height
   */
  public double getHeight() {
    return height;
  }
} 