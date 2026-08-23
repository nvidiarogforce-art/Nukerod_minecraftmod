# NukeRod

A Fabric mod for **Minecraft 1.21.10** that adds the **Nuke Rod** — a fishing rod that calls down a miniature nuclear strike at the target location.

[CurseForge project](https://www.curseforge.com/minecraft/mc-mods/nukerod-beta) · [Report an issue](https://github.com/nvidiarogforce-art/Nukerod_minecraftmod/issues)

## Features

- **Nuke Rod** with custom cast/reel behavior
- **Warhead Core** ammunition and crafting recipes
- Custom airborne **warhead entity** and renderer
- Large custom crater generation spread across ticks to reduce server hitching
- Multi-ring blast damage, knockback, fire and scorched terrain
- Fallout area effects
- Custom particles, flash effects, camera shake and HUD effects
- Configurable explosion radius, depth, cooldown, terrain healing, fallout and more
- Respects the `mobGriefing` gamerule for terrain destruction

## Requirements

- Minecraft **1.21.10**
- Fabric Loader **0.17.2+**
- Fabric API **0.135.0+1.21.10**
- Java **21+**

Exact dependency versions used by the project are listed in [`gradle.properties`](gradle.properties).

## Installation

1. Install Fabric Loader for Minecraft 1.21.10.
2. Install Fabric API.
3. Download NukeRod from the [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/nukerod-beta).
4. Put the mod JAR in your Minecraft `mods` folder.

## How to use

1. Craft a **Warhead Core**.
2. Craft the **Nuke Rod**.
3. Right-click to launch/cast the strike.
4. Right-click again to reel/detonate, or let the warhead detonate on impact.
5. After firing, the configured cooldown applies.

The runtime configuration is stored in:

```text
.minecraft/config/nukerod.json
```

## Configuration

The mod exposes options including:

| Setting | Default | Purpose |
|---|---:|---|
| `explosionRadius` | `24` | Horizontal crater radius |
| `explosionDepth` | `18` | Maximum crater depth |
| `craterLip` | `2` | Raised crater rim height |
| `power` | `12.0` | Blast damage/knockback and FX scale |
| `cooldownTicks` | `800` | Cooldown after a strike |
| `requiresWarheadCore` | `true` | Consume Warhead Core ammunition |
| `durabilityPerUse` | `5` | Rod durability consumed per launch |
| `enableFalloutDebuff` | `true` | Enable lingering fallout effects |
| `falloutDurationTicks` | `600` | Fallout duration |
| `enableTerrainHealing` | `false` | Allow scorched terrain to recover |
| `enableFire` | `true` | Spawn fire around the blast area |
| `columnsPerTick` | `512` | Crater work processed per server tick |

## Building from source

Clone the repository and build with the included Gradle wrapper:

```bash
git clone https://github.com/nvidiarogforce-art/Nukerod_minecraftmod.git
cd Nukerod_minecraftmod
./gradlew build
```

On Windows PowerShell:

```powershell
.\gradlew.bat build
```

The built JAR will be generated under `build/libs/`.

To launch a development client:

```bash
./gradlew runClient
```

## Project structure

```text
src/main/java/       Common/server-side mod code
src/client/java/     Client rendering, HUD, particles and mixins
src/main/resources/  Mod metadata, recipes, models, textures and sounds
src/client/resources/ Client mixin configuration
```

## Development notes

The project uses Fabric Loom with split client/common source sets. The current Java package remains `com.example.nukerod` for compatibility with the existing codebase; changing the package should be treated as a separate refactor rather than mixed into a release upload.

## License

NukeRod is licensed under the **MIT License**. See [`LICENSE`](LICENSE).

## Credits

Created by **Nizomiddin**.

Minecraft is a trademark of Mojang Studios. This project is not affiliated with or endorsed by Mojang or Microsoft.
