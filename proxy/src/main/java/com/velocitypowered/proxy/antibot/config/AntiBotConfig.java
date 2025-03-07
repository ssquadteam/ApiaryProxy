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

package com.velocitypowered.proxy.antibot.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Configuration for ApiaryAntibot system.
 * This is a custom fork based on Sonar AntiBot.
 */
public class AntiBotConfig {

  private boolean enabled = true;
  private GeneralConfig general = new GeneralConfig();
  private AttackTrackerConfig attackTracker = new AttackTrackerConfig();
  private QueueConfig queue = new QueueConfig();
  private VerificationConfig verification = new VerificationConfig();

  /**
   * Loads the configuration from the given path.
   *
   * @param path the path to load from
   * @return the loaded configuration
   * @throws IOException if an error occurs during loading
   */
  public static AntiBotConfig load(Path path) throws IOException {
    Yaml yaml = new Yaml();
    String content = Files.readString(path);
    Map<String, Object> rawConfig = yaml.load(content);
    
    AntiBotConfig config = new AntiBotConfig();
    config.enabled = true; // Default to enabled
    
    if (rawConfig == null) {
      return config;
    }
    
    // Load general config
    if (rawConfig.containsKey("general")) {
      Map<String, Object> generalMap = (Map<String, Object>) rawConfig.get("general");
      config.general = GeneralConfig.fromMap(generalMap);
    }
    
    // Load attack tracker config
    if (rawConfig.containsKey("attack-tracker")) {
      Map<String, Object> attackTrackerMap = (Map<String, Object>) rawConfig.get("attack-tracker");
      config.attackTracker = AttackTrackerConfig.fromMap(attackTrackerMap);
    }
    
    // Load queue config
    if (rawConfig.containsKey("queue")) {
      Map<String, Object> queueMap = (Map<String, Object>) rawConfig.get("queue");
      config.queue = QueueConfig.fromMap(queueMap);
    }
    
    // Load verification config
    if (rawConfig.containsKey("verification")) {
      Map<String, Object> verificationMap = (Map<String, Object>) rawConfig.get("verification");
      config.verification = VerificationConfig.fromMap(verificationMap);
    }
    
    return config;
  }
  
  /**
   * Creates a configuration from a map of values.
   *
   * @param map the map of values
   * @return the configuration
   */
  public static AntiBotConfig fromMap(Map<String, Object> map) {
    AntiBotConfig config = new AntiBotConfig();
    if (map == null) {
      return config;
    }
    
    if (map.containsKey("enabled")) {
      config.enabled = (boolean) map.get("enabled");
    }
    
    if (map.containsKey("general")) {
      Map<String, Object> generalMap = (Map<String, Object>) map.get("general");
      config.general = GeneralConfig.fromMap(generalMap);
    }
    
    if (map.containsKey("attack-tracker")) {
      Map<String, Object> attackTrackerMap = (Map<String, Object>) map.get("attack-tracker");
      config.attackTracker = AttackTrackerConfig.fromMap(attackTrackerMap);
    }
    
    if (map.containsKey("queue")) {
      Map<String, Object> queueMap = (Map<String, Object>) map.get("queue");
      config.queue = QueueConfig.fromMap(queueMap);
    }
    
    if (map.containsKey("verification")) {
      Map<String, Object> verificationMap = (Map<String, Object>) map.get("verification");
      config.verification = VerificationConfig.fromMap(verificationMap);
    }
    
    return config;
  }
  
  public boolean isEnabled() {
    return enabled;
  }
  
  public GeneralConfig getGeneral() {
    return general;
  }
  
  public AttackTrackerConfig getAttackTracker() {
    return attackTracker;
  }
  
  public QueueConfig getQueue() {
    return queue;
  }
  
  public VerificationConfig getVerification() {
    return verification;
  }
  
  /**
   * Configuration for general settings.
   */
  public static class GeneralConfig {
    private boolean checkForUpdates = true;
    private boolean logPlayerAddresses = true;
    private int maxOnlinePerIp = 3;
    
    /**
     * Creates a GeneralConfig from a configuration map.
     *
     * @param map the map containing configuration values
     * @return the populated GeneralConfig instance
     */
    public static GeneralConfig fromMap(Map<String, Object> map) {
      GeneralConfig config = new GeneralConfig();
      if (map.containsKey("check-for-updates")) {
        config.checkForUpdates = (boolean) map.get("check-for-updates");
      }
      if (map.containsKey("log-player-addresses")) {
        config.logPlayerAddresses = (boolean) map.get("log-player-addresses");
      }
      if (map.containsKey("max-online-per-ip")) {
        config.maxOnlinePerIp = (int) map.get("max-online-per-ip");
      }
      return config;
    }
    
    public boolean isCheckForUpdates() {
      return checkForUpdates;
    }
    
    public boolean isLogPlayerAddresses() {
      return logPlayerAddresses;
    }
    
    public int getMaxOnlinePerIp() {
      return maxOnlinePerIp;
    }
  }
  
  /**
   * Configuration for attack tracker settings.
   */
  public static class AttackTrackerConfig {
    private int minPlayersForAttack = 8;
    private int minAttackDuration = 30000;
    private int minAttackThreshold = 2;
    private int attackCooldownDelay = 3000;
    
    /**
     * Creates a configuration from a map of values.
     *
     * @param map the map of values
     * @return the configuration
     */
    public static AttackTrackerConfig fromMap(Map<String, Object> map) {
      AttackTrackerConfig config = new AttackTrackerConfig();
      if (map == null) {
        return config;
      }
      
      if (map.containsKey("min-players-for-attack")) {
        config.minPlayersForAttack = (int) map.get("min-players-for-attack");
      }
      
      if (map.containsKey("min-attack-duration")) {
        config.minAttackDuration = (int) map.get("min-attack-duration");
      }
      
      if (map.containsKey("min-attack-threshold")) {
        config.minAttackThreshold = (int) map.get("min-attack-threshold");
      }
      
      if (map.containsKey("attack-cooldown-delay")) {
        config.attackCooldownDelay = (int) map.get("attack-cooldown-delay");
      }
      
      return config;
    }
    
    public int getMinPlayersForAttack() {
      return minPlayersForAttack;
    }
    
    public int getMinAttackDuration() {
      return minAttackDuration;
    }
    
    public int getMinAttackThreshold() {
      return minAttackThreshold;
    }
    
    public int getAttackCooldownDelay() {
      return attackCooldownDelay;
    }
  }
  
  /**
   * Configuration for connection queue settings.
   */
  public static class QueueConfig {
    private int maxPolls = 30;
    
    /**
     * Creates a configuration from a map of values.
     *
     * @param map the map of values
     * @return the configuration
     */
    public static QueueConfig fromMap(Map<String, Object> map) {
      QueueConfig config = new QueueConfig();
      if (map == null) {
        return config;
      }
      
      if (map.containsKey("max-polls")) {
        config.maxPolls = (int) map.get("max-polls");
      }
      
      return config;
    }
    
    public int getMaxPolls() {
      return maxPolls;
    }
  }
  
  /**
   * Configuration for verification settings.
   */
  public static class VerificationConfig {
    private String timing = "ALWAYS";
    private GravityConfig gravity = new GravityConfig();
    private CollisionConfig collision = new CollisionConfig();
    private VehicleConfig vehicle = new VehicleConfig();
    private MapCaptchaConfig mapCaptcha = new MapCaptchaConfig();
    private ClientBrandConfig clientBrand = new ClientBrandConfig();
    private String validNameRegex = "^[a-zA-Z0-9_]+$";
    private String validLocaleRegex = "^[a-zA-Z_]+$";
    private String gamemode = "ADVENTURE";
    private int timeOfDay = 1000;
    private String cachedUsername = "ApiaryAntibot";
    private boolean logConnections = true;
    private boolean logDuringAttack = false;
    private boolean debugXyzPositions = false;
    private boolean checkGeyserPlayers = true;
    private int readTimeout = 8000;
    private int writeTimeout = 10000;
    private int rejoinDelay = 5000;
    private int rememberTime = 120000;
    private int blacklistTime = 600000;
    private int blacklistThreshold = 2;
    private List<Integer> blacklistedProtocols = List.of();
    
    private boolean forceRejoin = false;
    private int rejoinValidTime = 60000; // 1 minute by default
    
    /**
     * Creates a configuration from a map of values.
     *
     * @param map the map of values
     * @return the configuration
     */
    public static VerificationConfig fromMap(Map<String, Object> map) {
      VerificationConfig config = new VerificationConfig();
      if (map == null) {
        return config;
      }
      
      if (map.containsKey("timing")) {
        config.timing = (String) map.get("timing");
      }
      
      if (map.containsKey("checks")) {
        Map<String, Object> checks = (Map<String, Object>) map.get("checks");
        
        // Gravity checks
        if (checks.containsKey("gravity")) {
          Map<String, Object> gravityMap = (Map<String, Object>) checks.get("gravity");
          config.gravity = GravityConfig.fromMap(gravityMap);
        }
        
        // Collision checks
        if (checks.containsKey("collision")) {
          Map<String, Object> collisionMap = (Map<String, Object>) checks.get("collision");
          config.collision = CollisionConfig.fromMap(collisionMap);
        }
        
        // Vehicle checks
        if (checks.containsKey("vehicle")) {
          Map<String, Object> vehicleMap = (Map<String, Object>) checks.get("vehicle");
          config.vehicle = VehicleConfig.fromMap(vehicleMap);
        }
        
        // Map CAPTCHA configuration
        if (checks.containsKey("map-captcha")) {
          Map<String, Object> captchaMap = (Map<String, Object>) checks.get("map-captcha");
          config.mapCaptcha = MapCaptchaConfig.fromMap(captchaMap);
        }
        
        // Client brand validation
        if (checks.containsKey("client-brand")) {
          Map<String, Object> clientBrandMap = (Map<String, Object>) checks.get("client-brand");
          config.clientBrand = ClientBrandConfig.fromMap(clientBrandMap);
        }
        
        // Username regex
        if (checks.containsKey("valid-name-regex")) {
          config.validNameRegex = (String) checks.get("valid-name-regex");
        }
        
        // Locale regex
        if (checks.containsKey("valid-locale-regex")) {
          config.validLocaleRegex = (String) checks.get("valid-locale-regex");
        }
      }
      
      if (map.containsKey("gamemode")) {
        config.gamemode = (String) map.get("gamemode");
      }
      
      if (map.containsKey("time-of-day")) {
        config.timeOfDay = (int) map.get("time-of-day");
      }
      
      if (map.containsKey("cached-username")) {
        config.cachedUsername = (String) map.get("cached-username");
      }
      
      if (map.containsKey("log-connections")) {
        config.logConnections = (boolean) map.get("log-connections");
      }
      
      if (map.containsKey("log-during-attack")) {
        config.logDuringAttack = (boolean) map.get("log-during-attack");
      }
      
      if (map.containsKey("debug-xyz-positions")) {
        config.debugXyzPositions = (boolean) map.get("debug-xyz-positions");
      }
      
      if (map.containsKey("check-geyser-players")) {
        config.checkGeyserPlayers = (boolean) map.get("check-geyser-players");
      }
      
      if (map.containsKey("read-timeout")) {
        config.readTimeout = (int) map.get("read-timeout");
      }
      
      if (map.containsKey("write-timeout")) {
        config.writeTimeout = (int) map.get("write-timeout");
      }
      
      if (map.containsKey("rejoin-delay")) {
        config.rejoinDelay = (int) map.get("rejoin-delay");
      }
      
      if (map.containsKey("force-rejoin")) {
        config.forceRejoin = (boolean) map.get("force-rejoin");
      }
      
      if (map.containsKey("rejoin-valid-time")) {
        config.rejoinValidTime = (int) map.get("rejoin-valid-time");
      }
      
      if (map.containsKey("remember-time")) {
        config.rememberTime = (int) map.get("remember-time");
      }
      
      if (map.containsKey("blacklist-time")) {
        config.blacklistTime = (int) map.get("blacklist-time");
      }
      
      if (map.containsKey("blacklist-threshold")) {
        config.blacklistThreshold = (int) map.get("blacklist-threshold");
      }
      
      if (map.containsKey("blacklisted-protocols")) {
        config.blacklistedProtocols = (List<Integer>) map.get("blacklisted-protocols");
      }
      
      return config;
    }
    
    public String getTiming() {
      return timing;
    }
    
    public GravityConfig getGravity() {
      return gravity;
    }
    
    public CollisionConfig getCollision() {
      return collision;
    }
    
    public VehicleConfig getVehicle() {
      return vehicle;
    }
    
    public MapCaptchaConfig getMapCaptcha() {
      return mapCaptcha;
    }
    
    public ClientBrandConfig getClientBrand() {
      return clientBrand;
    }
    
    public String getValidNameRegex() {
      return validNameRegex;
    }
    
    public String getValidLocaleRegex() {
      return validLocaleRegex;
    }
    
    public String getGamemode() {
      return gamemode;
    }
    
    public int getTimeOfDay() {
      return timeOfDay;
    }
    
    public String getCachedUsername() {
      return cachedUsername;
    }
    
    public boolean isLogConnections() {
      return logConnections;
    }
    
    public boolean isLogDuringAttack() {
      return logDuringAttack;
    }
    
    public boolean isDebugXyzPositions() {
      return debugXyzPositions;
    }
    
    public boolean isCheckGeyserPlayers() {
      return checkGeyserPlayers;
    }
    
    public int getReadTimeout() {
      return readTimeout;
    }
    
    public int getWriteTimeout() {
      return writeTimeout;
    }
    
    public int getRejoinDelay() {
      return rejoinDelay;
    }
    
    public boolean isForceRejoin() {
      return forceRejoin;
    }
    
    public int getRejoinValidTime() {
      return rejoinValidTime;
    }
    
    public int getRememberTime() {
      return rememberTime;
    }
    
    public int getBlacklistTime() {
      return blacklistTime;
    }
    
    public int getBlacklistThreshold() {
      return blacklistThreshold;
    }
    
    public List<Integer> getBlacklistedProtocols() {
      return blacklistedProtocols;
    }
    
    /**
     * Configuration for gravity verification settings.
     */
    public static class GravityConfig {
      private boolean enabled = true;
      private boolean captchaOnFail = false;
      private int maxMovementTicks = 8;
      
      /**
       * Creates a configuration from a map of values.
       *
       * @param map the map of values
       * @return the configuration
       */
      public static GravityConfig fromMap(Map<String, Object> map) {
        GravityConfig config = new GravityConfig();
        if (map == null) {
          return config;
        }
        
        if (map.containsKey("enabled")) {
          config.enabled = (boolean) map.get("enabled");
        }
        
        if (map.containsKey("captcha-on-fail")) {
          config.captchaOnFail = (boolean) map.get("captcha-on-fail");
        }
        
        if (map.containsKey("max-movement-ticks")) {
          config.maxMovementTicks = (int) map.get("max-movement-ticks");
        }
        
        return config;
      }
      
      public boolean isEnabled() {
        return enabled;
      }
      
      public boolean isCaptchaOnFail() {
        return captchaOnFail;
      }
      
      public int getMaxMovementTicks() {
        return maxMovementTicks;
      }
    }
    
    /**
     * Configuration for collision verification settings.
     */
    public static class CollisionConfig {
      private boolean enabled = true;
      
      /**
       * Creates a configuration from a map of values.
       *
       * @param map the map of values
       * @return the configuration
       */
      public static CollisionConfig fromMap(Map<String, Object> map) {
        CollisionConfig config = new CollisionConfig();
        if (map == null) {
          return config;
        }
        
        if (map.containsKey("enabled")) {
          config.enabled = (boolean) map.get("enabled");
        }
        
        return config;
      }
      
      public boolean isEnabled() {
        return enabled;
      }
    }
    
    /**
     * Configuration for vehicle verification settings.
     */
    public static class VehicleConfig {
      private boolean enabled = true;
      private int minimumPackets = 2;
      
      /**
       * Creates a configuration from a map of values.
       *
       * @param map the map of values
       * @return the configuration
       */
      public static VehicleConfig fromMap(Map<String, Object> map) {
        VehicleConfig config = new VehicleConfig();
        if (map == null) {
          return config;
        }
        
        if (map.containsKey("enabled")) {
          config.enabled = (boolean) map.get("enabled");
        }
        
        if (map.containsKey("minimum-packets")) {
          config.minimumPackets = (int) map.get("minimum-packets");
        }
        
        return config;
      }
      
      public boolean isEnabled() {
        return enabled;
      }
      
      public int getMinimumPackets() {
        return minimumPackets;
      }
    }
    
    /**
     * Configuration for map-based CAPTCHA verification settings.
     */
    public static class MapCaptchaConfig {
      private String timing = "NEVER";
      private String background = "";
      private int precompute = 500;
      private int maxDuration = 30000;
      private int maxTries = 3;
      private String alphabet = "abcdefhjkmnoprstuxyz";
      
      /**
       * Creates a configuration from a map of values.
       *
       * @param map the map of values
       * @return the configuration
       */
      public static MapCaptchaConfig fromMap(Map<String, Object> map) {
        MapCaptchaConfig config = new MapCaptchaConfig();
        if (map == null) {
          return config;
        }
        
        if (map.containsKey("timing")) {
          config.timing = (String) map.get("timing");
        }
        
        if (map.containsKey("background")) {
          config.background = (String) map.get("background");
        }
        
        if (map.containsKey("precompute")) {
          config.precompute = (int) map.get("precompute");
        }
        
        if (map.containsKey("max-duration")) {
          config.maxDuration = (int) map.get("max-duration");
        }
        
        if (map.containsKey("max-tries")) {
          config.maxTries = (int) map.get("max-tries");
        }
        
        if (map.containsKey("alphabet")) {
          config.alphabet = (String) map.get("alphabet");
        }
        
        return config;
      }
      
      public String getTiming() {
        return timing;
      }
      
      public String getBackground() {
        return background;
      }
      
      public int getPrecompute() {
        return precompute;
      }
      
      public int getMaxDuration() {
        return maxDuration;
      }
      
      public int getMaxTries() {
        return maxTries;
      }
      
      public String getAlphabet() {
        return alphabet;
      }
      
      /**
       * Checks if the CAPTCHA is enabled.
       *
       * @return true if the CAPTCHA is enabled, false otherwise
       */
      public boolean isEnabled() {
        return !timing.equalsIgnoreCase("NEVER");
      }
    }
    
    /**
     * Configuration for client brand verification settings.
     */
    public static class ClientBrandConfig {
      private boolean enabled = true;
      private String validRegex = "^[!-~ ]+$";
      private int maxLength = 64;
      
      /**
       * Creates a configuration from a map of values.
       *
       * @param map the map of values
       * @return the configuration
       */
      public static ClientBrandConfig fromMap(Map<String, Object> map) {
        ClientBrandConfig config = new ClientBrandConfig();
        if (map == null) {
          return config;
        }
        
        if (map.containsKey("enabled")) {
          config.enabled = (boolean) map.get("enabled");
        }
        
        if (map.containsKey("valid-regex")) {
          config.validRegex = (String) map.get("valid-regex");
        }
        
        if (map.containsKey("max-length")) {
          config.maxLength = (int) map.get("max-length");
        }
        
        return config;
      }
      
      public boolean isEnabled() {
        return enabled;
      }
      
      public String getValidRegex() {
        return validRegex;
      }
      
      public int getMaxLength() {
        return maxLength;
      }
    }
  }
} 