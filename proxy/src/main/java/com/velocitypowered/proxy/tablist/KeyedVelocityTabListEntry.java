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

package com.velocitypowered.proxy.tablist;

import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.player.ChatSession;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.chat.RemoteChatSession;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Handles modern tab list entries.
 */
public class KeyedVelocityTabListEntry implements TabListEntry {

  private final KeyedVelocityTabList tabList;
  private final GameProfile profile;
  private net.kyori.adventure.text.Component displayName;
  private int latency;
  private int gameMode;
  private @Nullable IdentifiedKey playerKey;

  KeyedVelocityTabListEntry(final KeyedVelocityTabList tabList, final GameProfile profile,
                            final net.kyori.adventure.text.@Nullable Component displayName, final int latency, final int gameMode,
                            @Nullable final IdentifiedKey playerKey) {
    this.tabList = tabList;
    this.profile = profile;
    this.displayName = displayName;
    this.latency = latency;
    this.gameMode = gameMode;
    this.playerKey = playerKey;
  }

  @Override
  public TabList getTabList() {
    return tabList;
  }

  @Override
  public GameProfile getProfile() {
    return profile;
  }

  @Override
  public Optional<net.kyori.adventure.text.Component> getDisplayNameComponent() {
    return Optional.ofNullable(displayName);
  }

  @Override
  public TabListEntry setDisplayName(final net.kyori.adventure.text.@Nullable Component displayName) {
    this.displayName = displayName;
    tabList.updateEntry(LegacyPlayerListItemPacket.UPDATE_DISPLAY_NAME, this);
    return this;
  }

  void setDisplayNameInternal(final net.kyori.adventure.text.@Nullable Component displayName) {
    this.displayName = displayName;
  }

  @Override
  public int getLatency() {
    return latency;
  }

  @Override
  public TabListEntry setLatency(final int latency) {
    this.latency = latency;
    tabList.updateEntry(LegacyPlayerListItemPacket.UPDATE_LATENCY, this);
    return this;
  }

  void setLatencyInternal(final int latency) {
    this.latency = latency;
  }

  @Override
  public int getGameMode() {
    return gameMode;
  }

  @Override
  public TabListEntry setGameMode(final int gameMode) {
    this.gameMode = gameMode;
    tabList.updateEntry(LegacyPlayerListItemPacket.UPDATE_GAMEMODE, this);
    return this;
  }

  void setGameModeInternal(final int gameMode) {
    this.gameMode = gameMode;
  }

  @Override
  public @Nullable ChatSession getChatSession() {
    return new RemoteChatSession(null, this.playerKey);
  }

  @Override
  public IdentifiedKey getIdentifiedKey() {
    return playerKey;
  }

  void setPlayerKeyInternal(final IdentifiedKey playerKey) {
    this.playerKey = playerKey;
  }
}
