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

package com.velocitypowered.proxy.antibot.captcha;

/**
 * Holds information about a CAPTCHA.
 */
public class CaptchaInfo {
  private final String answer;
  private final byte[] mapData;
  private final int mapId;
  
  /**
   * Creates a new CAPTCHA info.
   *
   * @param answer the answer to the CAPTCHA
   * @param mapData the map data for the CAPTCHA
   * @param mapId the map ID for the CAPTCHA
   */
  public CaptchaInfo(String answer, byte[] mapData, int mapId) {
    this.answer = answer;
    this.mapData = mapData;
    this.mapId = mapId;
  }
  
  /**
   * Gets the answer to the CAPTCHA.
   *
   * @return the answer
   */
  public String getAnswer() {
    return answer;
  }
  
  /**
   * Gets the map data for the CAPTCHA.
   *
   * @return the map data
   */
  public byte[] getMapData() {
    return mapData;
  }
  
  /**
   * Gets the map ID for the CAPTCHA.
   *
   * @return the map ID
   */
  public int getMapId() {
    return mapId;
  }
} 