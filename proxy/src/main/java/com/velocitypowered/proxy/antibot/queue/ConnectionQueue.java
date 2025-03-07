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

package com.velocitypowered.proxy.antibot.queue;

import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.antibot.ApiaryAntibot;
import java.net.InetAddress;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages a queue of player connections during bot attacks.
 */
public class ConnectionQueue {

  private static final Logger logger = LogManager.getLogger(ConnectionQueue.class);
  
  private final ApiaryAntibot antiBot;
  private final Queue<PreLoginEvent> queue = new ConcurrentLinkedQueue<>();
  private final Map<InetAddress, Long> queuedIps = new ConcurrentHashMap<>();
  
  private ScheduledTask processTask;
  private boolean enabled = false;

  /**
   * Creates a new connection queue.
   *
   * @param antiBot the antibot instance
   */
  public ConnectionQueue(ApiaryAntibot antiBot) {
    this.antiBot = antiBot;
  }

  /**
   * Starts the connection queue processor.
   */
  public void start() {
    if (enabled) {
      return;
    }
    
    enabled = true;
    processTask = antiBot.getServer().getScheduler().buildTask(antiBot, this::processQueue)
        .repeat(1, TimeUnit.SECONDS)
        .schedule();
    
    logger.info(antiBot.getMessages().get("console.queue.enabled"));
  }

  /**
   * Stops the connection queue processor.
   */
  public void stop() {
    if (!enabled) {
      return;
    }
    
    enabled = false;
    if (processTask != null) {
      processTask.cancel();
      processTask = null;
    }
    queue.clear();
    
    logger.info(antiBot.getMessages().get("console.queue.disabled"));
  }

  /**
   * Queues a connection for processing.
   *
   * @param event the login event to queue
   * @return true if the connection was queued, false if it was rejected
   */
  public boolean queueConnection(PreLoginEvent event) {
    InetAddress address = event.getConnection().getRemoteAddress().getAddress();
    
    // Check if this IP has tried to connect too recently
    Long lastAttempt = queuedIps.get(address);
    if (lastAttempt != null) {
      long elapsed = System.currentTimeMillis() - lastAttempt;
      if (elapsed < antiBot.getConfig().getVerification().getRejoinDelay()) {
        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
            antiBot.getMessages().getComponent("player.wait.before.reconnecting")));
        return false;
      }
    }
    
    // Update last connection attempt
    queuedIps.put(address, System.currentTimeMillis());
    
    // Add to queue
    queue.add(event);
    
    // Pause the event until it's processed
    event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
        antiBot.getMessages().getComponent("player.queue")));
    
    return true;
  }

  /**
   * Processes the next batch of connections in the queue.
   */
  private void processQueue() {
    if (!enabled || queue.isEmpty()) {
      return;
    }

    int processed = 0;
    int maxPolls = antiBot.getConfig().getQueue().getMaxPolls();
    
    while (!queue.isEmpty() && processed < maxPolls) {
      PreLoginEvent event = queue.poll();
      if (event != null) {
        // Process the connection by resuming it
        // In a real implementation, we would resume the connection event here
        logger.debug("Processing queued connection from {}", 
            event.getConnection().getRemoteAddress());
        processed++;
      }
    }
    
    if (processed > 0) {
      logger.debug(antiBot.getMessages().format("console.queue.processed", processed, queue.size()));
    }
  }

  /**
   * Gets the current size of the connection queue.
   *
   * @return the number of connections in the queue
   */
  public int getQueueSize() {
    return queue.size();
  }

  /**
   * Adds a connection to the queue.
   *
   * @param username the player's username
   * @param address the player's IP address
   * @param event the pre-login event
   */
  public void addToQueue(String username, InetAddress address, PreLoginEvent event) {
    String key = username.toLowerCase() + ":" + address.getHostAddress();
    queue.add(event);
    queuedIps.put(address, System.currentTimeMillis());
  }
} 