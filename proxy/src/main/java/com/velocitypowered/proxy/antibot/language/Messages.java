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

package com.velocitypowered.proxy.antibot.language;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * Manages messages for the AntiBot system.
 */
public class Messages {

  private static final Logger logger = LogManager.getLogger(Messages.class);
  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  
  private final Map<String, String> messages = new HashMap<>();
  private final Path configPath;
  
  private static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>();
  
  // Initialize default messages
  static {
    // Default messages for console
    DEFAULT_MESSAGES.put("console.prefix", "[AntiBot] ");
    DEFAULT_MESSAGES.put("console.enabled", "ApiaryAntibot protection enabled!");
    DEFAULT_MESSAGES.put("console.disabled", "ApiaryAntibot protection disabled");
    DEFAULT_MESSAGES.put("console.config.loaded", "Loaded ApiaryAntibot configuration");
    DEFAULT_MESSAGES.put("console.config.created", "Created default ApiaryAntibot configuration");
    DEFAULT_MESSAGES.put("console.config.error", "Failed to load ApiaryAntibot configuration");
    DEFAULT_MESSAGES.put("console.attack.detected", "Bot attack detected! {} connections/second");
    DEFAULT_MESSAGES.put("console.attack.ended", "Bot attack has ended");
    DEFAULT_MESSAGES.put("console.queue.enabled", "Connection queue enabled");
    DEFAULT_MESSAGES.put("console.queue.disabled", "Connection queue disabled");
    DEFAULT_MESSAGES.put("console.queue.processed", "Processed {} connections from queue. Remaining: {}");
    DEFAULT_MESSAGES.put("console.connection", "New connection from {} ({})");
    DEFAULT_MESSAGES.put("console.verification.rejoin.added", "Player {} added to rejoin verification");
    DEFAULT_MESSAGES.put("console.verification.rejoin.success", "Player {} passed rejoin verification");
    
    // Player-facing messages
    DEFAULT_MESSAGES.put("player.queue", "<yellow>Connection queued. Please wait...</yellow>");
    DEFAULT_MESSAGES.put("player.verification.failed", "<red>Verification failed: {0}</red>");
    DEFAULT_MESSAGES.put("player.invalid.username", "<red>Invalid username format</red>");
    DEFAULT_MESSAGES.put("player.invalid.clientbrand", "<red>Invalid client brand</red>");
    DEFAULT_MESSAGES.put("player.too.many.connections", "<red>Too many connections from your IP address</red>");
    DEFAULT_MESSAGES.put("player.blacklisted", "<red>Your IP address has been temporarily blacklisted</red>");
    DEFAULT_MESSAGES.put("player.wait.before.reconnecting", "<red>Please wait before reconnecting</red>");
    DEFAULT_MESSAGES.put("player.rejoin.required", "<yellow>Please reconnect to complete verification</yellow>");
    DEFAULT_MESSAGES.put("player.rejoin.expired", "<red>Verification expired. Please start over</red>");
  }

  /**
   * Creates a new Messages instance.
   *
   * @param configDir the directory where the language file is located
   */
  public Messages(Path configDir) {
    this.configPath = configDir.resolve("language.yml");
    load();
  }

  /**
   * Loads messages from the language file.
   */
  public void load() {
    try {
      if (!Files.exists(configPath)) {
        saveDefaultLanguageFile();
      }
      
      // Load existing file
      String content = Files.readString(configPath);
      Yaml yaml = new Yaml();
      Map<String, Object> loaded = yaml.load(content);
      
      // Reset to defaults
      messages.clear();
      messages.putAll(DEFAULT_MESSAGES);
      
      // Override with loaded values
      if (loaded != null) {
        loaded.forEach((key, value) -> {
          if (value instanceof String) {
            messages.put(key, (String) value);
          }
        });
      }
      
      logger.info("Loaded language file from {}", configPath);
    } catch (IOException e) {
      logger.error("Failed to load language file, using defaults", e);
      messages.putAll(DEFAULT_MESSAGES);
    }
  }

  /**
   * Saves the default language file.
   *
   * @throws IOException if an error occurs during saving
   */
  private void saveDefaultLanguageFile() throws IOException {
    StringBuilder yaml = new StringBuilder();
    yaml.append("# ApiaryAntibot Language Configuration\n");
    yaml.append("# This file contains all messages used by the antibot system\n\n");
    
    yaml.append("# Console messages\n");
    yaml.append("console:\n");
    
    // Group by prefix (console, player, etc.)
    Map<String, Map<String, String>> grouped = new HashMap<>();
    for (Map.Entry<String, String> entry : DEFAULT_MESSAGES.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      
      String prefix = key.substring(0, key.indexOf('.'));
      String suffix = key.substring(key.indexOf('.') + 1);
      
      grouped.computeIfAbsent(prefix, k -> new HashMap<>()).put(suffix, value);
    }
    
    // Write grouped messages
    for (Map.Entry<String, Map<String, String>> group : grouped.entrySet()) {
      yaml.append("  ").append(group.getKey()).append(":\n");
      
      for (Map.Entry<String, String> entry : group.getValue().entrySet()) {
        yaml.append("    ").append(entry.getKey()).append(": \"")
            .append(entry.getValue().replace("\"", "\\\""))
            .append("\"\n");
      }
      
      yaml.append("\n");
    }
    
    Files.writeString(configPath, yaml.toString());
    logger.info("Created default language file at {}", configPath);
  }

  /**
   * Gets a message by key.
   *
   * @param key the message key
   * @return the message, or the key if not found
   */
  public String get(String key) {
    return messages.getOrDefault(key, key);
  }

  /**
   * Gets a message by key and formats it with the given arguments.
   *
   * @param key the message key
   * @param args the arguments to format the message with
   * @return the formatted message
   */
  public String format(String key, Object... args) {
    String message = get(key);
    for (int i = 0; i < args.length; i++) {
      message = message.replace("{" + i + "}", String.valueOf(args[i]));
    }
    return message;
  }

  /**
   * Gets a message and converts it to a component using MiniMessage format.
   *
   * @param key the message key
   * @return the component
   */
  public Component getComponent(String key) {
    String message = get(key);
    if (message.contains("<") && message.contains(">")) {
      return MINI_MESSAGE.deserialize(message);
    } else {
      return Component.text(message);
    }
  }

  /**
   * Gets a message, formats it, and converts it to a component using MiniMessage format.
   *
   * @param key the message key
   * @param args the arguments to format the message with
   * @return the formatted component
   */
  public Component formatComponent(String key, Object... args) {
    String message = format(key, args);
    if (message.contains("<") && message.contains(">")) {
      return MINI_MESSAGE.deserialize(message);
    } else {
      return Component.text(message);
    }
  }
} 