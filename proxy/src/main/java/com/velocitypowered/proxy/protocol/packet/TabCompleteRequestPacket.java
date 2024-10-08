/*
 * Copyright (C) 2018-2021 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a packet sent by the client when a tab-completion request is initiated.
 */
public class TabCompleteRequestPacket implements MinecraftPacket {

  private static final int VANILLA_MAX_TAB_COMPLETE_LEN = 2048;

  private @Nullable String command;
  private int transactionId;
  private boolean assumeCommand;
  private boolean hasPosition;
  private long position;

  /**
   * Gets the command string to be completed.
   *
   * @return the command string
   * @throws IllegalStateException if the command is not set
   */
  public String getCommand() {
    if (command == null) {
      throw new IllegalStateException("Command is not specified");
    }
    return command;
  }

  public void setCommand(@Nullable final String command) {
    this.command = command;
  }

  public boolean isAssumeCommand() {
    return assumeCommand;
  }

  public void setAssumeCommand(final boolean assumeCommand) {
    this.assumeCommand = assumeCommand;
  }

  public boolean hasPosition() {
    return hasPosition;
  }

  public void setHasPosition(final boolean hasPosition) {
    this.hasPosition = hasPosition;
  }

  public long getPosition() {
    return position;
  }

  public void setPosition(final long position) {
    this.position = position;
  }

  public int getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(final int transactionId) {
    this.transactionId = transactionId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("command", command)
        .add("transactionId", transactionId)
        .add("assumeCommand", assumeCommand)
        .add("hasPosition", hasPosition)
        .add("position", position)
        .toString();
  }

  @Override
  public void decode(final ByteBuf buf, final ProtocolUtils.Direction direction, final ProtocolVersion version) {
    if (version.noLessThan(MINECRAFT_1_13)) {
      this.transactionId = ProtocolUtils.readVarInt(buf);
      this.command = ProtocolUtils.readString(buf, VANILLA_MAX_TAB_COMPLETE_LEN);
    } else {
      this.command = ProtocolUtils.readString(buf, VANILLA_MAX_TAB_COMPLETE_LEN);
      if (version.noLessThan(MINECRAFT_1_9)) {
        this.assumeCommand = buf.readBoolean();
      }
      if (version.noLessThan(MINECRAFT_1_8)) {
        this.hasPosition = buf.readBoolean();
        if (hasPosition) {
          this.position = buf.readLong();
        }
      }
    }
  }

  /**
   * Encodes the packet data into the provided {@link ByteBuf}.
   *
   * @param buf the buffer to write to
   * @param direction the direction of the packet (client to server or server to client)
   * @param version the protocol version in use
   * @throws IllegalStateException if the command is not specified
   */
  @Override
  public void encode(final ByteBuf buf, final ProtocolUtils.Direction direction, final ProtocolVersion version) {
    if (command == null) {
      throw new IllegalStateException("Command is not specified");
    }

    if (version.noLessThan(MINECRAFT_1_13)) {
      ProtocolUtils.writeVarInt(buf, transactionId);
      ProtocolUtils.writeString(buf, command);
    } else {
      ProtocolUtils.writeString(buf, command);
      if (version.noLessThan(MINECRAFT_1_9)) {
        buf.writeBoolean(assumeCommand);
      }
      if (version.noLessThan(MINECRAFT_1_8)) {
        buf.writeBoolean(hasPosition);
        if (hasPosition) {
          buf.writeLong(position);
        }
      }
    }
  }

  @Override
  public boolean handle(final MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
