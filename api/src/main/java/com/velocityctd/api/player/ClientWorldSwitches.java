/*
 * Copyright (C) 2026 Velocity-CTD Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocityctd.api.player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates an explicitly requested client-world-preserving server switch.
 *
 * <p>A coordinating proxy plugin requests preservation before it starts staging a destination.
 * ApiaryProxy consumes that one request when the destination sends its join-game packet. Requests
 * expire quickly so a failed connection cannot affect a later, normal server switch.
 */
public final class ClientWorldSwitches {

  private static final long REQUEST_TTL_MILLIS = 5_000L;
  private static final ConcurrentHashMap<UUID, Integer> CLIENT_ENTITY_IDS = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<UUID, Long> PRESERVATION_REQUESTS = new ConcurrentHashMap<>();

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
   * Requests that the next destination join for {@code playerId} retain the client's loaded world.
   *
   * <p>The request only takes effect when ApiaryProxy's world-preservation setting is enabled and
   * the destination reports the same dimension. It intentionally does not apply to ordinary
   * server switches.
   *
   * @param playerId the player being transferred
   * @return {@code true} when a request was recorded; {@code false} before the initial join
   */
  public static boolean requestWorldPreservation(UUID playerId) {
    if (clientEntityId(playerId) <= 0) {
      return false;
    }

    PRESERVATION_REQUESTS.put(playerId, System.currentTimeMillis() + REQUEST_TTL_MILLIS);
    return true;
  }

  /**
   * Cancels a previously requested world-preserving switch.
   *
   * @param playerId the player whose pending request should be cancelled
   */
  public static void cancelWorldPreservation(UUID playerId) {
    PRESERVATION_REQUESTS.remove(playerId);
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
    PRESERVATION_REQUESTS.remove(playerId);
  }

  /**
   * Consumes a pending request if it has not expired.
   *
   * <p>This method is used by ApiaryProxy's connection implementation.
   *
   * @param playerId the player whose request should be consumed
   * @return whether a live request existed
   */
  public static boolean consumeWorldPreservation(UUID playerId) {
    Long deadline = PRESERVATION_REQUESTS.remove(playerId);
    return deadline != null && System.currentTimeMillis() <= deadline;
  }
}
