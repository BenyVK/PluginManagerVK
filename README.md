# PluginManagerVK

A lightweight **Paper 1.21** plugin that lets you control **which worlds each plugin is active in**.

If a plugin isn't allowed in a world, it behaves as if it doesn't exist there: its events are not delivered, its commands are blocked, and they disappear from tab-complete.

> Bukkit/Paper has no native "enable plugin per world" feature, so PluginManagerVK emulates it by wrapping each managed plugin's event listeners and filtering by world. See [Limitations](#limitations).

## Features

- Per-plugin world whitelist via a simple config file
- Event filtering: events from non-allowed worlds never reach the plugin
- Command filtering: blocked in non-allowed worlds (shows the vanilla "Unknown command" message) and removed from tab-complete
- Player lifecycle handling: when a player moves between an allowed and a non-allowed world, the plugin receives a synthetic `PlayerQuitEvent` / `PlayerJoinEvent`, so its per-player state (fly, bossbars, scoreboards, caches) stays consistent
- Live reload with `/pmvk reload`
- Plugins that aren't listed in the config are left untouched (active in all worlds)

## Requirements

- Paper 1.21+ (Spigot/Bukkit are not supported)
- Java 21+

## Installation

1. Download the latest jar from the [Releases](../../releases) page, or build it yourself (see below).
2. Put the jar in your server's `plugins/` folder.
3. Start the server once to generate `plugins/PluginManagerVK/configvk.yml`.
4. Edit the config and run `/pmvk reload`.

## Configuration

`plugins/PluginManagerVK/configvk.yml`

```yaml
plugins:
  Essentials:
    worlds:
      - world
  WorldEdit:
    worlds:
      - creative
      - world_nether
```

- The key under `plugins:` is the plugin name (case-insensitive), exactly as it appears in `/plugins`.
- `worlds` is the list of world names where that plugin is allowed.
- Any plugin **not** listed here is active everywhere.

## Commands & Permissions

| Command | Description | Permission |
|---|---|---|
| `/pmvk reload` | Reloads `configvk.yml` and re-syncs online players | `pmvk.admin` (default: op) |

## How it works

1. One tick after startup, PluginManagerVK replaces every `RegisteredListener` belonging to a managed plugin with a world-aware wrapper.
2. For each event, the wrapper resolves the world (player, block, entity, world, chunk, weather, vehicle, hanging, inventory events) and only forwards the event if the plugin is allowed in that world.
3. For each managed plugin, PluginManagerVK tracks which players the plugin "knows about". When a player's world changes, it fires a fake Join/Quit directly to that plugin only, so the plugin never sees a half-finished state.
4. A command filter blocks `PluginCommand`s of managed plugins in disallowed worlds and strips them from the command list sent to the client.

## Building

```bash
git clone https://github.com/BenyVK/PluginManagerVK.git
cd PluginManagerVK
./gradlew build
```

The jar will be in `build/libs/`. On Windows use `gradlew.bat build`.

To test on a local Paper server:

```bash
./gradlew runServer
```

## Limitations

- **Scheduler tasks** created by a plugin cannot be intercepted and keep running in every world.
- **Events without a world** (e.g. `ServerListPingEvent`) are not forwarded for managed plugins.
- **Brigadier / Paper-style commands** that are not `PluginCommand`s are not filtered.
- **Direct API calls** between plugins still work (e.g. a plugin calling another plugin's API), even when the target is restricted in that world.
- Plugins that **save data on quit** or **greet players on join** will do so again on the synthetic Quit/Join when a player crosses between allowed and disallowed worlds.
- Plugins that manipulate `HandlerList` themselves may bypass the filter.

**Recommendation:** only restrict plugins that are naturally world-specific (WorldEdit, land protection, minigames, etc.). Avoid restricting global plugins such as economy, ranks, or chat.

## Roadmap

- [ ] In-game commands to edit rules (`/pmvk set <plugin> <world>`)
- [ ] GUI for managing plugin/world rules
- [ ] Blacklist mode (allow everywhere except listed worlds)

## Contributing

Issues and pull requests are welcome.

## License

Released under the [MIT License](LICENSE).
