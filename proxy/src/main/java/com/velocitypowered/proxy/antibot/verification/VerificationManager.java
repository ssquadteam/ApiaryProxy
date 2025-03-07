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
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.antibot.ApiaryAntibot;
import com.velocitypowered.proxy.antibot.checks.CaptchaCheck;
import com.velocitypowered.proxy.antibot.checks.Check;
import com.velocitypowered.proxy.antibot.checks.ClientBrandCheck;
import com.velocitypowered.proxy.antibot.checks.CollisionCheck;
import com.velocitypowered.proxy.antibot.checks.GravityCheck;
import com.velocitypowered.proxy.antibot.checks.VehicleCheck;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages verification sessions for players.
 */
public class VerificationManager {

  private static final Logger logger = LogManager.getLogger(VerificationManager.class);
  
  private final ApiaryAntibot antiBot;
  private final Map<UUID, VerificationSession> sessions = new ConcurrentHashMap<>();
  private final Map<InetAddress, Integer> failedAttempts = new ConcurrentHashMap<>();
  private final Set<InetAddress> blacklistedAddresses = ConcurrentHashMap.newKeySet();
  
  private ScheduledTask cleanupTask;
  private boolean enabled = false;

  public VerificationManager(ApiaryAntibot antiBot) {
    this.antiBot = antiBot;
  }

  /**
   * Starts the verification manager.
   */
  public void start() {
    if (enabled) {
      return;
    }
    
    enabled = true;
    
    // Start cleanup task to remove expired sessions
    cleanupTask = antiBot.getServer().getScheduler().buildTask(antiBot, this::cleanupSessions)
        .repeat(30, TimeUnit.SECONDS)
        .schedule();
    
    logger.info(antiBot.getMessages().get("console.verification.manager.started"));
  }

  /**
   * Stops the verification manager.
   */
  public void stop() {
    if (!enabled) {
      return;
    }
    
    enabled = false;
    
    if (cleanupTask != null) {
      cleanupTask.cancel();
      cleanupTask = null;
    }
    
    sessions.clear();
    failedAttempts.clear();
    blacklistedAddresses.clear();
    
    logger.info(antiBot.getMessages().get("console.verification.manager.stopped"));
  }

  /**
   * Begins the verification process for a player.
   *
   * @param event the login event
   * @return true if verification was started, false if rejected
   */
  public boolean beginVerification(PreLoginEvent event) {
    if (!enabled) {
      return true;
    }
    
    InetAddress address = event.getConnection().getRemoteAddress().getAddress();
    
    // Check if IP is blacklisted
    if (blacklistedAddresses.contains(address)) {
      if (antiBot.getConfig().getVerification().isLogConnections()) {
        logger.info(antiBot.getMessages().format("console.blocked.blacklisted", address.getHostAddress()));
      }
      
      event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
          antiBot.getMessages().getComponent("player.blacklisted")));
      return false;
    }
    
    // Create new session
    UUID id = UUID.randomUUID();
    VerificationSession session = new VerificationSession(antiBot, event);
    sessions.put(id, session);
    
    // Add checks
    registerChecks();
    
    // Set session to verifying state
    session.setState(VerificationSession.VerificationState.VERIFYING);
    
    if (antiBot.getConfig().getVerification().isLogConnections() 
        || (antiBot.isUnderAttack() && antiBot.getConfig().getVerification().isLogDuringAttack())) {
      logger.info(antiBot.getMessages().format("console.verification.started", 
          session.getUsername(), address.getHostAddress()));
    }
    
    // In a real implementation, this would be more complex and involve packets
    // For this simplified version, we'll just run the verification immediately
    boolean verificationPassed = session.verify();
    
    if (verificationPassed) {
      // Verification passed, mark as completed
      session.setState(VerificationSession.VerificationState.COMPLETED);
      logger.debug(antiBot.getMessages().format("console.verification.passed", session.getUsername()));
      
      // Allow the connection to proceed
      return true;
    } else {
      // Verification failed
      session.setState(VerificationSession.VerificationState.FAILED);
      
      // Record failed attempt
      int attempts = failedAttempts.compute(address, (ip, count) -> count == null ? 1 : count + 1);
      
      // Check if we should blacklist this IP
      int blacklistThreshold = antiBot.getConfig().getVerification().getBlacklistThreshold();
      if (blacklistThreshold > 0 && attempts >= blacklistThreshold) {
        blacklistedAddresses.add(address);
        
        // Schedule removal from blacklist
        final InetAddress finalAddress = address;
        antiBot.getServer().getScheduler().buildTask(antiBot, () -> {
          blacklistedAddresses.remove(finalAddress);
          logger.debug(antiBot.getMessages().format("console.blacklist.removed", finalAddress.getHostAddress()));
        }).delay(antiBot.getConfig().getVerification().getBlacklistTime(), TimeUnit.MILLISECONDS)
            .schedule();
        
        logger.info(antiBot.getMessages().format("console.blacklist.added", 
            address.getHostAddress(), attempts));
      }
      
      // Deny the connection with failure reason
      String reason = session.getFailReason() != null 
          ? session.getFailReason() : "Verification failed";
      
      event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
          antiBot.getMessages().formatComponent("player.verification.failed", reason)));
      
      return false;
    }
  }

  /**
   * Cleans up expired sessions and attempts.
   */
  private void cleanupSessions() {
    long now = System.currentTimeMillis();
    long rememberTime = antiBot.getConfig().getVerification().getRememberTime();
    
    // Clean up sessions
    sessions.entrySet().removeIf(entry -> {
      VerificationSession session = entry.getValue();
      boolean expired = now - session.getCreationTime() > rememberTime;
      
      if (expired) {
        session.cleanup();
      }
      
      return expired;
    });
    
    // Clean up failed attempts
    failedAttempts.entrySet().removeIf(entry -> 
        now - entry.getValue() > rememberTime);
  }

  /**
   * Gets a verification session by ID.
   *
   * @param sessionId the session ID
   * @return the verification session, or null if not found
   */
  public VerificationSession getSession(UUID sessionId) {
    return sessions.get(sessionId);
  }

  /**
   * Gets the number of active verification sessions.
   *
   * @return the number of sessions
   */
  public int getSessionCount() {
    return sessions.size();
  }

  /**
   * Gets the number of blacklisted addresses.
   *
   * @return the number of blacklisted addresses
   */
  public int getBlacklistCount() {
    return blacklistedAddresses.size();
  }

  /**
   * Registers verification checks based on configuration.
   * This method sets up the appropriate verification checks
   * according to the current configuration settings.
   */
  public void registerChecks() {
    // Register gravity check if enabled
    if (antiBot.getConfig().getVerification().getGravity().isEnabled()) {
      registerCheck(new GravityCheck(antiBot));
    }
    
    // Register collision check if enabled
    if (antiBot.getConfig().getVerification().getCollision().isEnabled()) {
      registerCheck(new CollisionCheck(antiBot));
    }
    
    // Register vehicle check if enabled
    if (antiBot.getConfig().getVerification().getVehicle().isEnabled()) {
      registerCheck(new VehicleCheck(antiBot));
    }
    
    // Register CAPTCHA check if enabled
    if (antiBot.getConfig().getVerification().getMapCaptcha().isEnabled()) {
      registerCheck(new CaptchaCheck(antiBot));
    }
    
    // Register client brand check if enabled
    if (antiBot.getConfig().getVerification().getClientBrand().isEnabled()) {
      registerCheck(new ClientBrandCheck(antiBot));
    }
  }

  /**
   * Registers a check to be used during verification.
   *
   * @param check the check to register
   */
  private void registerCheck(Check check) {
    // Add the check to all active sessions
    for (VerificationSession session : sessions.values()) {
      session.addCheck(check);
    }
  }
} 