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

package com.velocitypowered.proxy.antibot.verification;

import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.proxy.antibot.ApiaryAntibot;
import com.velocitypowered.proxy.antibot.checks.Check;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages verification of a player connection.
 */
public class VerificationSession {

  private static final Logger logger = LogManager.getLogger(VerificationSession.class);
  
  private final ApiaryAntibot antiBot;
  private final UUID sessionId;
  private final String username;
  private final InetAddress ipAddress;
  private final long creationTime;
  
  private final List<Check> checks = new ArrayList<>();
  private final Map<String, Object> sessionData = new ConcurrentHashMap<>();
  
  private VerificationState state = VerificationState.INITIALIZING;
  private int failedChecks = 0;
  private String failReason = null;

  /**
   * Creates a new verification session.
   *
   * @param antiBot the antibot instance
   * @param event the login event
   */
  public VerificationSession(ApiaryAntibot antiBot, PreLoginEvent event) {
    this.antiBot = antiBot;
    this.sessionId = UUID.randomUUID();
    this.username = event.getUsername();
    this.ipAddress = event.getConnection().getRemoteAddress().getAddress();
    this.creationTime = System.currentTimeMillis();
  }

  /**
   * Gets the unique ID of this session.
   *
   * @return the session ID
   */
  public UUID getSessionId() {
    return sessionId;
  }

  /**
   * Gets the username associated with this session.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Gets the IP address associated with this session.
   *
   * @return the IP address
   */
  public InetAddress getIpAddress() {
    return ipAddress;
  }

  /**
   * Gets the time this session was created.
   *
   * @return the creation time in milliseconds
   */
  public long getCreationTime() {
    return creationTime;
  }

  /**
   * Gets the current state of this verification session.
   *
   * @return the verification state
   */
  public VerificationState getState() {
    return state;
  }

  /**
   * Sets the state of this verification session.
   *
   * @param state the new state
   */
  public void setState(VerificationState state) {
    this.state = state;
  }

  /**
   * Gets the number of checks that have failed.
   *
   * @return the number of failed checks
   */
  public int getFailedChecks() {
    return failedChecks;
  }

  /**
   * Increments the failed checks counter.
   */
  public void incrementFailedChecks() {
    this.failedChecks++;
  }

  /**
   * Gets the reason for verification failure.
   *
   * @return the failure reason, or null if none
   */
  public String getFailReason() {
    return failReason;
  }

  /**
   * Sets the reason for verification failure.
   *
   * @param failReason the failure reason
   */
  public void setFailReason(String failReason) {
    this.failReason = failReason;
  }

  /**
   * Adds a verification check to this session.
   *
   * @param check the check to add
   */
  public void addCheck(Check check) {
    checks.add(check);
    check.initialize(this);
  }

  /**
   * Gets the list of checks for this session.
   *
   * @return the list of checks
   */
  public List<Check> getChecks() {
    return checks;
  }

  /**
   * Stores data in this session.
   *
   * @param key the data key
   * @param value the data value
   */
  public void setData(String key, Object value) {
    sessionData.put(key, value);
  }

  /**
   * Gets data from this session.
   *
   * @param key the data key
   * @return the data value, or null if not found
   */
  public Object getData(String key) {
    return sessionData.get(key);
  }

  /**
   * Checks if this session has data for the given key.
   *
   * @param key the data key
   * @return true if the session has data for the key, false otherwise
   */
  public boolean hasData(String key) {
    return sessionData.containsKey(key);
  }

  /**
   * Removes data from this session.
   *
   * @param key the key
   */
  public void removeData(String key) {
    sessionData.remove(key);
  }

  /**
   * Verifies this session.
   *
   * @return true if the verification was successful, false otherwise
   */
  public boolean verify() {
    if (state != VerificationState.VERIFYING) {
      return false;
    }
    
    for (Check check : checks) {
      if (!check.onVerify(this)) {
        incrementFailedChecks();
        if (failReason == null) {
          setFailReason("Failed check: " + check.getName());
        }
        
        logger.debug("Player {} failed check: {}", username, check.getName());
        return false;
      }
    }
    
    // All checks passed
    state = VerificationState.COMPLETE;
    return true;
  }

  /**
   * Cleans up this verification session.
   */
  public void cleanup() {
    for (Check check : checks) {
      check.reset(this);
    }
    
    sessionData.clear();
    state = VerificationState.COMPLETE;
  }

  /**
   * Sends a packet to the player.
   *
   * @param packet the packet to send
   */
  public void sendPacket(Object packet) {
    // In a real implementation, this would send the packet to the player
    // For now, this is just a placeholder
  }

  /**
   * Verification session states.
   */
  public enum VerificationState {
    INITIALIZING,
    VERIFYING,
    COMPLETED,
    FAILED,
    COMPLETE
  }
} 