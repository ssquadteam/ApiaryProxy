/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocitypowered.proxy.connection.player;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.packet.ChunkBatchEndPacket;
import com.velocitypowered.proxy.protocol.packet.ChunkBatchStartPacket;
import com.velocitypowered.proxy.protocol.packet.ChunkDataPacket;
import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tracks the chunks a client has loaded from each backend, with a retained copy of each chunk's
 * data, so they can be replayed when the client keeps its world across a server switch.
 *
 * <p>Thread-safe: chunk packets arrive on the backend event loop, the replay runs on the client
 * event loop.
 */
public class ChunkTracker {

  /**
   * Upper bound on retained chunks per backend; excess is evicted least-recently-sent first.
   */
  private static final int MAX_CHUNKS_PER_SERVER = 4096;

  private final Map<VelocityServerConnection, Map<Long, ByteBuf>> chunksByServer = new LinkedHashMap<>();

  private @Nullable VelocityServerConnection currentServer;

  /**
   * Records the chunk the client just received from the given backend.
   */
  public synchronized void onChunkData(VelocityServerConnection server, ChunkDataPacket packet) {
    currentServer = server;
    Map<Long, ByteBuf> chunks = chunksByServer.computeIfAbsent(server,
        key -> new LinkedHashMap<>(256, 0.75f, true));

    long key = chunkKey(packet.getChunkX(), packet.getChunkZ());
    ByteBuf previous = chunks.put(key, packet.content().retainedDuplicate());
    if (previous != null) {
      previous.release();
    }

    while (chunks.size() > MAX_CHUNKS_PER_SERVER) {
      Map.Entry<Long, ByteBuf> eldest = chunks.entrySet().iterator().next();
      chunks.remove(eldest.getKey());
      eldest.getValue().release();
    }
  }

  /**
   * Forgets the chunk the client just unloaded.
   */
  public synchronized void onChunkUnload(VelocityServerConnection server, int chunkX, int chunkZ) {
    currentServer = server;
    Map<Long, ByteBuf> chunks = chunksByServer.get(server);
    if (chunks == null) {
      return;
    }

    ByteBuf removed = chunks.remove(chunkKey(chunkX, chunkZ));
    if (removed != null) {
      removed.release();
    }
  }

  /**
   * Replays the tracked chunks of the streaming backend to the client as one chunk batch, then
   * forgets them. Called on a keep-world switch, before the destination starts streaming.
   *
   * @return the number of chunks replayed
   */
  public synchronized int replayCurrentChunks(MinecraftConnection client) {
    if (currentServer == null) {
      return 0;
    }

    VelocityServerConnection server = currentServer;
    currentServer = null;
    Map<Long, ByteBuf> chunks = chunksByServer.remove(server);
    if (chunks == null || chunks.isEmpty()) {
      return 0;
    }

    client.delayedWrite(ChunkBatchStartPacket.INSTANCE);
    for (Map.Entry<Long, ByteBuf> entry : chunks.entrySet()) {
      long key = entry.getKey();
      ChunkDataPacket packet = new ChunkDataPacket((int) (key >> 32), (int) key,
          entry.getValue().retainedDuplicate());
      client.delayedWrite(packet);
    }
    client.delayedWrite(new ChunkBatchEndPacket(chunks.size()));

    for (ByteBuf data : chunks.values()) {
      data.release();
    }
    return chunks.size();
  }

  /**
   * Forgets the tracked chunks of the streaming backend. Used when the client rebuilds its world
   * anyway, which makes the retained copies obsolete.
   */
  public synchronized void discardCurrent() {
    if (currentServer == null) {
      return;
    }

    clear(currentServer);
    currentServer = null;
  }

  /**
   * Forgets all tracked chunks of the given backend.
   */
  public synchronized void clear(VelocityServerConnection server) {
    Map<Long, ByteBuf> chunks = chunksByServer.remove(server);
    if (chunks == null) {
      return;
    }

    for (ByteBuf data : chunks.values()) {
      data.release();
    }
  }

  /**
   * Forgets everything.
   */
  public synchronized void clearAll() {
    currentServer = null;
    for (Map<Long, ByteBuf> chunks : chunksByServer.values()) {
      for (ByteBuf data : chunks.values()) {
        data.release();
      }
    }
    chunksByServer.clear();
  }

  private static long chunkKey(int chunkX, int chunkZ) {
    return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
  }
}
