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

package com.velocitypowered.proxy.connection.client;

import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.velocitypowered.proxy.VelocityServer.GENERAL_GSON;
import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;
import static com.velocitypowered.proxy.crypto.EncryptionUtils.decryptRsa;
import static com.velocitypowered.proxy.crypto.EncryptionUtils.generateServerId;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequestPacket;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponsePacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import com.velocitypowered.proxy.util.VelocityProperties;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * Handles authenticating the player to Mojang's servers.
 */
public class InitialLoginSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(InitialLoginSessionHandler.class);
  private static final ThreadLocalRandom random = ThreadLocalRandom.current();
  private static final String MOJANG_HASJOINED_URL =
      System.getProperty("mojang.sessionserver",
              "https://sessionserver.mojang.com/session/minecraft/hasJoined")
          .concat("?username=%s&serverId=%s");

  private final VelocityServer server;
  private final MinecraftConnection mcConnection;
  private final LoginInboundConnection inbound;
  private @MonotonicNonNull ServerLoginPacket login;
  private byte[] verify = EMPTY_BYTE_ARRAY;
  private LoginState currentState = LoginState.LOGIN_PACKET_EXPECTED;
  private final boolean forceKeyAuthentication;

  InitialLoginSessionHandler(final VelocityServer server, final MinecraftConnection mcConnection,
                             final LoginInboundConnection inbound) {
    this.server = Preconditions.checkNotNull(server, "server");
    this.mcConnection = Preconditions.checkNotNull(mcConnection, "mcConnection");
    this.inbound = Preconditions.checkNotNull(inbound, "inbound");
    this.forceKeyAuthentication = VelocityProperties.readBoolean(
            "auth.forceSecureProfiles", server.getConfiguration().isForceKeyAuthentication());
  }

  @Override
  public boolean handle(final ServerLoginPacket packet) {
    assertState(LoginState.LOGIN_PACKET_EXPECTED);
    this.currentState = LoginState.LOGIN_PACKET_RECEIVED;
    IdentifiedKey playerKey = packet.getPlayerKey();
    if (playerKey != null) {
      if (playerKey.hasExpired()) {
        inbound.disconnect(
            Component.translatable("multiplayer.disconnect.invalid_public_key_signature"));
        return true;
      }

      boolean isKeyValid;
      if (playerKey.getKeyRevision() == IdentifiedKey.Revision.LINKED_V2
          && playerKey instanceof final IdentifiedKeyImpl keyImpl) {
        isKeyValid = keyImpl.internalAddHolder(packet.getHolderUuid());
      } else {
        isKeyValid = playerKey.isSignatureValid();
      }

      if (!isKeyValid) {
        inbound.disconnect(Component.translatable("multiplayer.disconnect.invalid_public_key"));
        return true;
      }
    } else if (mcConnection.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_19)
        && forceKeyAuthentication
        && mcConnection.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
      inbound.disconnect(Component.translatable("multiplayer.disconnect.missing_public_key"));
      return true;
    }
    inbound.setPlayerKey(playerKey);
    this.login = packet;

    final PreLoginEvent event = new PreLoginEvent(inbound, login.getUsername(), login.getHolderUuid());
    server.getEventManager().fire(event).thenRunAsync(() -> {
      if (mcConnection.isClosed()) {
        // The player was disconnected
        return;
      }

      PreLoginComponentResult result = event.getResult();
      Optional<Component> disconnectReason = result.getReasonComponent();
      if (disconnectReason.isPresent()) {
        // The component is guaranteed to be provided if the connection was denied.
        inbound.disconnect(disconnectReason.get());
        return;
      }

      inbound.loginEventFired(() -> {
        if (mcConnection.isClosed()) {
          // The player was disconnected
          return;
        }

        mcConnection.eventLoop().execute(() -> {
          if (!result.isForceOfflineMode()
              && (server.getConfiguration().isOnlineMode() || result.isOnlineModeAllowed())) {
            // Request encryption.
            EncryptionRequestPacket request = generateEncryptionRequest();
            this.verify = Arrays.copyOf(request.getVerifyToken(), 4);
            mcConnection.write(request);
            this.currentState = LoginState.ENCRYPTION_REQUEST_SENT;
          } else {
            mcConnection.setActiveSessionHandler(StateRegistry.LOGIN,
                new AuthSessionHandler(server, inbound,
                    GameProfile.forOfflinePlayer(login.getUsername()), false));
          }
        });
      });
    }, mcConnection.eventLoop()).exceptionally((ex) -> {
      logger.error("Exception in pre-login stage", ex);
      return null;
    });

    return true;
  }

  @Override
  public boolean handle(final LoginPluginResponsePacket packet) {
    this.inbound.handleLoginPluginResponse(packet);
    return true;
  }

  @Override
  public boolean handle(final EncryptionResponsePacket packet) {
    assertState(LoginState.ENCRYPTION_REQUEST_SENT);
    this.currentState = LoginState.ENCRYPTION_RESPONSE_RECEIVED;
    ServerLoginPacket login = this.login;
    if (login == null) {
      throw new IllegalStateException("No ServerLogin packet received yet.");
    }

    if (verify.length == 0) {
      throw new IllegalStateException("No EncryptionRequest packet sent yet.");
    }

    try {
      KeyPair serverKeyPair = server.getServerKeyPair();
      if (inbound.getIdentifiedKey() != null) {
        IdentifiedKey playerKey = inbound.getIdentifiedKey();
        if (!playerKey.verifyDataSignature(packet.getVerifyToken(), verify,
            Longs.toByteArray(packet.getSalt()))) {
          throw new IllegalStateException("Invalid client public signature.");
        }
      } else {
        byte[] decryptedVerifyToken = decryptRsa(serverKeyPair, packet.getVerifyToken());
        if (!MessageDigest.isEqual(verify, decryptedVerifyToken)) {
          throw new IllegalStateException("Unable to successfully decrypt the verification token.");
        }
      }

      byte[] decryptedSharedSecret = decryptRsa(serverKeyPair, packet.getSharedSecret());
      String serverId = generateServerId(decryptedSharedSecret, serverKeyPair.getPublic());

      String playerIp = ((InetSocketAddress) mcConnection.getRemoteAddress()).getHostString();
      String url = String.format(MOJANG_HASJOINED_URL,
          urlFormParameterEscaper().escape(login.getUsername()), serverId);

      if (server.getConfiguration().shouldPreventClientProxyConnections()) {
        url += "&ip=" + urlFormParameterEscaper().escape(playerIp);
      }

      final HttpRequest httpRequest = HttpRequest.newBuilder()
              .setHeader("User-Agent",
                      server.getVersion().getName() + "/" + server.getVersion().getVersion())
              .uri(URI.create(url))
              .build();
      final HttpClient httpClient = server.createHttpClient();
      httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
          .whenCompleteAsync((response, throwable) -> {
            if (mcConnection.isClosed()) {
              // The player disconnected after we authenticated them.
              return;
            }

            if (throwable != null) {
              logger.error("Unable to authenticate player", throwable);
              inbound.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
              return;
            }

            // Go ahead and enable encryption. Once the client sends EncryptionResponse, encryption
            // is enabled.
            try {
              mcConnection.enableEncryption(decryptedSharedSecret);
            } catch (GeneralSecurityException e) {
              logger.error("Unable to enable encryption for connection", e);
              // At this point, the connection is encrypted, but something's wrong on our side, and
              // we can't do anything about it.
              mcConnection.close(true);
              return;
            }

            if (response.statusCode() == 200) {
              final GameProfile profile = GENERAL_GSON.fromJson(response.body(),
                  GameProfile.class);
              // Not so fast, now we verify the public key for 1.19.1+
              if (inbound.getIdentifiedKey() != null
                  && inbound.getIdentifiedKey().getKeyRevision() == IdentifiedKey.Revision.LINKED_V2
                  && inbound.getIdentifiedKey() instanceof final IdentifiedKeyImpl key) {
                if (!key.internalAddHolder(profile.getId())) {
                  inbound.disconnect(
                      Component.translatable("multiplayer.disconnect.invalid_public_key"));
                }
              }
              // All went well, initialize the session.
              mcConnection.setActiveSessionHandler(StateRegistry.LOGIN,
                  new AuthSessionHandler(server, inbound, profile, true));
            } else if (response.statusCode() == 204) {
              // Apparently an offline-mode user logged onto this online-mode proxy.
              inbound.disconnect(
                  Component.translatable("velocity.error.online-mode-only", NamedTextColor.RED));
            } else {
              // Something else went wrong
              logger.error(
                  "Got an unexpected error code {} whilst contacting Mojang to log in {} ({})",
                  response.statusCode(), login.getUsername(), playerIp);
              inbound.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
            }
          }, mcConnection.eventLoop())
          .thenRun(() -> {
            if (httpClient instanceof final AutoCloseable closeable) {
              try {
                closeable.close();
              } catch (Exception e) {
                // In Java 21, the HttpClient does not throw any Exception
                // when trying to clean its resources, so this should not happen
                logger.error("An unknown error occurred while trying to close an HttpClient", e);
              }
            }
          });
    } catch (GeneralSecurityException e) {
      logger.error("Unable to enable encryption", e);
      mcConnection.close(true);
    }
    return true;
  }

  private EncryptionRequestPacket generateEncryptionRequest() {
    byte[] verify = new byte[4];
    random.nextBytes(verify);

    EncryptionRequestPacket request = new EncryptionRequestPacket();
    request.setPublicKey(server.getServerKeyPair().getPublic().getEncoded());
    request.setVerifyToken(verify);
    return request;
  }

  @Override
  public void handleUnknown(final ByteBuf buf) {
    mcConnection.close(true);
  }

  @Override
  public void disconnected() {
    this.inbound.cleanup();
  }

  private void assertState(final LoginState expectedState) {
    if (this.currentState != expectedState) {
      if (MinecraftDecoder.DEBUG) {
        logger.error("{} Received an unexpected packet requiring state {}, but we are in {}",
            inbound,
            expectedState, this.currentState);
      }
      mcConnection.close(true);
    }
  }

  private enum LoginState {
    LOGIN_PACKET_EXPECTED,
    LOGIN_PACKET_RECEIVED,
    ENCRYPTION_REQUEST_SENT,
    ENCRYPTION_RESPONSE_RECEIVED
  }
}
