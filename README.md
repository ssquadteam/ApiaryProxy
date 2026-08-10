# ApiaryProxy

[![Join the Discord](https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExdG5sdGgwazRwYjh4djdsdXJwcHR5ajZrNGE2NDBvcTUzdXltbHp1cCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/fGIwpaCrtkFdHVksSu/giphy.gif)](https://discord.gg/themegahivemc) \
_[Discord](https://discord.gg/themegahivemc)_

<img src="apiary.png" alt="ApiaryProxy" width="160">

A Minecraft server proxy with unparalleled server support, scalability,
and flexibility.

ApiaryProxy is licensed under the GPLv3 license.

## Features

* Everything Velocity-CTD offers, tracking it upstream.
* Toggleable removal of the reconfiguration stage on server switches.

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
