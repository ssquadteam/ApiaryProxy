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
import com.velocitypowered.proxy.antibot.captcha.CaptchaInfo;
import com.velocitypowered.proxy.antibot.verification.VerificationSession;

/**
 * Verifies that a player can solve a CAPTCHA.
 */
public class CaptchaCheck extends CheckBase {
  // Data keys for session storage
  private static final String DATA_KEY_CAPTCHA = "captcha.info";
  private static final String DATA_KEY_TRIES = "captcha.tries";
  private static final String DATA_KEY_START_TIME = "captcha.startTime";
  
  /**
   * Creates a new CAPTCHA check.
   *
   * @param antiBot the antibot instance
   */
  public CaptchaCheck(ApiaryAntibot antiBot) {
    super(antiBot);
  }
  
  @Override
  public void initialize(VerificationSession session) {
    // Get a random CAPTCHA from the generator
    CaptchaInfo captcha = antiBot.getCaptchaGenerator().getRandomCaptcha();
    if (captcha == null) {
      logger.warn("No CAPTCHA available for player {}", session.getUsername());
      return;
    }
    
    // Store the CAPTCHA in the session
    session.setData(DATA_KEY_CAPTCHA, captcha);
    
    // Set the maximum number of tries
    int maxTries = antiBot.getConfig().getVerification().getMapCaptcha().getMaxTries();
    session.setData(DATA_KEY_TRIES, maxTries);
    
    // Set the start time
    session.setData(DATA_KEY_START_TIME, System.currentTimeMillis());
    
    // Send the CAPTCHA to the player
    // In a real implementation, this would send the map data to the player
    // For now, this is just a placeholder
  }
  
  @Override
  public boolean onVerify(VerificationSession session) {
    // This check is verified through chat messages
    return true;
  }
  
  /**
   * Processes a chat message from the player.
   *
   * @param session the verification session
   * @param message the chat message
   * @return true if the CAPTCHA was solved, false otherwise
   */
  public boolean processChatMessage(VerificationSession session, String message) {
    // Get the CAPTCHA from the session
    CaptchaInfo captcha = (CaptchaInfo) session.getData(DATA_KEY_CAPTCHA);
    if (captcha == null) {
      logger.warn("No CAPTCHA found for player {}", session.getUsername());
      return false;
    }
    
    // Check if the CAPTCHA has timed out
    Long startTime = (Long) session.getData(DATA_KEY_START_TIME);
    if (startTime == null) {
      logger.warn("No start time found for player {}", session.getUsername());
      return false;
    }
    
    long elapsed = System.currentTimeMillis() - startTime;
    int maxDuration = antiBot.getConfig().getVerification().getMapCaptcha().getMaxDuration();
    if (elapsed > maxDuration) {
      logger.debug("Player {} CAPTCHA timed out after {} ms", session.getUsername(), elapsed);
      return false;
    }
    
    // Check if the player has any tries left
    Integer tries = (Integer) session.getData(DATA_KEY_TRIES);
    if (tries == null || tries <= 0) {
      logger.debug("Player {} has no CAPTCHA tries left", session.getUsername());
      return false;
    }
    
    // Decrement the number of tries
    session.setData(DATA_KEY_TRIES, tries - 1);
    
    // Check if the answer is correct
    String answer = captcha.getAnswer();
    if (message.equalsIgnoreCase(answer)) {
      logger.debug("Player {} solved CAPTCHA: {}", session.getUsername(), answer);
      return true;
    }
    
    // Answer is incorrect
    logger.debug("Player {} failed CAPTCHA: {} (expected {})", 
        session.getUsername(), message, answer);
    
    // If the player has no tries left, fail the verification
    if (tries <= 1) {
      logger.debug("Player {} has no CAPTCHA tries left", session.getUsername());
      return false;
    }
    
    // Player still has tries left
    return false;
  }
  
  @Override
  public void reset(VerificationSession session) {
    // Clear all CAPTCHA-related data
    session.removeData(DATA_KEY_CAPTCHA);
    session.removeData(DATA_KEY_TRIES);
    session.removeData(DATA_KEY_START_TIME);
  }
} 