# AsyncParticles

- [中文](./README_zh.md)|English
- [Modrinth](https://modrinth.com/mod/asyncparticles)|[CurseForge](https://www.curseforge.com/minecraft/mc-mods/asyncparticles)

## Configuration
- Settings can be accessed via the ModList/ModMenu.

## Features

- Minecraft
  - Fast particle culling.
  - Async particle tick/rendering(buffer filling).
  - Async particle light cache.
  - Delayed texture tick by one frame to reduce client tick duration.
  - Async rain&snow tick/rendering. (currently only supported in MC1.21.5+)
- Valkyrien Skies/Create + Simple Weather/Pretty Rain/Particle Rain
  - Modded weather particles can now collide with ships/contraptions.
- Pretty Rain/Particle Rain/Effectual/Particular/Simple Weather
  - Async particle gen.

## Mods Recommended

- Sodium/Embeddium
- Startlight/ScalableLux
- ModernFix
- Flerovium

## Mod Compatability

### ✅ Compatible (Proactively)

- Sodium/Embeddium
- Flerovium
- Iris/Oculus
- Create
- Valkyrien Skies
- Particle Rain/Pretty Rain
- Simple Weather
- Effectual
- Effective
- Particular
- Particle Core
  - Will disable most of ParticleCore's optimizations while retaining its other functionalities.  
    (Due to incompatibilities with AsyncParticles' asynchronous optimizations.)
- ...

### ❌ Incompatible
- OptiFine
- MadParticle

## Credits

- [Fabric](https://github.com/FabricMC/fabric-loader)
- [NeoForge](https://github.com/neoforged/NeoForge)
- [Mixin](https://github.com/SpongePowered/Mixin)
- [MixinExtras](https://github.com/LlamaLad7/MixinExtras)
- [MixinSquared](https://github.com/Bawnorton/MixinSquared)
- [MixinConstraints](https://github.com/Moulberry/MixinConstraints)
