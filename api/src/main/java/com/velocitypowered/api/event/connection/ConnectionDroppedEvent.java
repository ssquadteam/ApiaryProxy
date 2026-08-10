/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import java.net.SocketAddress;

/**
 * Fired when a connection is dropped, either by the proxy or by the player.
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

  /**
   * Returns the address the dropped connection came from.
   *
   * @return the remote address of the dropped connection
   */
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
