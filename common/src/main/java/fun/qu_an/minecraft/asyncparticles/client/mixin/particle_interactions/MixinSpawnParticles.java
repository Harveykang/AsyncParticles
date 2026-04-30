package fun.qu_an.minecraft.asyncparticles.client.mixin.particle_interactions;

import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import games.enchanted.blockplaceparticles.particle_spawning.SpawnParticles;
import games.enchanted.blockplaceparticles.particle_spawning.override.BlockParticleOverride;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnParticles.class)
public class MixinSpawnParticles {
    @Inject(method = "spawnBlockBreakParticle", at = @At("HEAD"))
    private static void spawnBlockBreakParticlePre(ClientLevel level, BlockState brokenBlockState, BlockPos brokenBlockPos, BlockParticleOverride particleOverride, CallbackInfo ci) {
        int lightColor = GameUtil.getLightColorFromNeighbor(level, brokenBlockPos);
        GameUtil.DESTRUCTION_LIGHT_CACHE.set(lightColor);
    }

    @Inject(method = "spawnBlockBreakParticle", at = @At("RETURN"))
    private static void spawnBlockBreakParticlePost(ClientLevel level, BlockState brokenBlockState, BlockPos brokenBlockPos, BlockParticleOverride particleOverride, CallbackInfo ci) {
        GameUtil.DESTRUCTION_LIGHT_CACHE.remove();
    }
}
