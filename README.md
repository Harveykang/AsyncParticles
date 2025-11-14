# This repository is archived and no longer maintained.

Mojang will separate the render and main threads in newer versions of Minecraft, making this mod largely obsolete. Additionally, I currently don’t have the time to maintain the project.  

Thank you to everyone who used, supported, or contributed to it!

# AsyncParticles

- [中文](./README_zh.md)|English
- [Modrinth](https://modrinth.com/mod/asyncparticles)|[CurseForge](https://www.curseforge.com/minecraft/mc-mods/asyncparticles)

## Features

- Minecraft
    - Async particle ticking/rendering(buffer filling).
    - Async particle light cache.

## Mods Recommended

- ModernFix
- Sodium
- Flerovium
- ScalableLux

## Mod Compatability

### ❌ Incompatible
- OptiFine
- MadParticle

## License
-    0.x   LGPL-3.0
-    1.x   MIT
- <= ?.4.x MIT
- \> ?.4.x LGPL-3.0

## Credits

- [wahfl2/sodium-fabric](https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16) for the approach to detect GPU-acceleratable particles.
- [MixinSquared](https://github.com/Bawnorton/MixinSquared) for the amazing mixin-based framework.
- [MixinConstraints](https://github.com/Moulberry/MixinConstraints) for the version checking code.
