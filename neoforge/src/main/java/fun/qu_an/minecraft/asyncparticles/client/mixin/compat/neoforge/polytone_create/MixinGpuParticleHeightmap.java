package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.polytone_create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.ContraptionHeightMap;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.ContraptionRainBlocking;
import net.mehvahdjukaar.polytone.content.particle.gpu.GpuParticleHeightmap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GpuParticleHeightmap.class, remap = false)
public abstract class MixinGpuParticleHeightmap {
	@WrapOperation(method = "update", at = @At(value = "INVOKE", target =
		"Lnet/minecraft/client/multiplayer/ClientLevel;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
	private int includeContraptionHeight(ClientLevel level, Heightmap.Types type, int x, int z, Operation<Integer> original) {
		int height = original.call(level, type, x, z);
		ContraptionHeightMap.State state = ContraptionRainBlocking.getAttachedContractionHeightMap(level).getState();
		return Math.max(height, Mth.ceil(state.getHeight(x, z)));
	}
}
