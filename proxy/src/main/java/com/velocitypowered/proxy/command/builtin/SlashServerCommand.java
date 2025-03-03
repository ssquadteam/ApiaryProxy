/*
 * Copyright (C) 2018-2024 Velocity Contributors
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

package com.velocitypowered.proxy.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.command.VelocityCommands;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import net.kyori.adventure.text.Component;

/**
 * Implements {@code /<server_name>} aliases.
 */
public class SlashServerCommand {

  private final VelocityServer proxyServer;
  private final VelocityRegisteredServer server;

  public SlashServerCommand(final VelocityServer proxyServer, final String serverName) {
    this.proxyServer = proxyServer;
    this.server = (VelocityRegisteredServer) this.proxyServer.getServer(serverName).orElseThrow();
  }

  /**
   * Registers or unregisters the command based on the configuration value.
   */
  public void register(final String name) {
    final LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
        .literalArgumentBuilder(name)
        .requires(src -> VelocityCommands.checkServerPermissions(server, src))
        .executes(this::send);
    final BrigadierCommand command = new BrigadierCommand(rootNode);
    proxyServer.getCommandManager().register(
        proxyServer.getCommandManager().metaBuilder(command)
            .plugin(VelocityVirtualPlugin.INSTANCE)
            .build(),
        command
    );
  }

  /**
   * Connects the player to the target server when the command is executed.
   *
   * @param ctx the command context containing command details
   * @return a success code indicating the result of the command
   */
  private int send(final CommandContext<CommandSource> ctx) {
    final Player player = (Player) ctx.getSource();

    if (this.proxyServer.getConfiguration().getQueue().getNoQueueServers().contains(this.server.getServerInfo().getName())) {
      player.createConnectionRequest(this.server).connectWithIndication();
      return Command.SINGLE_SUCCESS;
    }

    ServerConnection conn = player.getCurrentServer().orElse(null);
    if (conn != null && conn.getServerInfo().getName().equalsIgnoreCase(server.getServerInfo().getName())) {
      player.sendMessage(Component.translatable("velocity.command.slashserver.already-connected"));
      return Command.SINGLE_SUCCESS;
    }

    this.proxyServer.getQueueManager().queue(player, this.server);
    return Command.SINGLE_SUCCESS;
  }
}
