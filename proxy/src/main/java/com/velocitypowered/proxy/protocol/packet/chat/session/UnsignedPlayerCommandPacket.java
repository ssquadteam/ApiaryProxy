/*
 * Copyright (C) 2022-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.chat.session;

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.LastSeenMessages;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an unsigned player command packet, extending {@link SessionPlayerCommandPacket}.
 *
 * <p>The {@code UnsignedPlayerCommandPacket} is used to handle player commands that are not
 * signed. It inherits session-specific behavior from {@link SessionPlayerCommandPacket}
 * while indicating that the command is unsigned.</p>
 */
public class UnsignedPlayerCommandPacket extends SessionPlayerCommandPacket {

  @Override
  public void decode(final ByteBuf buf, final ProtocolUtils.Direction direction, final ProtocolVersion protocolVersion) {
    this.command = ProtocolUtils.readString(buf, 256);
  }

  @Override
  public void encode(final ByteBuf buf, final ProtocolUtils.Direction direction, final ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, this.command);
  }

  @Override
  public SessionPlayerCommandPacket withLastSeenMessages(@Nullable final LastSeenMessages lastSeenMessages) {
    return this;
  }

  public boolean isSigned() {
    return false;
  }

  @Override
  public CommandExecuteEvent.SignedState getEventSignedState() {
    return CommandExecuteEvent.SignedState.UNSIGNED;
  }

  @Override
  public String toString() {
    return "UnsignedPlayerCommandPacket{"
      + "command='" + command + '\''
      + '}';
  }
}
