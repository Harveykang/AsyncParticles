# AsyncParticles

- [中文](./README_zh.md)|English
- [Modrinth](https://modrinth.com/mod/asyncparticles)|[CurseForge](https://www.curseforge.com/minecraft/mc-mods/asyncparticles)

## Features

- Minecraft
  - High performance/compatibility gpu particle acceleration.
  - Particle culling.
  - Async particle ticking/rendering(buffer filling).
  - Async particle light cache.
- Create + Simple Weather/Pretty Rain/Particle Rain
  - Weather particles can now collide with contraptions.
- Valkyrien Skies + Pretty Rain/Particle Rain
  - Weather particles can now collide with ships.
- Pretty Rain/Particle Rain/Effectual/Particular/Simple Weather
  - Async particle gen.

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
- Valkyrien Skies
- Particle Rain/Pretty Rain
- Simple Weather
- Effectual
- Effective
- Particular
- Particle Core
  - Will disable most of ParticleCore's optimizations while retaining its other functionalities.  
    (Due to incompatibilities with asynchronous optimizations.)
- ...

### ❔ Works fine, but not fully tested
- Epic Fight
- Draconic Evolution

### ❌ Incompatible
- OptiFine
- MadParticle

## License
-   1.x   MIT
- < ?.4.0 MIT
- \>=?.4.0 LGPL-3.0

## Credits

- [wahfl2/sodium-fabric](https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16) for the approach to detect GPU-acceleratable particles.
- [Flerovium](https://github.com/MoePus/Flerovium) for the faster particle rendering code.
- [MixinSquared](https://github.com/Bawnorton/MixinSquared) for the amazing mixin-based framework.
- [MixinConstraints](https://github.com/Moulberry/MixinConstraints) for the version checking code.
