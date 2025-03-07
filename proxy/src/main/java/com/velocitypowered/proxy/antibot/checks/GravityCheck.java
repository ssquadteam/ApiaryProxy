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
 * Verifies that a player follows Minecraft's gravity physics.
 */
public class GravityCheck extends CheckBase {
  // Data keys for session storage
  private static final String DATA_KEY_TICKS = "gravity.ticks";
  private static final String DATA_KEY_TELEPORTED = "gravity.teleported";
  private static final String DATA_KEY_CAN_FALL = "gravity.canFall";
  private static final String DATA_KEY_PLATFORM_BLOCK = "gravity.platformBlock";
  private static final String DATA_KEY_PLATFORM_Y = "gravity.platformY";
  private static final String DATA_KEY_LAST_Y = "gravity.lastY";
  private static final String DATA_KEY_LAST_ON_GROUND = "gravity.lastOnGround";
  
  // Pre-computed Y motion values for each tick of falling
  private static final double[] PREDICTED_Y_MOTION = new double[20];
  
  static {
    // Pre-compute the predicted Y motion for each tick
    // Based on Minecraft's gravity formula: motion.y -= 0.08; motion.y *= 0.98;
    double motionY = 0.0;
    for (int i = 0; i < PREDICTED_Y_MOTION.length; i++) {
      motionY -= 0.08;
      motionY *= 0.98;
      PREDICTED_Y_MOTION[i] = motionY;
    }
  }
  
  /**
   * Creates a new gravity check.
   *
   * @param antiBot the antibot instance
   */
  public GravityCheck(ApiaryAntibot antiBot) {
    super(antiBot);
  }
  
  @Override
  public void initialize(VerificationSession session) {
    // Initialize session data
    session.setData(DATA_KEY_TICKS, 0);
    session.setData(DATA_KEY_TELEPORTED, false);
    session.setData(DATA_KEY_CAN_FALL, false);
    
    // Select a random block type for the platform
    BlockType platformBlock = PacketPreparer.getRandomPlatformBlock();
    session.setData(DATA_KEY_PLATFORM_BLOCK, platformBlock);
    
    // Set the platform Y position (64 is a common ground level)
    int platformY = 64;
    session.setData(DATA_KEY_PLATFORM_Y, platformY);
    
    // Set initial Y position (10 blocks above platform)
    double initialY = platformY + 10.0;
    session.setData(DATA_KEY_LAST_Y, initialY);
    session.setData(DATA_KEY_LAST_ON_GROUND, false);
    
    // Send join game packet
    session.sendPacket(PacketPreparer.getJoinGamePacket());
    
    // Send spawn position packet at platform coordinates
    session.sendPacket(PacketPreparer.getSpawnPositionPacket(0, platformY, 0));
    
    // Teleport player to starting position
    session.sendPacket(PacketPreparer.getFallStartPositionPacket(0.0, initialY, 0.0));
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
      session.setData(DATA_KEY_CAN_FALL, true);
      return true;
    }
    
    // Check if player can fall
    Boolean canFall = (Boolean) session.getData(DATA_KEY_CAN_FALL);
    if (canFall == null || !canFall) {
      return true;
    }
    
    // Get platform Y position
    Integer platformY = (Integer) session.getData(DATA_KEY_PLATFORM_Y);
    if (platformY == null) {
      return true;
    }
    
    // Get last Y position and on-ground state
    Double lastY = (Double) session.getData(DATA_KEY_LAST_Y);
    Boolean lastOnGround = (Boolean) session.getData(DATA_KEY_LAST_ON_GROUND);
    
    if (lastY == null || lastOnGround == null) {
      // Initialize if not set
      session.setData(DATA_KEY_LAST_Y, packet.getY());
      session.setData(DATA_KEY_LAST_ON_GROUND, packet.isOnGround());
      return true;
    }
    
    // Check for illegal movement packet order
    if (lastOnGround && !packet.isOnGround()) {
      logger.debug("Player {} sent illegal movement packet order: was on ground, now in air",
          session.getUsername());
      return false;
    }
    
    // If player is on ground, check if they're at the platform
    if (packet.isOnGround()) {
      // Check if player is at the platform Y position
      double landingY = platformY + 1.0; // Player position is at feet level
      
      // Allow small tolerance for landing position
      if (Math.abs(packet.getY() - landingY) > 0.1) {
        logger.debug("Player {} landed at wrong height: expected {}, got {}",
            session.getUsername(), landingY, packet.getY());
        return false;
      }
      
      // Player has landed successfully
      return true;
    }
    
    // Player is in the air, check gravity motion
    Integer ticks = (Integer) session.getData(DATA_KEY_TICKS);
    if (ticks == null) {
      ticks = 0;
    }
    
    // Increment tick counter
    ticks++;
    session.setData(DATA_KEY_TICKS, ticks);
    
    // Check if we've exceeded the maximum ticks
    int maxTicks = antiBot.getConfig().getVerification().getGravity().getMaxMovementTicks();
    if (ticks > maxTicks) {
      logger.debug("Player {} exceeded maximum fall ticks: {}", session.getUsername(), maxTicks);
      return false;
    }
    
    // Check if the tick count is within the predicted motion array bounds
    if (ticks < PREDICTED_Y_MOTION.length) {
      // Calculate expected Y position based on gravity
      double expectedY = lastY + PREDICTED_Y_MOTION[ticks - 1];
      
      // Allow small tolerance for position differences
      if (Math.abs(packet.getY() - expectedY) > 0.1) {
        logger.debug("Player {} has unexpected Y motion: expected {}, got {}",
            session.getUsername(), expectedY, packet.getY());
        return false;
      }
    }
    
    // Update last position and on-ground state
    session.setData(DATA_KEY_LAST_Y, packet.getY());
    session.setData(DATA_KEY_LAST_ON_GROUND, packet.isOnGround());
    
    return true;
  }
  
  @Override
  public boolean onVerify(VerificationSession session) {
    // This check is verified through packet processing
    return true;
  }
  
  @Override
  public void reset(VerificationSession session) {
    // Clear all gravity-related data
    session.removeData(DATA_KEY_TICKS);
    session.removeData(DATA_KEY_TELEPORTED);
    session.removeData(DATA_KEY_CAN_FALL);
    session.removeData(DATA_KEY_PLATFORM_BLOCK);
    session.removeData(DATA_KEY_PLATFORM_Y);
    session.removeData(DATA_KEY_LAST_Y);
    session.removeData(DATA_KEY_LAST_ON_GROUND);
  }
} 