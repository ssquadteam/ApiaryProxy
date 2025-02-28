/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.ModInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a 1.7 and above server list ping response. This class is immutable.
 */
public final class ServerPing {

  private final Version version;
  private final @Nullable Players players;
  private final net.kyori.adventure.text.Component description;
  private final @Nullable Favicon favicon;
  private final @Nullable ModInfo modinfo;

  public ServerPing(final Version version, @Nullable final Players players,
      final net.kyori.adventure.text.Component description, @Nullable final Favicon favicon) {
    this(version, players, description, favicon, ModInfo.DEFAULT);
  }

  /**
   * Constructs a ServerPing instance.
   *
   * @param version the version of the server
   * @param players the players on the server
   * @param description the MOTD for the server
   * @param favicon the server's favicon
   * @param modinfo the mods this server runs
   */
  public ServerPing(final Version version, @Nullable final Players players,
      final net.kyori.adventure.text.Component description, @Nullable final Favicon favicon,
      @Nullable final ModInfo modinfo) {
    this.version = Preconditions.checkNotNull(version, "version");
    this.players = players;
    this.description = Preconditions.checkNotNull(description, "description");
    this.favicon = favicon;
    this.modinfo = modinfo;
  }

  public Version getVersion() {
    return version;
  }

  public Optional<Players> getPlayers() {
    return Optional.ofNullable(players);
  }

  public net.kyori.adventure.text.Component getDescriptionComponent() {
    return description;
  }

  public Optional<Favicon> getFavicon() {
    return Optional.ofNullable(favicon);
  }

  public Optional<ModInfo> getModinfo() {
    return Optional.ofNullable(modinfo);
  }

  @Override
  public String toString() {
    return "ServerPing{"
        + "version=" + version
        + ", players=" + players
        + ", description=" + description
        + ", favicon=" + favicon
        + ", modinfo=" + modinfo
        + '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerPing ping = (ServerPing) o;
    return Objects.equals(version, ping.version)
        && Objects.equals(players, ping.players)
        && Objects.equals(description, ping.description)
        && Objects.equals(favicon, ping.favicon)
        && Objects.equals(modinfo, ping.modinfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, players, description, favicon, modinfo);
  }

  /**
   * Returns a copy of this {@link ServerPing} instance as a builder so that it can be modified.
   * It is guaranteed that {@code ping.asBuilder().build().equals(ping)} is true: that is, if no
   * other changes are made to the returned builder, the built instance will equal the original
   * instance.
   *
   * @return a copy of this instance as a {@link Builder}
   */
  public Builder asBuilder() {
    Builder builder = new Builder();
    builder.version = version;
    if (players != null) {
      builder.onlinePlayers = players.online;
      builder.maximumPlayers = players.max;
      builder.samplePlayers.addAll(players.getSample());
    } else {
      builder.nullOutPlayers = true;
    }
    builder.description = description;
    builder.favicon = favicon;
    builder.nullOutModinfo = modinfo == null;
    if (modinfo != null) {
      builder.modType = modinfo.getType();
      builder.mods.addAll(modinfo.getMods());
    }
    return builder;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for {@link ServerPing} objects.
   */
  public static final class Builder {

    private Version version = new Version(0, "Unknown");
    private int onlinePlayers;
    private int maximumPlayers;
    private final List<SamplePlayer> samplePlayers = new ArrayList<>();
    private String modType = "FML";
    private final List<ModInfo.Mod> mods = new ArrayList<>();
    private net.kyori.adventure.text.Component description;
    private @Nullable Favicon favicon;
    private boolean nullOutPlayers;
    private boolean nullOutModinfo;

    private Builder() {

    }

    /**
     * Uses the modified {@code version} info in the response.
     *
     * @param version version info to set
     * @return this builder, for chaining
     */
    public Builder version(final Version version) {
      this.version = Preconditions.checkNotNull(version, "version");
      return this;
    }

    /**
     * Uses the modified {@code onlinePlayers} number in the response.
     *
     * @param onlinePlayers number for online players to set
     * @return this builder, for chaining
     */
    public Builder onlinePlayers(final int onlinePlayers) {
      this.onlinePlayers = onlinePlayers;
      return this;
    }

    /**
     * Uses the modified {@code maximumPlayers} number in the response.
     * <b>This will not modify the actual maximum players that can join the server.</b>
     *
     * @param maximumPlayers number for maximum players to set
     * @return this builder, for chaining
     */
    public Builder maximumPlayers(final int maximumPlayers) {
      this.maximumPlayers = maximumPlayers;
      return this;
    }

    /**
     * Uses the modified {@code players} array in the response.
     *
     * @param players array of SamplePlayers to set
     * @return this builder, for chaining
     */
    public Builder samplePlayers(final SamplePlayer... players) {
      this.samplePlayers.addAll(Arrays.asList(players));
      return this;
    }

    /**
     * Uses the modified {@code modType} in the response.
     *
     * @param modType the mod type to set
     * @return this builder, for chaining
     */
    public Builder modType(final String modType) {
      this.modType = Preconditions.checkNotNull(modType, "modType");
      return this;
    }

    /**
     * Uses the modified {@code mods} array in the response.
     *
     * @param mods array of mods to use
     * @return this builder, for chaining
     */
    public Builder mods(final ModInfo.Mod... mods) {
      this.mods.addAll(Arrays.asList(mods));
      return this;
    }

    /**
     * Uses the modified {@code mods} list in the response.
     *
     * @param mods the mod list to use
     * @return this builder, for chaining
     */
    public Builder mods(final ModInfo mods) {
      Preconditions.checkNotNull(mods, "mods");
      this.modType = mods.getType();
      this.mods.clear();
      this.mods.addAll(mods.getMods());
      return this;
    }

    /**
     * Clears the current list of mods to use in the response.
     *
     * @return this builder, for chaining
     */
    public Builder clearMods() {
      this.mods.clear();
      return this;
    }

    /**
     * Clears the current list of PlayerSamples to use in the response.
     *
     * @return this builder, for chaining
     */
    public Builder clearSamplePlayers() {
      this.samplePlayers.clear();
      return this;
    }

    /**
     * Defines the server as mod incompatible in the response.
     *
     * @return this builder, for chaining
     */
    public Builder notModCompatible() {
      this.nullOutModinfo = true;
      return this;
    }

    /**
     * Enables nulling Players in the response.
     * This will display the player count as {@code ???}.
     *
     * @return this builder, for chaining
     */
    public Builder nullPlayers() {
      this.nullOutPlayers = true;
      return this;
    }

    /**
     * Uses the {@code description} Component in the response.
     *
     * @param description Component to use as the description.
     * @return this builder, for chaining
     */
    public Builder description(final net.kyori.adventure.text.Component description) {
      this.description = Preconditions.checkNotNull(description, "description");
      return this;
    }

    /**
     * Uses the {@code favicon} in the response.
     *
     * @param favicon Favicon instance to use.
     * @return this builder, for chaining
     */
    public Builder favicon(final Favicon favicon) {
      this.favicon = Preconditions.checkNotNull(favicon, "favicon");
      return this;
    }

    /**
     * Clears the current favicon used in the response.
     *
     * @return this builder, for chaining
     */
    public Builder clearFavicon() {
      this.favicon = null;
      return this;
    }

    /**
     * Uses the information from this builder to create a new {@link ServerPing} instance. The
     * builder can be re-used after this event has been called.
     *
     * @return a new {@link ServerPing} instance
     */
    public ServerPing build() {
      if (this.version == null) {
        throw new IllegalStateException("version not specified");
      }
      if (this.description == null) {
        throw new IllegalStateException("no server description supplied");
      }
      return new ServerPing(version,
          nullOutPlayers ? null : new Players(onlinePlayers, maximumPlayers, samplePlayers),
          description, favicon, nullOutModinfo ? null : new ModInfo(modType, mods));
    }

    public Version getVersion() {
      return version;
    }

    public int getOnlinePlayers() {
      return onlinePlayers;
    }

    public int getMaximumPlayers() {
      return maximumPlayers;
    }

    public List<SamplePlayer> getSamplePlayers() {
      return samplePlayers;
    }

    public Optional<net.kyori.adventure.text.Component> getDescriptionComponent() {
      return Optional.ofNullable(description);
    }

    public Optional<Favicon> getFavicon() {
      return Optional.ofNullable(favicon);
    }

    public String getModType() {
      return modType;
    }

    public List<ModInfo.Mod> getMods() {
      return mods;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("version", version)
          .add("onlinePlayers", onlinePlayers)
          .add("maximumPlayers", maximumPlayers)
          .add("samplePlayers", samplePlayers)
          .add("modType", modType)
          .add("mods", mods)
          .add("description", description)
          .add("favicon", favicon)
          .add("nullOutPlayers", nullOutPlayers)
          .add("nullOutModinfo", nullOutModinfo)
          .toString();
    }
  }

  /**
   * Represents the version of the server sent to the client. A protocol version
   * that does not match the client's protocol version will show up on the server
   * list as an incompatible version, but the client will still permit the user
   * to connect to the server anyway.
   */
  public static final class Version {

    private final int protocol;
    private final String name;

    /**
     * Creates a new instance.
     *
     * @param protocol the protocol version as an integer
     * @param name a friendly name for the protocol version
     */
    public Version(final int protocol, final String name) {
      this.protocol = protocol;
      this.name = Preconditions.checkNotNull(name, "name");
    }

    public int getProtocol() {
      return protocol;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "Version{"
          + "protocol=" + protocol
          + ", name='" + name + '\''
          + '}';
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Version version = (Version) o;
      return protocol == version.protocol && Objects.equals(name, version.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(protocol, name);
    }
  }

  /**
   * Represents what the players the server purports to have online, its maximum capacity,
   * and a sample of players on the server.
   */
  public static final class Players {

    private final int online;
    private final int max;
    private final List<SamplePlayer> sample;

    /**
     * Creates a new instance.
     *
     * @param online the number of online players
     * @param max the maximum number of players
     * @param sample a sample of players on the server
     */
    public Players(final int online, final int max, final List<SamplePlayer> sample) {
      this.online = online;
      this.max = max;
      this.sample = ImmutableList.copyOf(sample);
    }

    public int getOnline() {
      return online;
    }

    public int getMax() {
      return max;
    }

    public List<SamplePlayer> getSample() {
      return sample == null ? ImmutableList.of() : sample;
    }

    @Override
    public String toString() {
      return "Players{"
          + "online='" + online + "'"
          + ", max='" + max + "'"
          + ", sample=" + sample
          + '}';
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Players players = (Players) o;
      return Objects.equals(online, players.online) && Objects.equals(max, players.max)
          && Objects.equals(sample, players.sample);
    }

    @Override
    public int hashCode() {
      return Objects.hash(online, max, sample);
    }
  }

  /**
   * A player returned in the sample field of the server ping players field.
   */
  public static final class SamplePlayer {

    private final String name;
    private final UUID id;

    public SamplePlayer(final Component name, final UUID id) {
      this.name = LegacyComponentSerializer.builder().hexCharacter('#').build().serialize(name);
      this.id = id;
    }

    public SamplePlayer(final String name, final UUID id) {
      this.name = name;
      this.id = id;
    }

    public String getName() {
      return this.name;
    }

    public Component getComponentName() {
      return LegacyComponentSerializer.legacyAmpersand().deserialize(name);
    }

    public UUID getId() {
      return id;
    }

    @Override
    public String toString() {
      return "SamplePlayer{"
          + "name=" + name
          + ", id=" + id
          + '}';
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SamplePlayer that = (SamplePlayer) o;
      return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }
}
