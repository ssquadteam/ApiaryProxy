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
import com.velocitypowered.proxy.antibot.protocol.PlayerPositionPacket;
import com.velocitypowered.proxy.antibot.verification.VerificationSession;

/**
 * Verifies that a player interacts correctly with vehicles.
 */
public class VehicleCheck extends CheckBase {
  // Data keys for session storage
  private static final String DATA_KEY_STATE = "vehicle.state";
  private static final String DATA_KEY_ROTATIONS = "vehicle.rotations";
  private static final String DATA_KEY_INPUTS = "vehicle.inputs";
  private static final String DATA_KEY_MOVEMENTS = "vehicle.movements";
  
  // Vehicle states
  private enum VehicleState {
    WAITING,
    IN_VEHICLE
  }
  
  /**
   * Creates a new vehicle check.
   *
   * @param antiBot the antibot instance
   */
  public VehicleCheck(ApiaryAntibot antiBot) {
    super(antiBot);
  }
  
  @Override
  public void initialize(VerificationSession session) {
    // Initialize session data
    session.setData(DATA_KEY_STATE, VehicleState.WAITING);
    session.setData(DATA_KEY_ROTATIONS, 0);
    session.setData(DATA_KEY_INPUTS, 0);
    session.setData(DATA_KEY_MOVEMENTS, 0);
    
    // In a real implementation, this would spawn a vehicle entity
    // For now, this is just a placeholder
  }
  
  /**
   * Processes a player position packet.
   *
   * @param session the verification session
   * @param packet the position packet
   * @return true if the position is valid, false otherwise
   */
  public boolean processPosition(VerificationSession session, PlayerPositionPacket packet) {
    VehicleState state = (VehicleState) session.getData(DATA_KEY_STATE);
    if (state == null) {
      return true;
    }
    
    if (state == VehicleState.IN_VEHICLE) {
      // Player is in a vehicle, increment movement count
      Integer movements = (Integer) session.getData(DATA_KEY_MOVEMENTS);
      if (movements != null) {
        session.setData(DATA_KEY_MOVEMENTS, movements + 1);
      }
    }
    
    return true;
  }
  
  /**
   * Processes a player rotation packet.
   *
   * @param session the verification session
   * @param yaw the yaw rotation
   * @param pitch the pitch rotation
   * @return true if the rotation is valid, false otherwise
   */
  public boolean processRotation(VerificationSession session, float yaw, float pitch) {
    VehicleState state = (VehicleState) session.getData(DATA_KEY_STATE);
    if (state == null) {
      return true;
    }
    
    if (state == VehicleState.IN_VEHICLE) {
      // Player is in a vehicle, increment rotation count
      Integer rotations = (Integer) session.getData(DATA_KEY_ROTATIONS);
      if (rotations != null) {
        session.setData(DATA_KEY_ROTATIONS, rotations + 1);
      }
    }
    
    return true;
  }
  
  /**
   * Processes a paddle action.
   *
   * @param session the verification session
   * @param leftPaddle whether the left paddle was used
   * @param rightPaddle whether the right paddle was used
   * @return true if the paddle action is valid, false otherwise
   */
  public boolean processPaddleAction(VerificationSession session, boolean leftPaddle, boolean rightPaddle) {
    VehicleState state = (VehicleState) session.getData(DATA_KEY_STATE);
    if (state == null) {
      return true;
    }
    
    if (state == VehicleState.WAITING) {
      // Player has entered the vehicle
      session.setData(DATA_KEY_STATE, VehicleState.IN_VEHICLE);
    }
    
    if (state == VehicleState.IN_VEHICLE) {
      // Player is in a vehicle, increment input count
      Integer inputs = (Integer) session.getData(DATA_KEY_INPUTS);
      if (inputs != null) {
        session.setData(DATA_KEY_INPUTS, inputs + 1);
      }
    }
    
    return true;
  }
  
  /**
   * Processes a vehicle movement.
   *
   * @param session the verification session
   * @param x the x position
   * @param y the y position
   * @param z the z position
   * @return true if the movement is valid, false otherwise
   */
  public boolean processVehicleMovement(VerificationSession session, double x, double y, double z) {
    VehicleState state = (VehicleState) session.getData(DATA_KEY_STATE);
    if (state == null) {
      return true;
    }
    
    if (state == VehicleState.WAITING) {
      // Player has entered the vehicle
      session.setData(DATA_KEY_STATE, VehicleState.IN_VEHICLE);
    }
    
    if (state == VehicleState.IN_VEHICLE) {
      // Player is in a vehicle, increment movement count
      Integer movements = (Integer) session.getData(DATA_KEY_MOVEMENTS);
      if (movements != null) {
        session.setData(DATA_KEY_MOVEMENTS, movements + 1);
      }
    }
    
    return true;
  }
  
  @Override
  public boolean onVerify(VerificationSession session) {
    // Check if the player has entered a vehicle
    VehicleState state = (VehicleState) session.getData(DATA_KEY_STATE);
    if (state != VehicleState.IN_VEHICLE) {
      logger.debug("Player {} failed vehicle check: never entered vehicle", 
          session.getUsername());
      return false;
    }
    
    // Check if the player has sent enough packets
    Integer inputs = (Integer) session.getData(DATA_KEY_INPUTS);
    Integer movements = (Integer) session.getData(DATA_KEY_MOVEMENTS);
    Integer rotations = (Integer) session.getData(DATA_KEY_ROTATIONS);
    
    if (inputs == null || movements == null || rotations == null) {
      logger.debug("Player {} failed vehicle check: missing data", 
          session.getUsername());
      return false;
    }
    
    // Calculate total packets
    int totalPackets = inputs + movements + rotations;
    int minimumPackets = antiBot.getConfig().getVerification().getVehicle().getMinimumPackets();
    
    if (totalPackets < minimumPackets) {
      logger.debug("Player {} failed vehicle check: not enough packets ({}/{})", 
          session.getUsername(), totalPackets, minimumPackets);
      return false;
    }
    
    return true;
  }
  
  @Override
  public void reset(VerificationSession session) {
    // Clear all vehicle-related data
    session.removeData(DATA_KEY_STATE);
    session.removeData(DATA_KEY_ROTATIONS);
    session.removeData(DATA_KEY_INPUTS);
    session.removeData(DATA_KEY_MOVEMENTS);
  }
} 