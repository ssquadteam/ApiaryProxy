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

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.CommandHandler;
import com.velocitypowered.proxy.protocol.packet.chat.builder.ChatBuilderV2;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;

/**
 * Handles keyed player commands by implementing {@link CommandHandler}.
 *
 * <p>The {@code KeyedCommandHandler} processes commands that are sent using
 * {@link KeyedPlayerCommandPacket}. It provides the necessary logic for handling
 * and executing commands associated with specific keys.</p>
 */
public class KeyedCommandHandler implements CommandHandler<KeyedPlayerCommandPacket> {

  private final ConnectedPlayer player;
  private final VelocityServer server;

  public KeyedCommandHandler(final ConnectedPlayer player, final VelocityServer server) {
    this.player = player;
    this.server = server;
  }

  @Override
  public Class<KeyedPlayerCommandPacket> packetClass() {
    return KeyedPlayerCommandPacket.class;
  }

  @Override
  public void handlePlayerCommandInternal(final KeyedPlayerCommandPacket packet) {
    if (!checkCommandRateLimit(this.player, this.server.getConfiguration().getMaxCommandsPerSecond())) {
      return;
    }

    queueCommandResult(this.server, this.player, (event, newLastSeenMessages) -> {
      CommandExecuteEvent.CommandResult result = event.getResult();
      IdentifiedKey playerKey = player.getIdentifiedKey();
      if (result == CommandExecuteEvent.CommandResult.denied()) {
        if (server.getConfiguration().enforceChatSigning() && playerKey != null) {
          if (!packet.isUnsigned()
              && playerKey.getKeyRevision().noLessThan(IdentifiedKey.Revision.LINKED_V2)) {
            logger.fatal("A plugin tried to deny a command with signable component(s). "
                + "This is not supported. "
                + "Disconnecting player {}. Command packet: {}",
                player.getUsername(), packet);
            player.disconnect(Component.text(
                "A proxy plugin caused an illegal protocol state. "
                    + "Contact your network administrator."));
          }
        }
        return CompletableFuture.completedFuture(null);
      }

      String commandToRun = result.getCommand().orElse(packet.getCommand());
      if (result.isForwardToServer()) {
        ChatBuilderV2 write = this.player.getChatBuilderFactory()
            .builder()
            .setTimestamp(packet.getTimestamp())
            .asPlayer(this.player);

        if (!server.getConfiguration().enforceChatSigning() || (!packet.isUnsigned() && commandToRun.equals(packet.getCommand()))) {
          return CompletableFuture.completedFuture(packet);
        } else {
          if (!packet.isUnsigned() && playerKey != null
              && playerKey.getKeyRevision().noLessThan(IdentifiedKey.Revision.LINKED_V2)) {
            logger.fatal("A plugin tried to change a command with signed component(s). "
                + "This is not supported. "
                + "Disconnecting player {}. Command packet: {}",
                player.getUsername(), packet);
            player.disconnect(Component.text(
                "A proxy plugin caused an illegal protocol state. "
                    + "Contact your network administrator."));
            return CompletableFuture.completedFuture(null);
          }
          write.message("/" + commandToRun);
        }
        return CompletableFuture.completedFuture(write.toServer());
      }
      return runCommand(this.server, this.player, commandToRun, hasRun -> {
        if (!hasRun) {
          if (commandToRun.equals(packet.getCommand())) {
            return packet;
          }

          if (server.getConfiguration().enforceChatSigning() && !packet.isUnsigned() && playerKey != null
              && playerKey.getKeyRevision().noLessThan(IdentifiedKey.Revision.LINKED_V2)) {
            logger.fatal("A plugin tried to change a command with signed component(s). "
                + "This is not supported. "
                + "Disconnecting player {}. Command packet: {}",
                player.getUsername(), packet);
            player.disconnect(Component.text(
                "A proxy plugin caused an illegal protocol state. "
                    + "Contact your network administrator."));
            return null;
          }

          return this.player.getChatBuilderFactory()
              .builder()
              .setTimestamp(packet.getTimestamp())
              .asPlayer(this.player)
              .message("/" + commandToRun)
              .toServer();
        }
        return null;
      });
    }, packet.getCommand(), packet.getTimestamp(), null, new CommandExecuteEvent.InvocationInfo(CommandExecuteEvent.SignedState.UNSUPPORTED, CommandExecuteEvent.Source.PLAYER));
  }
}
