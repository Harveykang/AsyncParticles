# AsyncParticles

- It will remain in the beta stage in the future to alert potential mod conflict issues.
- Now compatible with Particle Core(v20.1+).
- Now compatible with Valkyrien Skies v2.4.

## Core Features

### GPU Acceleration
- Offloading partricle vertex transformations to the GPU reduces per-frame data uploads to the GPU, lowers CPU load, and improves frame rates. 
- This essentially replaces the previous async partricle vertex extraction while also improving compatibility (async partricle extraction is now **disabled** by default).

### Create Integrations
- Create contraptions can now block vanilla weather.(v20.1.0a+) 
  - Will move this to a new mod when I get free time.
- Particles can now collide with contraptions.

### Other Features
- Flexible mixin options.
- Minecraft
  - Particle culling.
  - Async particle tick.
  - Async particle light cache.
  - Delayed texture tick by one frame to reduce client tick duration.

## Configuration
### Settings can be accessed via the ModList/ModMenu.

## Troubleshooting

### Use with BadOptimizations
- Set `enable_particle_manager_optimization: false` in `badoptimizations.txt`
- Fixed since `20.1.1.0`/`21.1.1.0`.

### Use with PhysicsMod
- Disable `Async Weather Tick`.
- **No compatibility guaranteed**.

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
- Particle Core
- ...

### ❌ Incompatible
- OptiFine
- MadParticle

## Credits

- [MixinSquared](https://github.com/Bawnorton/MixinSquared)
- [MixinExtras](https://github.com/LlamaLad7/MixinExtras)
- [MixinConstraints](https://github.com/Moulberry/MixinConstraints)
