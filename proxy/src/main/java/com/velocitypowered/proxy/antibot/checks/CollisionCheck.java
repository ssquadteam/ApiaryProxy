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

package com.velocitypowered.proxy.antibot.checks;

import com.velocitypowered.proxy.antibot.ApiaryAntibot;
import com.velocitypowered.proxy.antibot.protocol.BlockType;
import com.velocitypowered.proxy.antibot.protocol.PacketPreparer;
import com.velocitypowered.proxy.antibot.protocol.PlayerPositionPacket;
import com.velocitypowered.proxy.antibot.verification.VerificationSession;

/**
 * Verifies that a player collides with blocks correctly.
 */
public class CollisionCheck extends CheckBase {
  // Data keys for session storage
  private static final String DATA_KEY_TELEPORTED = "collision.teleported";
  private static final String DATA_KEY_PLATFORM_Y = "collision.platformY";
  private static final String DATA_KEY_BLOCK_TYPE = "collision.blockType";
  private static final String DATA_KEY_LAST_Y = "collision.lastY";
  private static final String DATA_KEY_ON_GROUND = "collision.onGround";
  
  /**
   * Creates a new collision check.
   *
   * @param antiBot the antibot instance
   */
  public CollisionCheck(ApiaryAntibot antiBot) {
    super(antiBot);
  }
  
  @Override
  public void initialize(VerificationSession session) {
    // Initialize session data
    session.setData(DATA_KEY_TELEPORTED, false);
    
    // Set the platform Y position (64 is a common ground level)
    int platformY = 64;
    session.setData(DATA_KEY_PLATFORM_Y, platformY);
    
    // Select a random block type for the platform
    BlockType blockType = PacketPreparer.getRandomPlatformBlock();
    session.setData(DATA_KEY_BLOCK_TYPE, blockType);
    
    // Set initial Y position (5 blocks above platform)
    double initialY = platformY + 5.0;
    session.setData(DATA_KEY_LAST_Y, initialY);
    session.setData(DATA_KEY_ON_GROUND, false);
    
    // Send join game packet
    session.sendPacket(PacketPreparer.getJoinGamePacket());
    
    // Send spawn position packet at platform coordinates
    session.sendPacket(PacketPreparer.getSpawnPositionPacket(0, platformY, 0));
    
    // Teleport player to starting position
    session.sendPacket(PacketPreparer.getFallStartPositionPacket(0.0, initialY, 0.0));
    
    // Create platform blocks
    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        session.sendPacket(PacketPreparer.createPlatformBlock(x, platformY, z, blockType));
      }
    }
  }
  
  /**
   * Processes a player position packet.
   *
   * @param session the verification session
   * @param packet the position packet
   * @return true if the position is valid, false otherwise
   */
  public boolean processPosition(VerificationSession session, PlayerPositionPacket packet) {
    // Check if teleport was confirmed
    Boolean teleported = (Boolean) session.getData(DATA_KEY_TELEPORTED);
    if (teleported == null || !teleported) {
      // First position packet confirms teleport
      session.setData(DATA_KEY_TELEPORTED, true);
      return true;
    }
    
    // Get platform Y position and block type
    Integer platformY = (Integer) session.getData(DATA_KEY_PLATFORM_Y);
    BlockType blockType = (BlockType) session.getData(DATA_KEY_BLOCK_TYPE);
    
    if (platformY == null || blockType == null) {
      return true;
    }
    
    // Get last Y position and on-ground state
    Double lastY = (Double) session.getData(DATA_KEY_LAST_Y);
    Boolean onGround = (Boolean) session.getData(DATA_KEY_ON_GROUND);
    
    if (lastY == null || onGround == null) {
      // Initialize if not set
      session.setData(DATA_KEY_LAST_Y, packet.getY());
      session.setData(DATA_KEY_ON_GROUND, packet.isOnGround());
      return true;
    }
    
    // Update last position and on-ground state
    session.setData(DATA_KEY_LAST_Y, packet.getY());
    session.setData(DATA_KEY_ON_GROUND, packet.isOnGround());
    
    // If player is on ground, check if they're at the platform
    if (packet.isOnGround()) {
      // Calculate expected Y position
      double expectedY = platformY + 1.0; // Player position is at feet level
      
      // Allow small tolerance for position differences
      if (Math.abs(packet.getY() - expectedY) > 0.1) {
        logger.debug("Player {} failed collision check: wrong Y position: expected {}, got {}", 
            session.getUsername(), expectedY, packet.getY());
        return false;
      }
    } else if (packet.getY() < platformY) {
      // Player is below the platform but not on ground
      logger.debug("Player {} failed collision check: below platform but not on ground: Y={}", 
          session.getUsername(), packet.getY());
      return false;
    }
    
    return true;
  }
  
  @Override
  public boolean onVerify(VerificationSession session) {
    // Check if player is on ground
    Boolean onGround = (Boolean) session.getData(DATA_KEY_ON_GROUND);
    if (onGround == null || !onGround) {
      logger.debug("Player {} failed collision check: not on ground", 
          session.getUsername());
      return false;
    }
    
    return true;
  }
  
  @Override
  public void reset(VerificationSession session) {
    // Clear all collision-related data
    session.removeData(DATA_KEY_TELEPORTED);
    session.removeData(DATA_KEY_PLATFORM_Y);
    session.removeData(DATA_KEY_BLOCK_TYPE);
    session.removeData(DATA_KEY_LAST_Y);
    session.removeData(DATA_KEY_ON_GROUND);
  }
} 