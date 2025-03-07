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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates CAPTCHA images for player verification.
 */
public class CaptchaGenerator {
  private static final Logger logger = LoggerFactory.getLogger(CaptchaGenerator.class);
  private static final Random RANDOM = new SecureRandom();
  
  // Map dimensions
  private static final int MAP_WIDTH = 128;
  private static final int MAP_HEIGHT = 128;
  
  // Colors
  private static final Color BACKGROUND_COLOR = new Color(240, 240, 240);
  private static final Color TEXT_COLOR = new Color(10, 10, 10);
  private static final Color NOISE_COLOR = new Color(180, 180, 180);
  private static final Color LINE_COLOR = new Color(80, 80, 80);
  
  private final String charset;
  private final int codeLength;
  private final BufferedImage backgroundImage;
  private final List<CaptchaInfo> captchaCache = new CopyOnWriteArrayList<>();
  
  /**
   * Creates a new CAPTCHA generator.
   *
   * @param charset the character set to use for generating CAPTCHA codes
   * @param codeLength the length of the CAPTCHA code
   * @param backgroundImagePath the path to the background image (can be null)
   */
  public CaptchaGenerator(String charset, int codeLength, Path backgroundImagePath) {
    this.charset = charset;
    this.codeLength = codeLength;
    
    // Load background image if provided
    BufferedImage loadedImage = null;
    if (backgroundImagePath != null) {
      try {
        loadedImage = ImageIO.read(backgroundImagePath.toFile());
        logger.info("Loaded CAPTCHA background image from {}", backgroundImagePath);
      } catch (IOException e) {
        logger.error("Failed to load CAPTCHA background image", e);
      }
    }
    this.backgroundImage = loadedImage;
  }
  
  /**
   * Generates a batch of CAPTCHAs and adds them to the cache.
   *
   * @param count the number of CAPTCHAs to generate
   */
  public void generateCaptchas(int count) {
    for (int i = 0; i < count; i++) {
      try {
        String code = generateRandomCode();
        BufferedImage image = generateCaptchaImage(code);
        byte[] mapData = convertToMapData(image);
        
        captchaCache.add(new CaptchaInfo(code, mapData, i));
      } catch (Exception e) {
        logger.error("Failed to generate CAPTCHA", e);
      }
    }
  }
  
  /**
   * Gets a random CAPTCHA from the cache.
   *
   * @return a random CAPTCHA, or null if none are available
   */
  public CaptchaInfo getRandomCaptcha() {
    if (captchaCache.isEmpty()) {
      return null;
    }
    
    int index = RANDOM.nextInt(captchaCache.size());
    return captchaCache.remove(index);
  }
  
  /**
   * Checks if there are CAPTCHAs available in the cache.
   *
   * @return true if there are CAPTCHAs available, false otherwise
   */
  public boolean hasCaptchas() {
    return !captchaCache.isEmpty();
  }
  
  /**
   * Gets the number of CAPTCHAs in the cache.
   *
   * @return the number of CAPTCHAs in the cache
   */
  public int getCaptchaCount() {
    return captchaCache.size();
  }
  
  /**
   * Generates a CAPTCHA image with the given code.
   *
   * @param code the CAPTCHA code
   * @return the generated image
   */
  private BufferedImage generateCaptchaImage(String code) {
    BufferedImage image = new BufferedImage(MAP_WIDTH, MAP_HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();
    
    // Enable anti-aliasing
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    
    // Draw background
    drawBackground(g2d);
    
    // Draw CAPTCHA text
    drawCaptchaText(g2d, code);
    
    // Add noise
    addNoise(g2d);
    
    // Add obfuscating lines
    drawLines(g2d);
    
    g2d.dispose();
    return image;
  }
  
  /**
   * Draws the background of the CAPTCHA image.
   *
   * @param g2d the graphics context
   */
  private void drawBackground(Graphics2D g2d) {
    if (backgroundImage != null) {
      // Use the provided background image
      g2d.drawImage(backgroundImage, 0, 0, MAP_WIDTH, MAP_HEIGHT, null);
    } else {
      // Use a solid color background
      g2d.setColor(BACKGROUND_COLOR);
      g2d.fillRect(0, 0, MAP_WIDTH, MAP_HEIGHT);
    }
  }
  
  /**
   * Draws the CAPTCHA text on the image.
   *
   * @param g2d the graphics context
   * @param code the CAPTCHA code to draw
   */
  private void drawCaptchaText(Graphics2D g2d, String code) {
    g2d.setColor(TEXT_COLOR);
    
    // Use a bold font for better readability
    Font font = new Font("Arial", Font.BOLD, 30);
    g2d.setFont(font);
    
    // Calculate text width for centering
    int textWidth = g2d.getFontMetrics().stringWidth(code);
    int x = (MAP_WIDTH - textWidth) / 2;
    
    // Draw each character with random rotation and position
    for (int i = 0; i < code.length(); i++) {
      char c = code.charAt(i);
      
      // Create a copy of the graphics context for this character
      Graphics2D charG2d = (Graphics2D) g2d.create();
      
      // Calculate position for this character
      int charWidth = charG2d.getFontMetrics().charWidth(c);
      int charX = x + (i * charWidth) + (i * 5); // Add some spacing between characters
      int charY = MAP_HEIGHT / 2 + RANDOM.nextInt(20) - 10; // Random Y position
      
      // Apply random rotation
      AffineTransform transform = new AffineTransform();
      transform.translate(charX, charY);
      transform.rotate(Math.toRadians(RANDOM.nextInt(40) - 20), 0, 0);
      charG2d.transform(transform);
      
      // Draw the character
      charG2d.drawString(String.valueOf(c), 0, 0);
      
      charG2d.dispose();
    }
  }
  
  /**
   * Adds random noise to the CAPTCHA image.
   *
   * @param g2d the graphics context
   */
  private void addNoise(Graphics2D g2d) {
    g2d.setColor(NOISE_COLOR);
    
    // Add random dots
    for (int i = 0; i < 100; i++) {
      int x = RANDOM.nextInt(MAP_WIDTH);
      int y = RANDOM.nextInt(MAP_HEIGHT);
      int size = RANDOM.nextInt(3) + 1;
      g2d.fillRect(x, y, size, size);
    }
  }
  
  /**
   * Draws random lines on the CAPTCHA image to make it harder to read by bots.
   *
   * @param g2d the graphics context
   */
  private void drawLines(Graphics2D g2d) {
    g2d.setColor(LINE_COLOR);
    g2d.setStroke(new BasicStroke(1.5f));
    
    // Draw random lines
    for (int i = 0; i < 5; i++) {
      int x1 = RANDOM.nextInt(MAP_WIDTH);
      int y1 = RANDOM.nextInt(MAP_HEIGHT);
      int x2 = RANDOM.nextInt(MAP_WIDTH);
      int y2 = RANDOM.nextInt(MAP_HEIGHT);
      
      g2d.drawLine(x1, y1, x2, y2);
    }
  }
  
  /**
   * Generates a random CAPTCHA code.
   *
   * @return the generated code
   */
  private String generateRandomCode() {
    StringBuilder code = new StringBuilder(codeLength);
    for (int i = 0; i < codeLength; i++) {
      int index = RANDOM.nextInt(charset.length());
      code.append(charset.charAt(index));
    }
    return code.toString();
  }
  
  /**
   * Converts a BufferedImage to Minecraft map data format.
   *
   * @param image the image to convert
   * @return the map data as a byte array
   */
  private byte[] convertToMapData(BufferedImage image) {
    byte[] mapData = new byte[MAP_WIDTH * MAP_HEIGHT];
    
    for (int y = 0; y < MAP_HEIGHT; y++) {
      for (int x = 0; x < MAP_WIDTH; x++) {
        int rgb = image.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        // Convert RGB to grayscale
        int gray = (r + g + b) / 3;
        
        // Map grayscale value to Minecraft color index (simplified)
        // In a real implementation, you would map to the closest Minecraft color
        byte colorIndex;
        if (gray < 64) {
          colorIndex = 29; // Dark gray
        } else if (gray < 128) {
          colorIndex = 30; // Gray
        } else if (gray < 192) {
          colorIndex = 31; // Light gray
        } else {
          colorIndex = 34; // White
        }
        
        mapData[y * MAP_WIDTH + x] = colorIndex;
      }
    }
    
    return mapData;
  }
} 