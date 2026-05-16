package fun.qu_an.minecraft.asyncparticles.client.mixin.core.weather;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.FrustumUtil;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer_WeatherCulling {
	@Shadow
	private Frustum cullingFrustum;
	@Unique
	private static final Holder<Biome> asyncparticles$NULL = Holder.direct(null);

	@Inject(method = "renderSnowAndRain", at = @At(value = "HEAD"))
	public void beforeRenderSnowAndRain(CallbackInfo ci,
										@Share(namespace = AsyncParticlesClient.MOD_ID, value = "enableCull")
										LocalBooleanRef enableCull) {
		enableCull.set(ConfigHelper.isCullWeathers());
	}

	@Inject(method = "renderSnowAndRain", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
	public void beforeGetBiome(LightTexture lightTexture,
							   float partialTick,
							   double camX,
							   double camY,
							   double camZ,
							   CallbackInfo ci,
							   @Local(ordinal = 0) Level level,
							   @Local(ordinal = 0) BlockPos.MutableBlockPos mutableBlockPos,
							   @Local(ordinal = 1) int j,
							   @Local(ordinal = 3) int l,
							   @Local(ordinal = 5) int n,
							   @Local(ordinal = 6) int o,
							   @Share(namespace = AsyncParticlesClient.MOD_ID, value = "enableCull")
							   LocalBooleanRef enableCull,
							   @Share(namespace = AsyncParticlesClient.MOD_ID, value = "height_map")
							   LocalIntRef qRef,
							   @Share(value = "isVisible") LocalBooleanRef isVisible) {
		if (!enableCull.get()) {
			isVisible.set(true);
			return;
		}
		int q = level.getHeight(Heightmap.Types.MOTION_BLOCKING, o, n);
		int s = j + l;
		if (s < q) {
			isVisible.set(false);
			return;
		}
		int r = j - l;
		if (r < q) {
			r = q;
		}
		if (FrustumUtil.isColumnVisible(cullingFrustum, mutableBlockPos.getX(), mutableBlockPos.getZ(), r, s)) {
			qRef.set(q);
			isVisible.set(true);
		} else {
			isVisible.set(false);
		}
	}

	@WrapOperation(method = "renderSnowAndRain", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
	public Holder<Biome> wrapGetBiome(Level instance, BlockPos pos,
									  Operation<Holder<Biome>> original,
									  @Share(value = "isVisible") LocalBooleanRef isVisible) {
		return isVisible.get() ? original.call(instance, pos) : asyncparticles$NULL;
	}

	@WrapOperation(method = "renderSnowAndRain", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/biome/Biome;hasPrecipitation()Z"))
	public boolean shouldRenderWeatherColumn(Biome instance, Operation<Boolean> original,
											 @Share(value = "isVisible") LocalBooleanRef isVisible) {
		return isVisible.get() && original.call(instance);
	}
}
