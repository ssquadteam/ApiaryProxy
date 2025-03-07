<img src="apiary.png" alt="Apiary Logo" align="right" width="200">
<div align="center">

## Apiary Proxy

[![Github Actions Build](https://img.shields.io/badge/BUILD-PASSING-green)](https://github.com/ssquadteam/ApiaryProxy/releases)

<h5>Apiary is a modernized <a href="https://papermc.io/software/velocity">Velocity</a> fork with all the necessary features for <a href="https://discord.gg/themegahive">The MegaHive</a></h5>
<h8>Logo designed by <a href="https://minecraft.net/">Mojang</a> duh</h8>
</div>

## Features
- **Modernized Codebase** to have an easily adaptable environment
 - **Necessary Server Commands in-built!** /alert /find /hub /ping /send, you name it!
 - **In-built Redis & Queue System!** Easy access to a Redi System to eliminate 
 - **Extremely High Performance** to achieve the main goal for having thousands of players!
 - **ApiaryAntibot Protection** - A custom fork of Sonar AntiBot integrated directly into the proxy for robust bot attack protection


## ApiaryProxy Permissions
* `velocity.command.alert` [/alert]
* `velocity.command.alertraw` [/alertraw]
* `velocity.command.find` [/find]
* `velocity.command.hub` [/hub]
* `velocity.command.ping` [/ping]
* `velocity.command.showall` [/showall]
* `velocity.command.uptime` [/velocity uptime]

## ApiaryProxy Redis Permissions
* `redis.command.proxy` [/proxy] (Shows the proxy you are connected to
  or the proxy another user is connected to).
* `redis.command.proxyids` [/proxyids] (Shows all available proxies
  with their according proxy IDs).
* `redis.command.plist` [/plist] (Shows all users connected
  to a specific proxy or a specific server on that proxy).

## ApiaryProxy Queue Commands
* `/queue` [Aliases: `/server` & `/joinqueue`]
* `/leavequeue`

## ApiaryProxy Administrative Commands
* `/queueadmin listqueues`
* `/queueadmin pause {SERVER}`
* `/queueadmin unpause {SERVER}`
* `/queueadmin add {PLAYER} {SERVER}`
* `/queueadmin addall {SERVER_FROM} {SERVER_TO}`
* `/queueadmin remove {PLAYER} {SERVER}` (Not including server name
  removes the user from all queues if multiple queuing is enabled).
* `/queueadmin removeall {SERVER}`

## ApiaryProxy Queue Permissions
* `queue.*`
* `queue.bypass` or `queue.bypass.{SERVER}`
* `queue.joinfull` or `queue.joinfull.{SERVER}`
* `queue.joinfullandbypass` or `queue.joinfullandbypass.{SERVER}`
* `queue.list` (Allows you to view the list of people queued for a specific server).
* `queue.listqueues` (Allows you to view all possible queues and number of people queued).
* `queue.pause` (Allows you to pause any specific server from queuing).
* `queue.pause.bypass` or `queue.pause.bypass.{SERVER}` (Allows you to bypass queue pauses
  for all servers or a specific server).
* `queue.priority.{ALL/SERVER}.{WEIGHT}` (Sets the position you are in for the/a queue).
* `queue.remove` (Allows you to remove a player from any specific queue).

## Contact

- 📫 Discord: `iamcxv7`


## Downloads

Pre-built Jars can be found in the [Releases Tab](https://github.com/ssquadteam/ApiaryProxy/releases)


## Building

Building a Server Jar for Distribution:

```bash
./gradlew build
```

Credits:
-------------
Thanks to these projects below. If these excellent projects hadn't appeared, Apiary would not have been so great.

- [Velocity-CTD](https://github.com/GemstoneGG/Velocity-CTD)
- [Velocity](https://github.com/PaperMC/Velocity)
