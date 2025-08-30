# 异步粒子

- 中文|[English](./README.md)
- [Modrinth](https://modrinth.com/mod/asyncparticles)|[CurseForge](https://www.curseforge.com/minecraft/mc-mods/asyncparticles)

## 特性

- Minecraft
  - 高性能/兼容性的GPU粒子加速渲染
  - 粒子剔除
  - 异步粒子刻/渲染（缓冲区填充）
  - 异步粒子光照缓存
- 机械动力 + Simple Weather/Pretty Rain/粒子雨
  - 天气粒子不会穿过动态结构
- 瓦尔基里天空 + Pretty Rain/粒子雨
  - 天气粒子不会穿过船只
- Pretty Rain/粒子雨/Effectual/Particular/Simple Weather
  - 异步粒子生成

## 推荐模组

- 现代化修复
- 钠/Embeddium
- 鈇
- 星光/ScalableLux

## 模组兼容性

### ✅ 主动兼容

- 钠/Embeddium
- 鈇
- Iris/Oculus
- 机械动力
- 瓦尔基里天空
- 粒子雨/Pretty Rain
- Simple Weather
- Effectual
- Effective
- Particular
- Particle Core
  - 会取消 Particle Core 的大部分优化  
    (不兼容异步化)
- ...

### ❔ 看起来没问题
- Epic Fight
- Draconic Evolution

### ❌ 不兼容
- 高清修复
- 疯狂粒子

## 致谢

- [Flerovium](https://github.com/MoePus/Flerovium) 关于快速粒子渲染的代码。
- [MixinSquared](https://github.com/Bawnorton/MixinSquared) 惊人的基于 mixin 的框架。
- [MixinConstraints](https://github.com/Moulberry/MixinConstraints) 关于版本号检查的代码。

# 备忘
- 如果启动不了游戏，报ijKotlinCoroutineJvmDebugInit1相关错误，把 Debugger/Attach Coroutine Agent关了