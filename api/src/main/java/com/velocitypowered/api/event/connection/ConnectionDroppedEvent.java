/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import java.net.SocketAddress;

/**
 * fired when a connection is dropped, either by us or the player.
 */
public final class ConnectionDroppedEvent {

  private final SocketAddress address;

  /**
   * Create an instance.
   *
   * @param address the address we lost.
   */
  public ConnectionDroppedEvent(SocketAddress address) {
    this.address = address;
  }

  public SocketAddress getAddress() {
    return address;
  }

  @Override
  public String toString() {
    return "ConnectionDroppedEvent{"
        + "address=" + address
        + '}';
  }
}
