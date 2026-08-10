/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.LoginPhaseConnection;

/**
 * This event is fired when the http request to the Mojang session servers is responded to
 * Velocity will fire this event asynchronously and will not wait for it to complete before
 * handling the connection.
 */
public final class ValidateSessionEvent {

  private final LoginPhaseConnection connection;
  private final int sessionServerResponseCode;
  private final boolean isValid;

  /**
   * Create an instance.
   *
   * @param connection the connection of the player being validated.
   * @param sessionServerResponseCode the http response code from the Mojang session servers.
   * @param isValid if the session was successfully validated.
   */
  public ValidateSessionEvent(LoginPhaseConnection connection, int sessionServerResponseCode, boolean isValid) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.sessionServerResponseCode = sessionServerResponseCode;
    this.isValid = isValid;
  }

  /**
   * Returns the connection of the player whose session was validated.
   *
   * @return the connection being validated
   */
  public LoginPhaseConnection getConnection() {
    return connection;
  }

  /**
   * Returns the status code the Mojang session servers responded with.
   *
   * @return the HTTP status code, or 503 if the request itself failed
   */
  public int getSessionServerResponseCode() {
    return this.sessionServerResponseCode;
  }

  /**
   * Returns whether the session was validated successfully.
   *
   * @return true if the session was validated
   */
  public boolean isValid() {
    return this.isValid;
  }

  @Override
  public String toString() {
    return "ValidateSessionEvent{"
        + "connection=" + connection
        + ", sessionServerResponseCode=" + sessionServerResponseCode
        + ", isValid=" + isValid
        + '}';
  }
}
