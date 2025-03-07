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

import java.util.Random;

/**
 * Constants for the packet-based verification system.
 */
public final class VerificationConstants {

  private static final Random RANDOM = new Random();

  // Entity IDs
  public static final int PLAYER_ENTITY_ID = RANDOM.nextInt(1000) + 1;
  public static final int BOAT_ENTITY_ID = RANDOM.nextInt(1000) + 1001;
  public static final int MINECART_ENTITY_ID = RANDOM.nextInt(1000) + 2001;

  // Position constants
  public static final int BLOCKS_PER_ROW = 8; // 8x8 platform
  public static final int SPAWN_X_POSITION = 16 / 2; // middle of chunk
  public static final int SPAWN_Z_POSITION = 16 / 2; // middle of chunk
  public static final int PLATFORM_Y_POSITION = 1 + RANDOM.nextInt(100); // Random height
  public static final int IN_AIR_Y_POSITION = PLATFORM_Y_POSITION + 10; // 10 blocks above platform

  // Teleport IDs
  public static final int FIRST_TELEPORT_ID = 1 + RANDOM.nextInt(1000);
  public static final int SECOND_TELEPORT_ID = FIRST_TELEPORT_ID + 1;

  // Block constants
  public static final BlockType[] POSSIBLE_BLOCK_TYPES = {
      BlockType.STONE,
      BlockType.GRASS_BLOCK,
      BlockType.BEDROCK,
      BlockType.COBBLESTONE,
      BlockType.OAK_PLANKS
  };

  private VerificationConstants() {
    // Prevent instantiation
  }
} 