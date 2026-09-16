# ApiaryProxy

<img src="apiary.png" alt="ApiaryProxy" width="160">

A Minecraft server proxy with unparalleled server support, scalability,
and flexibility.

## Licence

ApiaryProxy is licensed under the **GNU General Public License v3.0**, the same
licence as Velocity and Velocity-CTD, which it is derived from. The full text is
in [LICENSE](LICENSE).

The `api` module keeps upstream Velocity's **MIT** licence, so plugins compiled
against it are not affected by the GPL. See [api/LICENSE](api/LICENSE).

## Features

* Everything Velocity-CTD offers, tracking it upstream.
* Toggleable removal of the reconfiguration stage on server switches.
* bVelocity's compression system: more aggressive defaults, native levels
  through 12, live compression counters, a synthetic benchmark, and
  packet/player bandwidth diagnostics — all under `/velocity compression`.

## Remove Reconfig

By default, a player switching backend servers is sent back through the
configuration state, which shows the "Reconfiguring..." screen and makes the
resource pack, tab list and scoreboard flicker.

Setting `remove-reconfig = true` near the top of `velocity.toml` keeps the
player in the play state for the whole switch. The proxy answers the backend's
known-packs handshake on the client's behalf, replays the client brand, and
clears the scoreboard objectives, teams and boss bars the previous server left
behind. Titles are not reset either, so an in-progress teleport keeps its fade.

This is **off by default**.

### Notes

* Every backend server must run the same Minecraft protocol version. The client
  keeps the registry data it received from the first server it joined, so a
  backend on a different version will desync the client and cause visual
  corruption or kicks.
* Leave this off if your backends are not all on the same version.

## Compression

ApiaryProxy ports [bVelocity](https://github.com/coralundersea/bVelocity)'s
compression stack into `velocity.toml`'s `[advanced]` block. There is no
separate config file. Settings stay next to upstream Velocity's
`compression-threshold` and `compression-level` so the tree remains easy to
merge.

Compared with stock Velocity:

* the default threshold is `128` rather than `256`
* `compression-level = -1` resolves to native 6 / Java 6, the ratio sweet spot
  on typical Minecraft traffic rather than zlib's generic default
* native libdeflate accepts levels through 12 (Java fallback still caps at 9)
* outbound writes can be flush-consolidated, and the compressor pre-sizes
  output buffers with a small headroom so the grow-loop almost never retries

```toml
[advanced]
compression-threshold = 128
compression-level = -1
flush-consolidation-enabled = true
flush-consolidation-threshold = 256
compress-bound-headroom = 16
compression-stats-enabled = true
packet-bandwidth-stats-enabled = true
```

A more conservative profile:

```toml
[advanced]
compression-threshold = 192
compression-level = 9
```

An aggressive bandwidth-chasing profile:

```toml
[advanced]
compression-threshold = 64
compression-level = 12
```

Lower threshold and higher level reduce wire usage further, but they cost CPU.
The commands below measure that tradeoff live instead of guessing.

`/velocity compression` reports protocol-layer savings, not full NIC-level
traffic including TCP/IP overhead. `/velocity compression bandwidth` reports
compressed Minecraft frame bytes on player-facing connections. It excludes
TCP/IP overhead and does not double-count backend links.

### Commands

All of this lives under `/velocity compression`. There is no separate `/bv`
command.

* `/velocity compression` and `/velocity compression stats` — live counters
* `/velocity compression status` — backend, threshold, and effective level
* `/velocity compression backend` — loaded compressor and max level
* `/velocity compression config` — configured threshold, level, and auto defaults
* `/velocity compression reset` — start a new compression stats window
* `/velocity compression benchmark [bytes] [rounds]` — synthetic level sweep
* `/velocity compression bandwidth packets [all|outbound|inbound] [packetTop] [playerTop]`
* `/velocity compression bandwidth players [all|outbound|inbound] [playerTop]`
* `/velocity compression bandwidth reset`

Bandwidth directions are `outbound` (proxy to players), `inbound` (players to
proxy), or `all`. Top values accept `1..50`. Traffic before authentication,
such as status queries and login setup, is shown as unattributed. Disable
collection with `packet-bandwidth-stats-enabled = false`; that setting takes
effect after restart.

Permissions:

* `velocity.command.compression`
* `velocity.command.compression.reset`
* `velocity.command.compression.bandwidth`
* `velocity.command.compression.bandwidth.reset`

### Example numbers

One real-world run on `libdeflate (Linux x86_64)` produced:

```text
◆ Compression Benchmark
│ Sample=32.00 KiB | Rounds=64 | Backend=libdeflate (Linux x86_64)
│ Level 1 | Encoded=14.05 KiB | Saved=56.09% | Avg=109.93 us
│ Level 6 | Encoded=11.80 KiB | Saved=63.13% | Avg=339.09 us
│ Level 12 | Encoded=11.73 KiB | Saved=63.35% | Avg=2.28 ms
│ Best size: level 11 -> 11.73 KiB
│ Best speed: level 1 -> 109.93 us
```

And a matching live window:

```text
◆ Compression Stats
│ Window: 285s
│ Backend: libdeflate (Linux x86_64)
│ Threshold: 128 | Level: auto(native=6/java=6)
│ Packets total=58796 compressed=9634 passthrough=49162
│ Raw payload: 84.70 MiB | Wire bytes: 9.27 MiB
│ Compressed-only savings: 75.52 MiB (89.87%)
│ Overall wire efficiency: 89.06%
```

## Building

ApiaryProxy is built with [Gradle](https://gradle.org). We recommend using the
wrapper script (`./gradlew`) as our CI builds using it.

It is sufficient to run `./gradlew build` to run the full build cycle.

## Running

Once you've built ApiaryProxy, you can copy and run the `-all` JAR from
`proxy/build/libs`. ApiaryProxy will generate a default configuration file,
and you can configure it from there.

## Thanks to These Projects

* [Velocity](https://github.com/PaperMC/Velocity)
* [Velocity-CTD](https://github.com/GemstoneGG/Velocity-CTD)
* [Velocity (SunnySMP Fork)](https://github.com/Sunny-SMP/Velocity)
* [bVelocity](https://github.com/coralundersea/bVelocity)
