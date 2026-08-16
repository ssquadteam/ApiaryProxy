/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the entity ID the client currently uses for its own player, so a coordinating proxy
 * plugin can pass it to the destination server for a seamless, world-preserving switch.
 */
public final class ClientWorldSwitches {

  private static final ConcurrentHashMap<UUID, Integer> CLIENT_ENTITY_IDS = new ConcurrentHashMap<>();

  private ClientWorldSwitches() {
  }

  /**
   * Returns the entity ID the client currently uses for {@code playerId}, or {@code 0} when the
   * player has not finished their initial join.
   *
   * @param playerId the player whose client entity ID is needed
   * @return the client-visible entity ID, or {@code 0}
   */
  public static int clientEntityId(UUID playerId) {
    return CLIENT_ENTITY_IDS.getOrDefault(playerId, 0);
  }

  /**
   * Records the entity ID most recently presented to a client.
   *
   * <p>This method is used by ApiaryProxy's connection implementation. Coordinating plugins
   * should use {@link #clientEntityId(UUID)} instead.
   *
   * @param playerId the client that received the ID
   * @param entityId the entity ID from its join-game packet
   */
  public static void rememberClientEntityId(UUID playerId, int entityId) {
    if (entityId > 0) {
      CLIENT_ENTITY_IDS.put(playerId, entityId);
    }
  }

  /**
   * Clears all switch state for a disconnected player.
   *
   * <p>This method is used by ApiaryProxy's connection implementation.
   *
   * @param playerId the disconnected player
   */
  public static void forget(UUID playerId) {
    CLIENT_ENTITY_IDS.remove(playerId);
  }
}