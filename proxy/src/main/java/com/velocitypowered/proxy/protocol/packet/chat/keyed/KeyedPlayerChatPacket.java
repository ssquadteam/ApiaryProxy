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

package com.velocitypowered.proxy.protocol.packet.chat.keyed;

import com.google.common.primitives.Longs;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.EncryptionUtils;
import com.velocitypowered.proxy.crypto.SignaturePair;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import java.time.Instant;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a player chat packet with support for message signing and preview.
 *
 * <p>The {@code KeyedPlayerChatPacket} handles player chat messages, supporting signed previews,
 * message signatures, and previous message validation. It includes fields for tracking message
 * signatures and handling expired messages.</p>
 */
public class KeyedPlayerChatPacket implements MinecraftPacket {

  private String message;
  private boolean signedPreview;
  private boolean unsigned = false;
  private @Nullable Instant expiry;
  private byte[] signature;
  private byte[] salt;
  private SignaturePair[] previousMessages = new SignaturePair[0];
  private @Nullable SignaturePair lastMessage;

  public static final int MAXIMUM_PREVIOUS_MESSAGE_COUNT = 5;

  public static final QuietDecoderException INVALID_PREVIOUS_MESSAGES =
      new QuietDecoderException("Invalid previous messages");

  public KeyedPlayerChatPacket() {
  }

  public KeyedPlayerChatPacket(final String message) {
    this.message = message;
    this.unsigned = true;
  }

  public void setExpiry(@Nullable final Instant expiry) {
    this.expiry = expiry;
  }

  public @Nullable Instant getExpiry() {
    return expiry;
  }

  public boolean isUnsigned() {
    return unsigned;
  }

  public String getMessage() {
    return message;
  }

  public boolean isSignedPreview() {
    return signedPreview;
  }

  @Override
  public void decode(final ByteBuf buf, final ProtocolUtils.Direction direction,
      final ProtocolVersion protocolVersion) {
    message = ProtocolUtils.readString(buf, 256);

    long expiresAt = buf.readLong();
    long saltLong = buf.readLong();
    byte[] signatureBytes = ProtocolUtils.readByteArray(buf);

    if (saltLong != 0L && signatureBytes.length > 0) {
      salt = Longs.toByteArray(saltLong);
      signature = signatureBytes;
      expiry = Instant.ofEpochMilli(expiresAt);
    } else if ((protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)
        || saltLong == 0L) && signatureBytes.length == 0) {
      unsigned = true;
    } else {
      throw EncryptionUtils.INVALID_SIGNATURE;
    }

    signedPreview = buf.readBoolean();
    if (signedPreview && unsigned) {
      throw EncryptionUtils.PREVIEW_SIGNATURE_MISSING;
    }

    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
      int size = ProtocolUtils.readVarInt(buf);
      if (size < 0 || size > MAXIMUM_PREVIOUS_MESSAGE_COUNT) {
        throw INVALID_PREVIOUS_MESSAGES;
      }

      SignaturePair[] lastSignatures = new SignaturePair[size];
      for (int i = 0; i < size; i++) {
        lastSignatures[i] = new SignaturePair(ProtocolUtils.readUuid(buf),
            ProtocolUtils.readByteArray(buf));
      }
      previousMessages = lastSignatures;

      if (buf.readBoolean()) {
        lastMessage = new SignaturePair(ProtocolUtils.readUuid(buf),
            ProtocolUtils.readByteArray(buf));
      }
    }
  }

  @Override
  public void encode(final ByteBuf buf, final ProtocolUtils.Direction direction,
      final ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, message);

    buf.writeLong(unsigned ? Instant.now().toEpochMilli() : Objects.requireNonNull(expiry).toEpochMilli());
    buf.writeLong(unsigned ? 0L : Longs.fromByteArray(salt));

    ProtocolUtils.writeByteArray(buf, unsigned ? EncryptionUtils.EMPTY : signature);

    buf.writeBoolean(signedPreview);

    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
      ProtocolUtils.writeVarInt(buf, previousMessages.length);
      for (SignaturePair previousMessage : previousMessages) {
        ProtocolUtils.writeUuid(buf, previousMessage.signer());
        ProtocolUtils.writeByteArray(buf, previousMessage.signature());
      }

      if (lastMessage != null) {
        buf.writeBoolean(true);
        ProtocolUtils.writeUuid(buf, lastMessage.signer());
        ProtocolUtils.writeByteArray(buf, lastMessage.signature());
      } else {
        buf.writeBoolean(false);
      }
    }
  }

  @Override
  public boolean handle(final MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
