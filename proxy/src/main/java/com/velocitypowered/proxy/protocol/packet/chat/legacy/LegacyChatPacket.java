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

package com.velocitypowered.proxy.protocol.packet.chat.legacy;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a legacy chat packet used in older versions of Minecraft.
 *
 * <p>The {@code LegacyChatPacket} is responsible for holding and transmitting chat messages
 * in the format used by legacy versions of Minecraft. It implements {@link MinecraftPacket}
 * to ensure compatibility with the packet-handling system.</p>
 */
public class LegacyChatPacket implements MinecraftPacket {

  public static final byte CHAT_TYPE = (byte) 0;
  public static final byte SYSTEM_TYPE = (byte) 1;
  public static final byte GAME_INFO_TYPE = (byte) 2;

  public static final int MAX_SERVERBOUND_MESSAGE_LENGTH = 256;
  public static final UUID EMPTY_SENDER = new UUID(0, 0);

  private @Nullable String message;
  private byte type;
  private @Nullable UUID sender;

  public LegacyChatPacket() {
  }

  /**
   * Creates a Chat packet.
   */
  public LegacyChatPacket(@Nullable final String message, final byte type, @Nullable final UUID sender) {
    this.message = message;
    this.type = type;
    this.sender = sender;
  }

  /**
   * Retrieves the Chat message.
   */
  public String getMessage() {
    if (message == null) {
      throw new IllegalStateException("Message is not specified");
    }
    return message;
  }

  public void setMessage(@Nullable final String message) {
    this.message = message;
  }

  public byte getType() {
    return type;
  }

  public void setType(final byte type) {
    this.type = type;
  }

  public UUID getSenderUuid() {
    return sender;
  }

  public void setSenderUuid(final UUID sender) {
    this.sender = sender;
  }

  @Override
  public String toString() {
    return "Chat{"
        + "message='" + message + '\''
        + ", type=" + type
        + ", sender=" + sender
        + '}';
  }

  @Override
  public void decode(final ByteBuf buf, final ProtocolUtils.Direction direction, final ProtocolVersion version) {
    message = ProtocolUtils.readString(buf);
    if (direction == ProtocolUtils.Direction.CLIENTBOUND
        && version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      type = buf.readByte();
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
        sender = ProtocolUtils.readUuid(buf);
      }
    }
  }

  @Override
  public void encode(final ByteBuf buf, final ProtocolUtils.Direction direction, final ProtocolVersion version) {
    if (message == null) {
      throw new IllegalStateException("Message is not specified");
    }
    ProtocolUtils.writeString(buf, message);
    if (direction == ProtocolUtils.Direction.CLIENTBOUND
        && version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      buf.writeByte(type);
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
        ProtocolUtils.writeUuid(buf, sender == null ? EMPTY_SENDER : sender);
      }
    }
  }

  @Override
  public boolean handle(final MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
