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

package com.velocitypowered.proxy.antibot;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.antibot.captcha.CaptchaGenerator;
import com.velocitypowered.proxy.antibot.config.AntiBotConfig;
import com.velocitypowered.proxy.antibot.language.Messages;
import com.velocitypowered.proxy.antibot.queue.ConnectionQueue;
import com.velocitypowered.proxy.antibot.verification.VerificationManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * ApiaryAntibot integration for Velocity.
 * This is a custom fork based on jonesdevelopment's Sonar AntiBot.
 */
public class ApiaryAntibot {

  private final VelocityServer server;
  private final Logger logger;
  private final Path configDir;
  private final Path configPath;
  
  private AntiBotConfig config;
  private Messages messages;
  private boolean enabled = false;
  
  // Attack detection
  private boolean underAttack = false;
  private long lastAttackTime = 0;
  private final AtomicInteger connectionsPerSecond = new AtomicInteger(0);
  private final Map<InetAddress, Integer> connectionsByIp = new ConcurrentHashMap<>();
  private final Map<String, Pattern> validationPatterns = new ConcurrentHashMap<>();
  
  // Map to track players who need to reconnect (for double-join verification)
  private final Map<String, Long> rejoinVerificationCache = new ConcurrentHashMap<>();
  
  // Components
  private ConnectionQueue connectionQueue;
  private VerificationManager verificationManager;
  private ScheduledTask attackDetectionTask;
  private ScheduledTask rejoinCleanupTask;
  private CaptchaGenerator captchaGenerator;

  /**
   * Creates a new ApiaryAntibot instance.
   *
   * @param server the Velocity server
   */
  public ApiaryAntibot(VelocityServer server) {
    this.server = server;
    this.logger = LogManager.getLogger(ApiaryAntibot.class);
    this.configDir = Path.of("antibot");
    this.configPath = configDir.resolve("config.yml");
    
    try {
      if (!Files.exists(configDir)) {
        Files.createDirectories(configDir);
        logger.info("Created antibot directory");
      }
      
      // Initialize messages first
      this.messages = new Messages(configDir);
      messages.load();
      
      // Then load config
      loadConfig();
    } catch (IOException e) {
      logger.error("Failed to create antibot directory", e);
    }
  }

  /**
   * Saves the default configuration file.
   *
   * @throws IOException if an error occurs during file creation
   */
  private void saveDefaultConfig() throws IOException {
    if (!Files.exists(configPath)) {
      // Copy default config
      try (InputStream in = 
          ApiaryAntibot.class.getResourceAsStream("/antibot/default-config.yml")) {
        if (in != null) {
          Files.copy(in, configPath);
          logger.info("Created default ApiaryAntibot configuration");
        }
      }
    }
  }

  /**
   * Loads the configuration from disk.
   */
  private void loadConfig() {
    try {
      if (Files.exists(configPath)) {
        try (InputStream is = Files.newInputStream(configPath)) {
          Yaml yaml = new Yaml();
          Map<String, Object> map = yaml.load(is);
          config = AntiBotConfig.fromMap(map);
        }
      } else {
        // Config doesn't exist, create it
        saveDefaultConfig();
        
        // Load the default config
        try (InputStream is = Files.newInputStream(configPath)) {
          Yaml yaml = new Yaml();
          Map<String, Object> map = yaml.load(is);
          config = AntiBotConfig.fromMap(map);
        }
      }
      
      // Apply the enabled state from the config
      enabled = config.isEnabled();
      
      logger.info("Loaded configuration from {}", 
          configPath.toAbsolutePath());
      
      // Compile regex patterns for validation
      validationPatterns.put("username", Pattern.compile(config.getVerification().getValidNameRegex()));
      validationPatterns.put("locale", Pattern.compile(config.getVerification().getValidLocaleRegex()));
      validationPatterns.put("brand", Pattern.compile(
          config.getVerification().getClientBrand().getValidRegex()));
    } catch (Exception e) {
      logger.error("Failed to load configuration", e);
      
      // Use default config
      config = new AntiBotConfig();
    }
  }

  /**
   * Enables the ApiaryAntibot system.
   */
  public void enable() {
    if (enabled) {
      return;
    }

    if (!server.getConfiguration().getAntiBot().isEnabled()) {
      logger.info("ApiaryAntibot is disabled in velocity.toml. Skipping initialization.");
      return;
    }

    enabled = true;

    // Compile validation patterns
    validationPatterns.put("username", Pattern.compile(config.getVerification().getValidNameRegex()));
    validationPatterns.put("locale", Pattern.compile(config.getVerification().getValidLocaleRegex()));
    validationPatterns.put("brand", Pattern.compile(
        config.getVerification().getClientBrand().getValidRegex()));

    // Start connection queue
    connectionQueue = new ConnectionQueue(this);
    connectionQueue.start();
    
    // Start verification manager
    verificationManager = new VerificationManager(this);
    verificationManager.start();
    
    // Start attack detection
    startAttackDetection();
    
    // Start rejoin cache cleanup
    startRejoinCleanupTask();

    // Initialize CAPTCHA generator if enabled
    if (config.getVerification().getMapCaptcha().isEnabled()) {
      String alphabet = config.getVerification().getMapCaptcha().getAlphabet();
      int precompute = config.getVerification().getMapCaptcha().getPrecompute();
      String background = config.getVerification().getMapCaptcha().getBackground();
      
      // Create a path to the background image if specified
      Path backgroundPath = null;
      if (background != null && !background.isEmpty()) {
        backgroundPath = configDir.resolve(background);
        if (!Files.exists(backgroundPath)) {
          logger.warn("CAPTCHA background image not found: {}", backgroundPath);
          backgroundPath = null;
        }
      }
      
      // Initialize the CAPTCHA generator
      captchaGenerator = new CaptchaGenerator(alphabet, 5, backgroundPath);
      
      // Precompute CAPTCHAs in the background
      server.getScheduler().buildTask(this, () -> {
        logger.info("Precomputing {} CAPTCHAs...", precompute);
        captchaGenerator.generateCaptchas(precompute);
        logger.info("Finished precomputing CAPTCHAs");
      }).schedule();
    }

    if (messages != null) {
      logger.info(messages.get("console.enabled"));
    } else {
      logger.info("ApiaryAntibot protection enabled!");
    }
  }

  /**
   * Disables the ApiaryAntibot system.
   */
  public void disable() {
    if (!enabled) {
      return;
    }
    
    enabled = false;
    
    // Stop connection queue
    if (connectionQueue != null) {
      connectionQueue.stop();
      connectionQueue = null;
    }
    
    // Stop verification manager
    if (verificationManager != null) {
      verificationManager.stop();
      verificationManager = null;
    }
    
    // Stop attack detection
    if (attackDetectionTask != null) {
      attackDetectionTask.cancel();
      attackDetectionTask = null;
    }
    
    // Stop rejoin cleanup task
    if (rejoinCleanupTask != null) {
      rejoinCleanupTask.cancel();
      rejoinCleanupTask = null;
    }
    
    // Clear all data
    validationPatterns.clear();
    connectionsByIp.clear();
    connectionsPerSecond.set(0);
    underAttack = false;
    
    // Clear rejoin cache
    rejoinVerificationCache.clear();
    
    // Clear CAPTCHA generator
    captchaGenerator = null;
    
    if (messages != null) {
      logger.info(messages.get("console.disabled"));
    } else {
      logger.info("ApiaryAntibot protection disabled");
    }
  }
  
  /**
   * Starts the attack detection system.
   */
  private void startAttackDetection() {
    attackDetectionTask = server.getScheduler().buildTask(this, () -> {
      // Reset connections per second counter every second
      int connections = connectionsPerSecond.getAndSet(0);
      
      // Check if we're under attack
      if (connections >= config.getAttackTracker().getMinPlayersForAttack()) {
        if (!underAttack) {
          underAttack = true;
          lastAttackTime = System.currentTimeMillis();
          if (messages != null) {
            logger.warn(messages.format("console.attack.detected", connections));
          } else {
            logger.warn("Bot attack detected! {} connections/second", connections);
          }
        }
      } else if (underAttack && System.currentTimeMillis() - lastAttackTime > config.getAttackTracker().getMinAttackDuration()) {
        underAttack = false;
        if (messages != null) {
          logger.info(messages.get("console.attack.ended"));
        } else {
          logger.info("Bot attack has ended");
        }
      }
    }).repeat(1, TimeUnit.SECONDS).schedule();
  }

  /**
   * Handles proxy initialization.
   *
   * @param event the initialization event
   */
  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    enable();
  }

  /**
   * Handles proxy shutdown.
   *
   * @param event the shutdown event
   */
  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    disable();
  }

  /**
   * Handles pre-login events.
   *
   * @param event the pre-login event
   */
  @Subscribe(order = PostOrder.FIRST)
  public void onPreLogin(PreLoginEvent event) {
    if (!enabled) {
      return;
    }
    
    // Get player info
    String username = event.getUsername();
    InetAddress address = event.getConnection().getRemoteAddress().getAddress();
    
    // Check if player is rejoining for verification
    if (isRejoining(username, address)) {
      // Player is rejoining, remove from cache and allow
      removeFromRejoinVerification(username, address);
      return;
    }
    
    // Check if username is valid
    if (!validationPatterns.get("username").matcher(username).matches()) {
      // Invalid username
      Component kickMessage = messages != null 
          ? messages.getComponent("kick.invalid-name") 
          : Component.text("Invalid username");
      
      event.setResult(PreLoginEvent.PreLoginComponentResult.denied(kickMessage));
      
      if (messages != null) {
        logger.debug(messages.format("console.connection.invalid-name", username, address.getHostAddress()));
      } else {
        logger.debug("Player {} ({}) was disconnected: invalid username", 
            username, address.getHostAddress());
      }
      return;
    }
    
    // Check if IP has too many connections
    int currentConnections = connectionsByIp.getOrDefault(address, 0);
    int maxConnections = config.getGeneral().getMaxOnlinePerIp();
    
    if (currentConnections >= maxConnections) {
      // Too many connections from this IP
      Component kickMessage = messages != null 
          ? messages.getComponent("kick.ip-limit") 
          : Component.text("Too many connections from your IP address");
      
      event.setResult(PreLoginEvent.PreLoginComponentResult.denied(kickMessage));
      
      if (messages != null) {
        logger.debug(messages.format("console.connection.ip-limit", username, address.getHostAddress()));
      } else {
        logger.debug("Player {} ({}) was disconnected: too many connections", 
            username, address.getHostAddress());
      }
      return;
    }
    
    // Check if under attack and should queue
    if (underAttack) {
      // Add to queue
      connectionQueue.queueConnection(event);
      
      // Increment connections per second counter
      connectionsPerSecond.incrementAndGet();
      
      // Update connections by IP
      connectionsByIp.put(address, currentConnections + 1);
      
      // Event is already handled by queueConnection
      return;
    }
    
    // Not under attack, proceed normally
    // Increment connections per second counter
    connectionsPerSecond.incrementAndGet();
    
    // Update connections by IP
    connectionsByIp.put(address, currentConnections + 1);
    
    // Check if force rejoin is enabled
    if (config.getVerification().isForceRejoin()) {
      // Add to rejoin verification
      addToRejoinVerification(username, address);
      
      Component kickMessage = messages != null 
          ? messages.getComponent("kick.rejoin") 
          : Component.text("Please reconnect to verify.");
      
      event.setResult(PreLoginEvent.PreLoginComponentResult.denied(kickMessage));
      return;
    }
    
    // Begin verification if needed
    if (verificationManager.beginVerification(event)) {
      return; // Verification started, event handled
    }
    
    // No verification needed, allow connection
  }
  
  /**
   * Gets the proxy server instance.
   *
   * @return the server instance
   */
  public VelocityServer getServer() {
    return server;
  }
  
  /**
   * Gets the config directory.
   *
   * @return the config directory
   */
  public Path getConfigDir() {
    return configDir;
  }
  
  /**
   * Gets the messages manager.
   *
   * @return the messages manager
   */
  public Messages getMessages() {
    return messages;
  }
  
  /**
   * Checks if the server is currently under attack.
   *
   * @return true if under attack, false otherwise
   */
  public boolean isUnderAttack() {
    return underAttack;
  }
  
  /**
   * Gets the connection queue.
   *
   * @return the connection queue
   */
  public ConnectionQueue getConnectionQueue() {
    return connectionQueue;
  }
  
  /**
   * Gets the verification manager.
   *
   * @return the verification manager
   */
  public VerificationManager getVerificationManager() {
    return verificationManager;
  }
  
  /**
   * Gets the antibot configuration.
   *
   * @return the configuration
   */
  public AntiBotConfig getConfig() {
    return config;
  }

  /**
   * Checks if a player needs to be verified using the rejoin system.
   * This method is used to determine if a player should be verified based on previous connections.
   *
   * @param username the player's username
   * @param address the player's IP address
   * @return true if the player needs to be verified, false otherwise
   */
  public boolean needsRejoinVerification(String username, InetAddress address) {
    if (!config.getVerification().isForceRejoin()) {
      return false;
    }
    
    String key = username.toLowerCase() + ":" + address.getHostAddress();
    return !rejoinVerificationCache.containsKey(key);
  }

  /**
   * Adds a player to the rejoin verification cache.
   * This method is used to track players who need to reconnect for verification.
   *
   * @param username the player's username
   * @param address the player's IP address
   */
  public void addToRejoinVerification(String username, InetAddress address) {
    if (!config.getVerification().isForceRejoin()) {
      return;
    }
    
    String key = username.toLowerCase() + ":" + address.getHostAddress();
    rejoinVerificationCache.put(key, System.currentTimeMillis());
  }

  /**
   * Checks if a player is rejoining for verification.
   * This method verifies if a player is reconnecting as part of the verification process.
   *
   * @param username the player's username
   * @param address the player's IP address
   * @return true if the player is rejoining, false otherwise
   */
  public boolean isRejoining(String username, InetAddress address) {
    String key = username.toLowerCase() + ":" + address.getHostAddress();
    return rejoinVerificationCache.containsKey(key);
  }

  /**
   * Starts the rejoin cache cleanup task.
   * This task periodically removes expired entries from the rejoin verification cache.
   */
  private void startRejoinCleanupTask() {
    if (rejoinCleanupTask != null) {
      rejoinCleanupTask.cancel();
    }
    
    rejoinCleanupTask = server.getScheduler().buildTask(this, () -> {
      long now = System.currentTimeMillis();
      long maxAge = config.getVerification().getRejoinValidTime();
      
      rejoinVerificationCache.entrySet().removeIf(entry -> 
          now - entry.getValue() > maxAge);
          
    }).repeat(30, TimeUnit.SECONDS).schedule();
  }
  
  /**
   * Removes a player from the rejoin verification cache.
   * This method is called when a player no longer needs to be tracked for rejoin verification.
   *
   * @param username the player's username
   * @param address the player's IP address
   */
  public void removeFromRejoinVerification(String username, InetAddress address) {
    String key = username.toLowerCase() + ":" + address.getHostAddress();
    rejoinVerificationCache.remove(key);
  }

  /**
   * Gets the CAPTCHA generator.
   *
   * @return the CAPTCHA generator
   */
  public CaptchaGenerator getCaptchaGenerator() {
    return captchaGenerator;
  }
} 