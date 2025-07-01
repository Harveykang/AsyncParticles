# AsyncParticles

- [中文](./README_zh.md)|English
- [Modrinth](https://modrinth.com/mod/asyncparticles)|[CurseForge](https://www.curseforge.com/minecraft/mc-mods/asyncparticles)

## Features

- Minecraft
  - Particle culling.
  - Async particle ticking/rendering(buffer filling).
  - Async particle light cache.
  - Delay texture ticking by one frame to reduce client tick duration.
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

### ❔ Works fine, but not fully tested
- Epic Fight
- Draconic Evolution

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
