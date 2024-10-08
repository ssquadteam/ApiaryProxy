/*
 * Copyright (C) 2024 Velocity Contributors
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
import net.kyori.adventure.key.Key;

/**
 * Represents a packet sent from the server to the client to store a cookie.
 * This packet can be used to send cookie-related data from the server to be stored or processed
 * by the client.
 */
public class ClientboundStoreCookiePacket implements MinecraftPacket {

  private Key key;
  private byte[] payload;

  public Key getKey() {
    return key;
  }

  public byte[] getPayload() {
    return payload;
  }

  public ClientboundStoreCookiePacket() {
  }

  public ClientboundStoreCookiePacket(final Key key, final byte[] payload) {
    this.key = key;
    this.payload = payload;
  }

  @Override
  public void decode(final ByteBuf buf, final Direction direction, final ProtocolVersion protocolVersion) {
    this.key = ProtocolUtils.readKey(buf);
    this.payload = ProtocolUtils.readByteArray(buf, 5120);
  }

  @Override
  public void encode(final ByteBuf buf, final Direction direction, final ProtocolVersion protocolVersion) {
    ProtocolUtils.writeKey(buf, key);
    ProtocolUtils.writeByteArray(buf, payload);
  }

  @Override
  public boolean handle(final MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
