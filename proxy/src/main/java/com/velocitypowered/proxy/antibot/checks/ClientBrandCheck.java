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
import com.velocitypowered.proxy.antibot.verification.VerificationSession;
import java.util.regex.Pattern;

/**
 * Verifies that a player's client brand is valid.
 */
public class ClientBrandCheck extends CheckBase {
  // Data keys for session storage
  private static final String DATA_KEY_BRAND = "clientBrand.brand";
  private static final String DATA_KEY_VALID = "clientBrand.valid";
  
  /**
   * Creates a new client brand check.
   *
   * @param antiBot the antibot instance
   */
  public ClientBrandCheck(ApiaryAntibot antiBot) {
    super(antiBot);
  }
  
  @Override
  public void initialize(VerificationSession session) {
    // Initialize session data
    session.setData(DATA_KEY_BRAND, null);
    session.setData(DATA_KEY_VALID, false);
  }
  
  /**
   * Processes a client brand.
   *
   * @param session the verification session
   * @param brand the client brand
   * @return true if the brand is valid, false otherwise
   */
  public boolean processClientBrand(VerificationSession session, String brand) {
    // Store the brand
    session.setData(DATA_KEY_BRAND, brand);
    
    // Check if the brand is valid
    boolean valid = isValidBrand(brand);
    session.setData(DATA_KEY_VALID, valid);
    
    if (!valid) {
      logger.debug("Player {} failed client brand check: invalid brand: {}", 
          session.getUsername(), brand);
    }
    
    return valid;
  }
  
  /**
   * Checks if a client brand is valid.
   *
   * @param brand the client brand
   * @return true if the brand is valid, false otherwise
   */
  private boolean isValidBrand(String brand) {
    if (brand == null) {
      return false;
    }
    
    // Check if the brand is too long
    int maxLength = antiBot.getConfig().getVerification().getClientBrand().getMaxLength();
    if (brand.length() > maxLength) {
      return false;
    }
    
    // Check if the brand matches the regex
    String regex = antiBot.getConfig().getVerification().getClientBrand().getValidRegex();
    return Pattern.matches(regex, brand);
  }
  
  @Override
  public boolean onVerify(VerificationSession session) {
    // Check if the brand is valid
    Boolean valid = (Boolean) session.getData(DATA_KEY_VALID);
    if (valid == null || !valid) {
      String brand = (String) session.getData(DATA_KEY_BRAND);
      logger.debug("Player {} failed client brand check: invalid brand: {}", 
          session.getUsername(), brand);
      return false;
    }
    
    return true;
  }
  
  @Override
  public void reset(VerificationSession session) {
    // Clear all client brand-related data
    session.removeData(DATA_KEY_BRAND);
    session.removeData(DATA_KEY_VALID);
  }
} 