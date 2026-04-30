package fun.qu_an.minecraft.asyncparticles.client.mixin.particle_interactions;

import fun.qu_an.minecraft.asyncparticles.client.particle.GpuParticleBehavior;
import games.enchanted.blockplaceparticles.particle_spawning.SpawnParticles;
import games.enchanted.blockplaceparticles.particle_spawning.override.BlockParticleOverride;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.joml.Math.max;

@Mixin(SpawnParticles.class)
public class MixinSpawnParticles {
    @Inject(method = "spawnBlockBreakParticle", at = @At("HEAD"))
    private static void spawnBlockBreakParticlePre(ClientLevel level, BlockState brokenBlockState, BlockPos brokenBlockPos, BlockParticleOverride particleOverride, CallbackInfo ci) {
        int lightColor = asyncparticles$getLightColor(level, brokenBlockPos);
        GpuParticleBehavior.INSTANCE.DESTROY_LIGHT_CACHE.set(lightColor);
    }

    @Inject(method = "spawnBlockBreakParticle", at = @At("RETURN"))
    private static void spawnBlockBreakParticlePost(ClientLevel level, BlockState brokenBlockState, BlockPos brokenBlockPos, BlockParticleOverride particleOverride, CallbackInfo ci) {
        GpuParticleBehavior.INSTANCE.DESTROY_LIGHT_CACHE.remove();
    }

    @Unique
    private static int asyncparticles$getLightColor(ClientLevel level, BlockPos pos) {
        var asyncparticles$mutable = new BlockPos.MutableBlockPos();
        LayerLightEventListener sky = level.getLightEngine().getLayerListener(LightLayer.SKY);
        LayerLightEventListener block = level.getLightEngine().getLayerListener(LightLayer.BLOCK);
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int lx = x & 15;
        int ly = y & 15;
        int lz = z & 15;
        int i;
        int j;

        DataLayer skyDataLayerData = sky.getDataLayerData(SectionPos.of(pos));
        if (skyDataLayerData == null) {
            i = 16;
        } else {
            i = ly == 15 ? sky.getLightValue(asyncparticles$mutable.set(x, y + 1, z)) :
                    skyDataLayerData.get(lx, ly + 1, lz);
        }
        DataLayer blockDataLayerData = block.getDataLayerData(SectionPos.of(pos));
        if (blockDataLayerData == null) {
            j = 16;
        } else {
            j = ly == 15 ? block.getLightValue(asyncparticles$mutable.set(x, y + 1, z)) :
                    blockDataLayerData.get(lx, ly + 1, lz);
        }

        if (i < 15) {
            i = max(i, lz == 0 ? sky.getLightValue(asyncparticles$mutable.set(x, y, z - 1)) :
                    skyDataLayerData.get(lx, ly, lz - 1));
        }
        if (j < 15) {
            j = max(j, lz == 0 ? block.getLightValue(asyncparticles$mutable.set(x, y, z - 1)) :
                    blockDataLayerData.get(lx, ly, lz - 1));
        }

        if (i < 15) {
            i = max(i, lx == 0 ? sky.getLightValue(asyncparticles$mutable.set(x - 1, y, z)) :
                    skyDataLayerData.get(lx - 1, ly, lz));
        }
        if (j < 15) {
            j = max(j, lx == 0 ? block.getLightValue(asyncparticles$mutable.set(x - 1, y, z)) :
                    blockDataLayerData.get(lx - 1, ly, lz));
        }

        if (i < 15) {
            i = max(i, lz == 15 ? sky.getLightValue(asyncparticles$mutable.set(x, y, z + 1)) :
                    skyDataLayerData.get(lx, ly, lz + 1));
        }
        if (j < 15) {
            j = max(j, lz == 15 ? block.getLightValue(asyncparticles$mutable.set(x, y, z + 1)) :
                    blockDataLayerData.get(lx, ly, lz + 1));
        }

        if (i < 15) {
            i = max(i, lx == 15 ? sky.getLightValue(asyncparticles$mutable.set(x + 1, y, z)) :
                    skyDataLayerData.get(lx + 1, ly, lz));
        }
        if (j < 15) {
            j = max(j, lx == 15 ? block.getLightValue(asyncparticles$mutable.set(x + 1, y, z)) :
                    blockDataLayerData.get(lx + 1, ly, lz));
        }

        if (i < 15) {
            i = max(i, ly == 0 ? sky.getLightValue(asyncparticles$mutable.set(x, y - 1, z)) :
                    skyDataLayerData.get(lx, ly - 1, lz));
        }
        if (j < 15) {
            j = max(j, ly == 0 ? block.getLightValue(asyncparticles$mutable.set(x, y - 1, z)) :
                    blockDataLayerData.get(lx, ly - 1, lz));
        }

        if (i < 15 && i > 0) {
            --i;
        }
        if (j > 0) {
            --j;
        }
        return (i & 0xF) << 20 | (j & 0xF) << 4;
    }
}
