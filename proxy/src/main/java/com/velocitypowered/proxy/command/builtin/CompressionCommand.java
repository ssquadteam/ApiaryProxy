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

package com.velocitypowered.proxy.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.natives.compression.CompressionLevelUtil;
import com.velocitypowered.natives.compression.CompressorUtils;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.bandwidth.BandwidthStats;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.DirectionFilter;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.PacketReport;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.PacketRow;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.PlayerReport;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.PlayerRow;
import com.velocitypowered.proxy.bandwidth.BandwidthStats.TrafficDirection;
import com.velocitypowered.proxy.compression.CompressionStats;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.translation.Argument;

/**
 * Implements {@code /velocity compression} and its subcommands.
 */
public final class CompressionCommand {

  private static final String ROOT_COMMAND = "/velocity compression";
  private static final String SIZE_ARG = "bytes";
  private static final String ROUNDS_ARG = "rounds";
  private static final String PACKET_TOP_ARG = "packetTop";
  private static final String PLAYER_TOP_ARG = "playerTop";
  private static final NamedTextColor ACCENT = NamedTextColor.AQUA;
  private static final NamedTextColor MUTED = NamedTextColor.DARK_GRAY;
  private static final NamedTextColor VALUE = NamedTextColor.WHITE;
  private static final NamedTextColor GOOD = NamedTextColor.GREEN;
  private static final int DEFAULT_BENCHMARK_BYTES = 32768;
  private static final int DEFAULT_BENCHMARK_ROUNDS = 64;
  private static final int DEFAULT_PACKET_TOP = 10;
  private static final int DEFAULT_PACKET_PLAYER_TOP = 3;
  private static final int DEFAULT_PLAYER_TOP = 10;
  private static final int MAX_BANDWIDTH_TOP = 50;

  private CompressionCommand() {
  }

  /**
   * Creates the {@code compression} subcommand tree under {@code /velocity}.
   *
   * @param server the proxy server instance
   * @return the compression command node
   */
  public static LiteralCommandNode<CommandSource> create(final VelocityServer server) {
    final LiteralCommandNode<CommandSource> backend = BrigadierCommand
        .literalArgumentBuilder("backend")
        .requires(source -> source.getPermissionValue("velocity.command.compression") == Tristate.TRUE)
        .executes(new Backend())
        .build();
    final LiteralCommandNode<CommandSource> config = BrigadierCommand
        .literalArgumentBuilder("config")
        .requires(source -> source.getPermissionValue("velocity.command.compression") == Tristate.TRUE)
        .executes(new Config(server))
        .build();
    final LiteralCommandNode<CommandSource> status = BrigadierCommand
        .literalArgumentBuilder("status")
        .requires(source -> source.getPermissionValue("velocity.command.compression") == Tristate.TRUE)
        .executes(new Status(server))
        .build();
    final LiteralCommandNode<CommandSource> compressionBenchmark = BrigadierCommand
        .literalArgumentBuilder("benchmark")
        .requires(source ->
            source.getPermissionValue("velocity.command.compression") == Tristate.TRUE)
        .executes(new CompressionBenchmark(server, DEFAULT_BENCHMARK_BYTES, DEFAULT_BENCHMARK_ROUNDS))
        .then(
            BrigadierCommand.requiredArgumentBuilder(SIZE_ARG, IntegerArgumentType.integer(512))
                .executes(ctx -> new CompressionBenchmark(
                    server,
                    IntegerArgumentType.getInteger(ctx, SIZE_ARG),
                    DEFAULT_BENCHMARK_ROUNDS
                ).run(ctx))
                .then(
                    BrigadierCommand.requiredArgumentBuilder(
                            ROUNDS_ARG,
                            IntegerArgumentType.integer(1)
                        )
                        .executes(ctx -> new CompressionBenchmark(
                            server,
                            IntegerArgumentType.getInteger(ctx, SIZE_ARG),
                            IntegerArgumentType.getInteger(ctx, ROUNDS_ARG)
                        ).run(ctx))
                )
        )
        .build();
    final LiteralCommandNode<CommandSource> compressionStats = BrigadierCommand
        .literalArgumentBuilder("stats")
        .requires(source ->
            source.getPermissionValue("velocity.command.compression") == Tristate.TRUE)
        .executes(new Stats(server))
        .build();
    final LiteralCommandNode<CommandSource> compressionReset = BrigadierCommand
        .literalArgumentBuilder("reset")
        .requires(source ->
            source.getPermissionValue("velocity.command.compression.reset") == Tristate.TRUE)
        .executes(new CompressionReset())
        .build();

    final LiteralCommandNode<CommandSource> bandwidthPackets = buildBandwidthPackets(server);
    final LiteralCommandNode<CommandSource> bandwidthPlayers = buildBandwidthPlayers(server);
    final LiteralCommandNode<CommandSource> bandwidthReset = BrigadierCommand
        .literalArgumentBuilder("reset")
        .requires(source ->
            source.getPermissionValue("velocity.command.compression.bandwidth.reset") == Tristate.TRUE)
        .executes(new BandwidthReset())
        .build();
    final LiteralCommandNode<CommandSource> bandwidth = BrigadierCommand
        .literalArgumentBuilder("bandwidth")
        .requires(source ->
            source.getPermissionValue("velocity.command.compression.bandwidth") == Tristate.TRUE
                || source.getPermissionValue("velocity.command.compression.bandwidth.reset")
                == Tristate.TRUE)
        .executes(new BandwidthRoot(server))
        .then(bandwidthPackets)
        .then(bandwidthPlayers)
        .then(bandwidthReset)
        .build();

    final List<LiteralCommandNode<CommandSource>> commands = List.of(
        status, backend, config, compressionStats, compressionReset, compressionBenchmark, bandwidth);
    return commands.stream()
        .reduce(
            BrigadierCommand.literalArgumentBuilder("compression")
                .requires(source -> source.getPermissionValue("velocity.command.compression")
                    == Tristate.TRUE
                    || source.getPermissionValue("velocity.command.compression.reset")
                    == Tristate.TRUE
                    || source.getPermissionValue("velocity.command.compression.bandwidth")
                    == Tristate.TRUE
                    || source.getPermissionValue("velocity.command.compression.bandwidth.reset")
                    == Tristate.TRUE)
                .executes(ctx -> {
                  final CommandSource source = ctx.getSource();
                  if (source.getPermissionValue("velocity.command.compression") == Tristate.TRUE) {
                    return new Stats(server).run(ctx);
                  }
                  final String availableCommands = commands.stream()
                      .filter(e -> e.getRequirement().test(source))
                      .map(LiteralCommandNode::getName)
                      .collect(Collectors.joining("|"));
                  sendHelp(source, availableCommands);
                  return Command.SINGLE_SUCCESS;
                }),
            ArgumentBuilder::then,
            ArgumentBuilder::then
        )
        .build();
  }

  private static LiteralCommandNode<CommandSource> buildBandwidthPackets(VelocityServer server) {
    final var root = BrigadierCommand.literalArgumentBuilder("packets")
        .requires(source ->
            source.getPermissionValue("velocity.command.compression.bandwidth") == Tristate.TRUE)
        .executes(new BandwidthPackets(
            server, DirectionFilter.ALL, DEFAULT_PACKET_TOP, DEFAULT_PACKET_PLAYER_TOP));

    root.then(packetTopArguments(server, DirectionFilter.ALL));
    for (DirectionFilter direction : DirectionFilter.values()) {
      root.then(BrigadierCommand.literalArgumentBuilder(direction.name().toLowerCase(Locale.ROOT))
          .executes(new BandwidthPackets(
              server, direction, DEFAULT_PACKET_TOP, DEFAULT_PACKET_PLAYER_TOP))
          .then(packetTopArguments(server, direction)));
    }
    return root.build();
  }

  private static ArgumentBuilder<CommandSource, ?> packetTopArguments(
      VelocityServer server, DirectionFilter direction) {
    return BrigadierCommand.requiredArgumentBuilder(
            PACKET_TOP_ARG, IntegerArgumentType.integer(1, MAX_BANDWIDTH_TOP))
        .executes(ctx -> new BandwidthPackets(
            server,
            direction,
            IntegerArgumentType.getInteger(ctx, PACKET_TOP_ARG),
            DEFAULT_PACKET_PLAYER_TOP
        ).run(ctx))
        .then(BrigadierCommand.requiredArgumentBuilder(
                PLAYER_TOP_ARG, IntegerArgumentType.integer(1, MAX_BANDWIDTH_TOP))
            .executes(ctx -> new BandwidthPackets(
                server,
                direction,
                IntegerArgumentType.getInteger(ctx, PACKET_TOP_ARG),
                IntegerArgumentType.getInteger(ctx, PLAYER_TOP_ARG)
            ).run(ctx)));
  }

  private static LiteralCommandNode<CommandSource> buildBandwidthPlayers(VelocityServer server) {
    final var root = BrigadierCommand.literalArgumentBuilder("players")
        .requires(source ->
            source.getPermissionValue("velocity.command.compression.bandwidth") == Tristate.TRUE)
        .executes(new BandwidthPlayers(server, DirectionFilter.ALL, DEFAULT_PLAYER_TOP));

    root.then(playerTopArguments(server, DirectionFilter.ALL));
    for (DirectionFilter direction : DirectionFilter.values()) {
      root.then(BrigadierCommand.literalArgumentBuilder(direction.name().toLowerCase(Locale.ROOT))
          .executes(new BandwidthPlayers(server, direction, DEFAULT_PLAYER_TOP))
          .then(playerTopArguments(server, direction)));
    }
    return root.build();
  }

  private static ArgumentBuilder<CommandSource, ?> playerTopArguments(
      VelocityServer server, DirectionFilter direction) {
    return BrigadierCommand.requiredArgumentBuilder(
            PLAYER_TOP_ARG, IntegerArgumentType.integer(1, MAX_BANDWIDTH_TOP))
        .executes(ctx -> new BandwidthPlayers(
            server,
            direction,
            IntegerArgumentType.getInteger(ctx, PLAYER_TOP_ARG)
        ).run(ctx));
  }

  private record Status(VelocityServer server) implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      final var compression = server.getConfiguration();
      final int configuredLevel = compression.getCompressionLevel();
      final String effectiveLevel = describeLevel(configuredLevel);

      sendSectionTitle(source, "velocity.command.compression.status-title");
      sendStatLine(source, Component.translatable(
          "velocity.command.compression.status-summary",
          VALUE,
          Argument.string("backend", Natives.compress.getLoadedVariant()),
          Argument.string(
              "threshold",
              Integer.toString(compression.getCompressionThreshold())
          ),
          Argument.string("level", effectiveLevel)
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression.status-backend",
          VALUE,
          Argument.string("backend", Natives.compress.getLoadedVariant())
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression.status-threshold",
          VALUE,
          Argument.string(
              "threshold",
              Integer.toString(compression.getCompressionThreshold())
          )
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression.status-level",
          VALUE,
          Argument.string("level", effectiveLevel)
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private record Backend() implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      sendSectionTitle(source, "velocity.command.compression.backend-title");
      sendStatLine(source, Component.translatable(
          "velocity.command.compression.backend-name",
          VALUE,
          Argument.string("backend", Natives.compress.getLoadedVariant())
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression.backend-levels",
          VALUE,
          Argument.string("levels", Integer.toString(maxSupportedLevel()))
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private record Config(VelocityServer server) implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      final var compression = server.getConfiguration();
      final int configuredLevel = compression.getCompressionLevel();
      sendSectionTitle(source, "velocity.command.compression.config-title");
      sendStatLine(source, Component.translatable(
          "velocity.command.compression.status-threshold",
          VALUE,
          Argument.string(
              "threshold",
              Integer.toString(compression.getCompressionThreshold())
          )
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression.status-level",
          VALUE,
          Argument.string("level", describeLevel(configuredLevel))
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression.config-defaults",
          VALUE,
          Argument.string("native", Integer.toString(CompressionLevelUtil.AGGRESSIVE_NATIVE_DEFAULT)),
          Argument.string("java", Integer.toString(CompressionLevelUtil.AGGRESSIVE_JAVA_DEFAULT))
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private record Stats(VelocityServer server) implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      final var compression = server.getConfiguration();
      final CompressionStats.Snapshot snapshot =
          CompressionStats.INSTANCE.snapshot();

      sendSectionTitle(source, "velocity.command.compression-title");
      sendStatLine(source, Component.translatable(
          "velocity.command.compression-window",
          VALUE,
          Argument.string("seconds", Long.toString(snapshot.elapsedSeconds()))
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression-backend",
          VALUE,
          Argument.string("backend", Natives.compress.getLoadedVariant())
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression-settings",
          VALUE,
          Argument.string(
              "threshold",
              Integer.toString(compression.getCompressionThreshold())
          ),
          Argument.string(
              "level",
              describeLevel(compression.getCompressionLevel())
          )
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression-packets",
          VALUE,
          Argument.string("total", Long.toString(snapshot.totalPackets())),
          Argument.string("compressed", Long.toString(snapshot.compressedPackets())),
          Argument.string("passthrough", Long.toString(snapshot.passthroughPackets()))
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression-raw-wire",
          VALUE,
          Argument.string("raw", humanBytes(snapshot.totalRawBytes())),
          Argument.string("wire", humanBytes(snapshot.totalEncodedBytes()))
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression-compressed-wire",
          VALUE,
          Argument.string("raw", humanBytes(snapshot.compressedRawBytes())),
          Argument.string("wire", humanBytes(snapshot.compressedEncodedBytes()))
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression-savings",
          GOOD,
          Argument.string(
              "bytes",
              humanBytes(Math.max(
                  0L,
                  snapshot.compressedRawBytes() - snapshot.compressedEncodedBytes()
              ))
          ),
          Argument.string(
              "percent",
              percentSaved(snapshot.compressedRawBytes(), snapshot.compressedEncodedBytes())
          )
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression-efficiency",
          GOOD,
          Argument.string(
              "percent",
              percentSaved(snapshot.totalRawBytes(), snapshot.totalEncodedBytes())
          )
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private record CompressionBenchmark(
      VelocityServer server,
      int sampleBytes,
      int rounds
  ) implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      final byte[] payload = createBenchmarkPayload(sampleBytes);
      final int maxLevel = maxSupportedLevel();
      long bestSize = Long.MAX_VALUE;
      int bestSizeLevel = 1;
      long bestTime = Long.MAX_VALUE;
      int bestTimeLevel = 1;

      sendSectionTitle(source, "velocity.command.compression.benchmark-title");
      sendStatLine(source, Component.translatable(
          "velocity.command.compression.benchmark-input",
          VALUE,
          Argument.string("bytes", humanBytes(sampleBytes)),
          Argument.string("rounds", Integer.toString(rounds)),
          Argument.string("backend", Natives.compress.getLoadedVariant())
      ));

      for (int level = 1; level <= maxLevel; level++) {
        final BenchmarkResult result = benchmark(level, payload, rounds);
        if (result.encodedBytes() < bestSize) {
          bestSize = result.encodedBytes();
          bestSizeLevel = level;
        }
        if (result.averageNanos() < bestTime) {
          bestTime = result.averageNanos();
          bestTimeLevel = level;
        }
        sendStatLine(source, Component.translatable(
            "velocity.command.compression.benchmark-row",
            VALUE,
            Argument.string("level", Integer.toString(level)),
            Argument.string("encoded", humanBytes(result.encodedBytes())),
            Argument.string("ratio", percentSaved(payload.length, result.encodedBytes())),
            Argument.string("time", formatNanos(result.averageNanos()))
        ));
      }

      sendStatLine(source, Component.translatable(
          "velocity.command.compression.benchmark-best-size",
          GOOD,
          Argument.string("level", Integer.toString(bestSizeLevel)),
          Argument.string("encoded", humanBytes(bestSize))
      ));
      sendStatLine(source, Component.translatable(
          "velocity.command.compression.benchmark-best-speed",
          GOOD,
          Argument.string("level", Integer.toString(bestTimeLevel)),
          Argument.string("time", formatNanos(bestTime))
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private record CompressionReset() implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      CompressionStats.INSTANCE.reset();
      context.getSource().sendMessage(Component.translatable(
          "velocity.command.compression-reset",
          NamedTextColor.GREEN
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private record BandwidthRoot(VelocityServer server) implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      if (source.getPermissionValue("velocity.command.compression.bandwidth") == Tristate.TRUE) {
        return new BandwidthPackets(
            server, DirectionFilter.ALL, DEFAULT_PACKET_TOP, DEFAULT_PACKET_PLAYER_TOP)
            .run(context);
      }
      sendHeader(source, Component.translatable("velocity.command.compression.help-title", ACCENT));
      sendCommandLine(
          source,
          "velocity.command.compression.help-bandwidth-reset",
          ROOT_COMMAND + " bandwidth reset"
      );
      return Command.SINGLE_SUCCESS;
    }
  }

  private record BandwidthPackets(
      VelocityServer server,
      DirectionFilter direction,
      int packetTop,
      int playerTop
  ) implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      if (!bandwidthStatsEnabled(server, source)) {
        return Command.SINGLE_SUCCESS;
      }

      final PacketReport report =
          BandwidthStats.INSTANCE.packetReport(direction, packetTop, playerTop);
      sendSectionTitle(source, "velocity.command.compression.bandwidth-packets-title");
      sendBandwidthSummary(
          source, direction, report.elapsedSeconds(), report.totalBytes(), report.totalPackets());
      sendUnattributed(source, report.unattributedBytes(), report.totalBytes());

      if (report.rows().isEmpty()) {
        sendStatLine(source, Component.translatable(
            "velocity.command.compression.bandwidth-empty", NamedTextColor.YELLOW));
        return Command.SINGLE_SUCCESS;
      }

      int rank = 1;
      for (PacketRow row : report.rows()) {
        sendStatLine(source, Component.translatable(
            "velocity.command.compression.bandwidth-packet-row",
            VALUE,
            Argument.string("rank", Integer.toString(rank++)),
            Argument.component("direction", trafficDirectionComponent(row.key().direction())),
            Argument.string("packet", row.key().displayName()),
            Argument.string("bytes", humanBytes(row.bytes())),
            Argument.string("percent", percentOf(row.bytes(), report.totalBytes())),
            Argument.string("packets", Long.toString(row.packets())),
            Argument.string("rate", averageRate(row.bytes(), report.elapsedSeconds()))
        ));
        for (PlayerRow player : row.players()) {
          sendNestedLine(source, Component.translatable(
              "velocity.command.compression.bandwidth-packet-player-row",
              NamedTextColor.GRAY,
              Argument.string("player", player.identity().name()),
              Argument.string("bytes", humanBytes(player.bytes())),
              Argument.string("packetPercent", percentOf(player.bytes(), row.bytes())),
              Argument.string("totalPercent", percentOf(player.bytes(), report.totalBytes()))
          ));
        }
        if (row.unattributedBytes() > 0L) {
          sendNestedLine(source, Component.translatable(
              "velocity.command.compression.bandwidth-packet-player-row",
              NamedTextColor.DARK_GRAY,
              Argument.component("player", Component.translatable(
                  "velocity.command.compression.bandwidth-unattributed-name")),
              Argument.string("bytes", humanBytes(row.unattributedBytes())),
              Argument.string(
                  "packetPercent", percentOf(row.unattributedBytes(), row.bytes())),
              Argument.string(
                  "totalPercent", percentOf(row.unattributedBytes(), report.totalBytes()))
          ));
        }
      }
      return Command.SINGLE_SUCCESS;
    }
  }

  private record BandwidthPlayers(
      VelocityServer server,
      DirectionFilter direction,
      int playerTop
  ) implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      if (!bandwidthStatsEnabled(server, source)) {
        return Command.SINGLE_SUCCESS;
      }

      final PlayerReport report =
          BandwidthStats.INSTANCE.playerReport(direction, playerTop);
      sendSectionTitle(source, "velocity.command.compression.bandwidth-players-title");
      sendBandwidthSummary(
          source, direction, report.elapsedSeconds(), report.totalBytes(), report.totalPackets());
      sendUnattributed(source, report.unattributedBytes(), report.totalBytes());

      if (report.rows().isEmpty()) {
        sendStatLine(source, Component.translatable(
            "velocity.command.compression.bandwidth-empty", NamedTextColor.YELLOW));
        return Command.SINGLE_SUCCESS;
      }

      int rank = 1;
      for (PlayerRow row : report.rows()) {
        final boolean online = row.identity().uuid() != null
            && server.getPlayer(row.identity().uuid()).isPresent();
        sendStatLine(source, Component.translatable(
            "velocity.command.compression.bandwidth-player-row",
            VALUE,
            Argument.string("rank", Integer.toString(rank++)),
            Argument.string("player", row.identity().name()),
            Argument.component("status", Component.translatable(online
                ? "velocity.command.compression.bandwidth-online"
                : "velocity.command.compression.bandwidth-offline")),
            Argument.string("bytes", humanBytes(row.bytes())),
            Argument.string("percent", percentOf(row.bytes(), report.totalBytes())),
            Argument.string("packets", Long.toString(row.packets())),
            Argument.string("rate", averageRate(row.bytes(), report.elapsedSeconds()))
        ));
      }
      return Command.SINGLE_SUCCESS;
    }
  }

  private record BandwidthReset() implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      BandwidthStats.INSTANCE.reset();
      context.getSource().sendMessage(Component.translatable(
          "velocity.command.compression.bandwidth-reset", NamedTextColor.GREEN));
      return Command.SINGLE_SUCCESS;
    }
  }

  private static boolean bandwidthStatsEnabled(VelocityServer server, CommandSource source) {
    if (server.getConfiguration().isPacketBandwidthStatsEnabled()) {
      return true;
    }
    sendStatLine(source, Component.translatable(
        "velocity.command.compression.bandwidth-disabled", NamedTextColor.YELLOW));
    return false;
  }

  private static void sendBandwidthSummary(CommandSource source, DirectionFilter direction,
      long elapsedSeconds, long totalBytes, long totalPackets) {
    sendStatLine(source, Component.translatable(
        "velocity.command.compression.bandwidth-summary",
        VALUE,
        Argument.component("direction", directionComponent(direction)),
        Argument.string("seconds", Long.toString(elapsedSeconds)),
        Argument.string("bytes", humanBytes(totalBytes)),
        Argument.string("rate", averageRate(totalBytes, elapsedSeconds)),
        Argument.string("packets", Long.toString(totalPackets))
    ));
  }

  private static void sendUnattributed(CommandSource source, long unattributed, long total) {
    if (unattributed <= 0L) {
      return;
    }
    sendStatLine(source, Component.translatable(
        "velocity.command.compression.bandwidth-unattributed",
        NamedTextColor.DARK_GRAY,
        Argument.string("bytes", humanBytes(unattributed)),
        Argument.string("percent", percentOf(unattributed, total))
    ));
  }

  private static Component directionComponent(DirectionFilter direction) {
    final String translationKey = switch (direction) {
      case ALL -> "velocity.command.compression.bandwidth-direction-all";
      case OUTBOUND -> "velocity.command.compression.bandwidth-direction-outbound";
      case INBOUND -> "velocity.command.compression.bandwidth-direction-inbound";
    };
    return Component.translatable(translationKey, ACCENT);
  }

  private static Component trafficDirectionComponent(TrafficDirection direction) {
    return Component.translatable(direction == TrafficDirection.OUTBOUND
        ? "velocity.command.compression.bandwidth-direction-outbound-short"
        : "velocity.command.compression.bandwidth-direction-inbound-short", ACCENT);
  }

  private static String percentOf(long part, long total) {
    if (total <= 0L) {
      return "0.00%";
    }
    return String.format(Locale.ROOT, "%.2f%%", (double) part / (double) total * 100.0d);
  }

  private static String averageRate(long bytes, long elapsedSeconds) {
    if (elapsedSeconds <= 0L) {
      return "0 B/s";
    }
    return humanBytes(bytes / elapsedSeconds) + "/s";
  }

  private static String describeLevel(int configuredLevel) {
    return configuredLevel == -1
        ? "auto(native=" + CompressionLevelUtil.AGGRESSIVE_NATIVE_DEFAULT
            + "/java=" + CompressionLevelUtil.AGGRESSIVE_JAVA_DEFAULT + ")"
        : Integer.toString(configuredLevel);
  }

  private static int maxSupportedLevel() {
    return Natives.compress.getLoadedVariant().toLowerCase(Locale.ROOT).contains("java")
        ? CompressionLevelUtil.JAVA_MAX_LEVEL
        : CompressionLevelUtil.LIBDEFLATE_MAX_LEVEL;
  }

  private static String percentSaved(long rawBytes, long encodedBytes) {
    if (rawBytes <= 0L) {
      return "0.00%";
    }
    final double percent = 100.0d - ((double) encodedBytes / (double) rawBytes * 100.0d);
    return String.format("%.2f%%", percent);
  }

  private static String humanBytes(long bytes) {
    if (bytes < 1024L) {
      return bytes + " B";
    }
    final String[] units = {"KiB", "MiB", "GiB", "TiB"};
    double value = bytes;
    int index = -1;
    do {
      value /= 1024.0d;
      index++;
    } while (value >= 1024.0d && index < units.length - 1);
    return String.format("%.2f %s", value, units[index]);
  }

  private static String formatNanos(long nanos) {
    if (nanos >= 1_000_000L) {
      return String.format("%.2f ms", nanos / 1_000_000.0d);
    }
    if (nanos >= 1_000L) {
      return String.format("%.2f us", nanos / 1_000.0d);
    }
    return nanos + " ns";
  }

  private static void sendHelp(CommandSource source, String availableCommands) {
    sendHeader(source, Component.translatable("velocity.command.compression.help-title", ACCENT));
    sendStatLine(source, Component.translatable(
        "velocity.command.compression.usage",
        VALUE,
        Argument.string("commands", availableCommands)
    ));
    sendCommandLine(source, "velocity.command.compression.help-backend", ROOT_COMMAND + " backend");
    sendCommandLine(source, "velocity.command.compression.help-config", ROOT_COMMAND + " config");
    sendCommandLine(source, "velocity.command.compression.help-status", ROOT_COMMAND + " status");
    sendCommandLine(source, "velocity.command.compression.help-stats", ROOT_COMMAND + " stats");
    sendCommandLine(source, "velocity.command.compression.help-reset", ROOT_COMMAND + " reset");
    sendCommandLine(
        source,
        "velocity.command.compression.help-benchmark",
        ROOT_COMMAND + " benchmark 32768 64"
    );
    sendCommandLine(
        source,
        "velocity.command.compression.help-bandwidth",
        ROOT_COMMAND + " bandwidth packets all 10 3"
    );
    sendCommandLine(
        source,
        "velocity.command.compression.help-bandwidth-players",
        ROOT_COMMAND + " bandwidth players all 10"
    );
    sendCommandLine(
        source,
        "velocity.command.compression.help-bandwidth-reset",
        ROOT_COMMAND + " bandwidth reset"
    );
  }

  private static void sendSectionTitle(CommandSource source, String translationKey) {
    sendHeader(source, Component.translatable(translationKey, ACCENT));
  }

  private static void sendHeader(CommandSource source, Component title) {
    final TextComponent line = Component.text()
        .append(Component.text("◆ ", ACCENT, TextDecoration.BOLD))
        .append(title.decoration(TextDecoration.BOLD, true))
        .build();
    source.sendMessage(line);
  }

  private static void sendStatLine(CommandSource source, Component content) {
    source.sendMessage(Component.text()
        .append(Component.text("│ ", MUTED))
        .append(content)
        .build());
  }

  private static void sendNestedLine(CommandSource source, Component content) {
    source.sendMessage(Component.text()
        .append(Component.text("│  └ ", MUTED))
        .append(content)
        .build());
  }

  private static void sendCommandLine(CommandSource source, String key, String command) {
    source.sendMessage(Component.text()
        .append(Component.text("│ ", MUTED))
        .append(Component.translatable(key, VALUE))
        .append(Component.text(" "))
        .append(Component.text(command, ACCENT, TextDecoration.UNDERLINED)
            .clickEvent(ClickEvent.suggestCommand(command))
            .hoverEvent(HoverEvent.showText(Component.text(command, VALUE))))
        .build());
  }

  private static BenchmarkResult benchmark(int level, byte[] payload, int rounds) {
    try (VelocityCompressor compressor = Natives.compress.get().create(level)) {
      // Hoist the ByteBufs out of the round loop. Unpooled direct buffers are expensive
      // to allocate (each is a native malloc plus a deallocation guard), and the prior per-round
      // allocation of source/encoded/decoded plus an encoded.copy() and a wrapped expected buffer
      // — five buffers every round — dominated wall-clock and pressured the allocator during
      // measurement. The buffers are reset between rounds: deflate/inflate only advance indices
      // and never mutate the source payload, so reuse is safe. encoded is sized to libdeflate's
      // compressBound (see CompressorUtils) so the grow-loop — which discards a full compression
      // pass on insufficient room and would pollute the timing — never triggers. Buffers are
      // allocated via the compressor's preferred buffer type (matching the production encode path)
      // so the measurement reflects the buffer type actually in use. A small JIT warmup phase runs
      // the same loop without recording, so averageNanos reflects steady-state rather than
      // first-invocation interpreter overhead.
      final int encodedCapacity = CompressorUtils.compressBound(payload.length);
      final int warmup = rounds >> 3;
      final ByteBufAllocator alloc = UnpooledByteBufAllocator.DEFAULT;
      final ByteBuf source = MoreByteBufUtils.preferredBuffer(alloc, compressor, payload.length);
      final ByteBuf encoded = MoreByteBufUtils.preferredBuffer(alloc, compressor, encodedCapacity);
      final ByteBuf decoded = MoreByteBufUtils.preferredBuffer(alloc, compressor, payload.length);
      final ByteBuf expected = Unpooled.wrappedBuffer(payload);
      source.writeBytes(payload);

      long totalNanos = 0L;
      int encodedBytes = 0;
      try {
        for (int index = 0; index < rounds + warmup; index++) {
          source.readerIndex(0);
          encoded.clear();
          decoded.clear();

          final boolean timing = index >= warmup;
          final long started = timing ? System.nanoTime() : 0L;
          compressor.deflate(source, encoded);
          if (timing) {
            totalNanos += System.nanoTime() - started;
            encodedBytes = encoded.readableBytes();
          }

          // Round-trip verify using encoded directly. inflate only advances readerIndex (which the
          // next round's clear() resets), so the previous per-round encoded.copy() — a full malloc
          // plus memcpy of the compressed payload — is eliminated.
          compressor.inflate(encoded, decoded, payload.length);
          if (!ByteBufUtil.equals(expected, decoded)) {
            throw new DataFormatException("Decoded payload did not match source data.");
          }
        }
      } finally {
        expected.release();
        source.release();
        encoded.release();
        decoded.release();
      }
      return new BenchmarkResult(encodedBytes, totalNanos / rounds);
    } catch (DataFormatException ex) {
      throw new IllegalStateException("Compression benchmark failed at level " + level, ex);
    }
  }

  private static byte[] createBenchmarkPayload(int sampleBytes) {
    final byte[] payload = new byte[sampleBytes];
    final byte[] repeated = (
        "{\"packet\":\"chat\",\"component\":\"<gray>[ApiaryProxy]</gray> "
            + "<aqua>compression probe</aqua>\",\"coords\":[123,64,-512],"
            + "\"server\":\"lobby\",\"nbt\":\"{foo:1b,bar:\\\"baz\\\"}\"}"
    ).getBytes(StandardCharsets.UTF_8);
    final Random random = new Random(12L);
    for (int index = 0; index < payload.length; index++) {
      final int marker = index % 16;
      if (marker < 12) {
        payload[index] = repeated[index % repeated.length];
      } else {
        payload[index] = (byte) random.nextInt(256);
      }
    }
    return payload;
  }

  private record BenchmarkResult(int encodedBytes, long averageNanos) {
  }
}
