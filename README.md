# AsyncParticles

- It will remain in the beta stage in the future to alert potential mod conflict issues.
  - Not yet compatible with Particle Core.
- Now compatible with Valkyrien Skies v2.4.
- Now support for GPU acceleration.(v20.1.0a+)
- Create contraptions can now block vanilla weather.(v20.1.0a+)
## Configuration
### Settings can be accessed via the ModList/ModMenu.

## Troubleshooting

### Crash with C2ME
- Set `enforceSafeWorldRandomAccess = "false"` in `c2me.toml`
  - Only set this if you are experiencing crashes.

### Crash with LodestoneLib
- Set `buffer_particles = false` in `lodestone-client.toml`.

### Crashes related to `ClassInstanceMultiMap` or `Level.getEntities(...)`
- Please share the logs or crash report on the GitHub issue.
- As a workaround, enable the option:  
  `Mixin` → `Make 'ClassInstanceMultiMap' Thread-Safe`

### Crashes related to `Level.getBlockEntity(...)`
- Please share the logs or crash report on the GitHub issue.
- As a workaround, enable the option:  
  `Mixin` → `Make 'LevelChunk#blockEntities' Thread-Safe`

### ConcurrentModificationException

#### The Most Common Cause
This exception typically occurs when particle rendering and ticking concurrently access the same **thread-unsafe container**.

#### How to Fix
- Add the fully qualified particle class names to both `particle$lockProvider` and `particle$lockRequired` in the mod's Mixin settings.  
  [See the configuration guide for details](https://github.com/Harveykang/AsyncParticles/wiki/Mixin-Configuration)

> **Note**: This enables fine-grained locking for specific particle types, allowing safe asynchronous execution between the `tick()` and `render()` methods.

#### Other Potential Causes
Starting in **vX.4.0**, the default particle rendering mode has been changed to `SYNCHRONOUSLY` to prevent rare `ConcurrentModificationExceptions` when used with mods that access external thread-unsafe containers (e.g., global maps or lists) during particle rendering.

- If you're using an older version (**≤ vX.3.0**), consider manually setting the mode to `SYNCHRONOUSLY` in the config screen for improved stability.
- **For modpack authors**: If you’d like to test whether `FASTEST` provides a noticeable FPS boost in your setup, you can switch to it in the config screen—**but use with caution**, as it may trigger crashes with incompatible mods.

### Need Help?
If you’re unsure how to identify or fix the issue, please open a [GitHub Issue](https://github.com/Harveykang/AsyncParticles/issues) or start a [Discussion](https://github.com/Harveykang/AsyncParticles/discussions).

- In version x.4.0, changed the default particle rendering mode to `SYNCHRONOUSLY` to avoid rare and confusing `ConcurrentModificationException`s when used with certain mods.
  - If you're still using an older version (≤ x.3.0), you may want to manually set it to `SYNCHRONOUSLY` in the config screen.
  - If you're a modpack author or want to test whether a different rendering mode (e.g., `ASYNCHRONOUSLY`) provides a noticeable FPS boost in your modpack, you can manually select another enum value in the config screen—use with caution.

## Features
- Flexible mixin options.
- Minecraft
  - Particle culling.
  - GPU particle acceleration(v20.1+ only).
  - Async particle tick&rendering.
  - Async particle light cache.
  - Delayed texture tick by one frame to reduce client tick duration.
  - Async rain&snow tick&rendering(buffer filling). (currently only supported in MC1.21.5+)
- Create
  - Contraptions now block vanilla rains(v20.1+ only).
- Valkyrien Skies/Create
  - Particles can now collide with ships/contraptions.
- Valkyrien Skies/Create + Simple Weather/Pretty Rain/Particle Rain
  - Modded weather particles can now collide with ships/contraptions.

## Mods Recommended

- ModernFix
- Sodium/Embeddium
- Flerovium
- Startlight/ScalableLux

## Mod Compatability

### ✅ Compatible (Proactively)

- Sodium/Embeddium
- Flerovium
- Iris/Oculus
- Create
- Create: Aeronautics
- Valkyrien Skies
- Particle Rain
- Simple Weather
- ...

### ❌ Incompatible
- OptiFine
- MadParticle
- Particle Core

## Credits

- [MixinSquared](https://github.com/Bawnorton/MixinSquared)
- [MixinExtras](https://github.com/LlamaLad7/MixinExtras)
- [MixinConstraints](https://github.com/Moulberry/MixinConstraints)
