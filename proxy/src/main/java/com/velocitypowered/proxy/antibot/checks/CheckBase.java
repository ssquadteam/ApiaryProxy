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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for verification checks.
 */
public abstract class CheckBase implements Check {
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  protected final ApiaryAntibot antiBot;
  
  /**
   * Creates a new check.
   *
   * @param antiBot the antibot instance
   */
  protected CheckBase(ApiaryAntibot antiBot) {
    this.antiBot = antiBot;
  }
  
  @Override
  public String getName() {
    return getClass().getSimpleName();
  }
  
  @Override
  public void initialize(VerificationSession session) {
    // Default implementation does nothing
  }
  
  @Override
  public boolean onVerify(VerificationSession session) {
    // Default implementation always passes
    return true;
  }
  
  @Override
  public void reset(VerificationSession session) {
    // Default implementation does nothing
  }
} 