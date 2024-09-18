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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import java.util.UUID;

/**
 * Represents a packet sent to remove a previously applied resource pack from the client.
 * The packet contains an optional UUID that identifies the resource pack to be removed.
 */
public class RemoveResourcePackPacket implements MinecraftPacket {

  private UUID id;

  public RemoveResourcePackPacket() {
  }

  public RemoveResourcePackPacket(final UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  @Override
  public void decode(final ByteBuf buf, final Direction direction, final ProtocolVersion protocolVersion) {
    if (buf.readBoolean()) {
      this.id = ProtocolUtils.readUuid(buf);
    }
  }

  @Override
  public void encode(final ByteBuf buf, final Direction direction, final ProtocolVersion protocolVersion) {
    buf.writeBoolean(id != null);

    if (id != null) {
      ProtocolUtils.writeUuid(buf, id);
    }
  }

  @Override
  public boolean handle(final MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}