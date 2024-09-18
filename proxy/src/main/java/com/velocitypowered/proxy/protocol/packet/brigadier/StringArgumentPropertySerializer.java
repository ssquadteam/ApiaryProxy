/*
 * Copyright (C) 2018-2022 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

/**
 * Serializes properties for {@link StringArgumentType}.
 */
final class StringArgumentPropertySerializer implements ArgumentPropertySerializer<StringArgumentType> {

  public static final ArgumentPropertySerializer<StringArgumentType> STRING =
      new StringArgumentPropertySerializer();

  private StringArgumentPropertySerializer() {

  }

  @Override
  public StringArgumentType deserialize(final ByteBuf buf, final ProtocolVersion protocolVersion) {
    int type = ProtocolUtils.readVarInt(buf);
    return switch (type) {
      case 0 -> StringArgumentType.word();
      case 1 -> StringArgumentType.string();
      case 2 -> StringArgumentType.greedyString();
      default -> throw new IllegalArgumentException("Invalid string argument type " + type);
    };
  }

  @Override
  public void serialize(final StringArgumentType object, final ByteBuf buf, final ProtocolVersion protocolVersion) {
    switch (object.getType()) {
      case SINGLE_WORD:
        ProtocolUtils.writeVarInt(buf, 0);
        break;
      case QUOTABLE_PHRASE:
        ProtocolUtils.writeVarInt(buf, 1);
        break;
      case GREEDY_PHRASE:
        ProtocolUtils.writeVarInt(buf, 2);
        break;
      default:
        throw new IllegalArgumentException("Invalid string argument type " + object.getType());
    }
  }
}
