package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.FrustumUtil;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = WeatherEffectRenderer.class, priority = 500)
public abstract class MixinWeatherEffectRenderer {
	@Shadow
	protected abstract Biome.Precipitation getPrecipitationAt(Level level, BlockPos blockPos);

	@Shadow
	protected abstract WeatherEffectRenderer.ColumnInstance createRainColumnInstance(RandomSource randomSource, int i, int j, int k, int l, int m, int n, float f);

	@Shadow
	protected abstract WeatherEffectRenderer.ColumnInstance createSnowColumnInstance(RandomSource randomSource, int i, int j, int k, int l, int m, int n, float f);

	@Inject(method = "render(Lnet/minecraft/world/level/Level;Lnet/minecraft/client/renderer/MultiBufferSource;IFLnet/minecraft/world/phys/Vec3;)V",
		order = 1500, at = @At(value = "NEW", ordinal = 0, target = "()Ljava/util/ArrayList;"))
	private void onNewArrayList(CallbackInfo ci,
								@Share("renderAsync") LocalBooleanRef renderAsync,
								@Share("rainColumns") LocalRef<List<WeatherEffectRenderer.ColumnInstance>> rainColumns,
								@Share("snowColumns") LocalRef<List<WeatherEffectRenderer.ColumnInstance>> snowColumns) {
		boolean b = ConfigHelper.isRenderWeatherAsync();
		renderAsync.set(b);
		if (!b) {
			return;
		}
		Pair<List<WeatherEffectRenderer.ColumnInstance>, List<WeatherEffectRenderer.ColumnInstance>>
			pair = AsyncRenderer.endWeather();
		rainColumns.set(pair.left());
		snowColumns.set(pair.right());
	}

	@WrapOperation(method = "render(Lnet/minecraft/world/level/Level;Lnet/minecraft/client/renderer/MultiBufferSource;IFLnet/minecraft/world/phys/Vec3;)V",
		at = @At(value = "NEW", target = "()Ljava/util/ArrayList;"))
	private ArrayList<?> redirectNewArrayList(Operation<ArrayList<?>> original,
											  @Share("renderAsync") LocalBooleanRef renderAsync) {
		return renderAsync.get() ? null : new ArrayList<>();
	}

	@WrapWithCondition(method = "render(Lnet/minecraft/world/level/Level;Lnet/minecraft/client/renderer/MultiBufferSource;IFLnet/minecraft/world/phys/Vec3;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;collectColumnInstances(Lnet/minecraft/world/level/Level;IFLnet/minecraft/world/phys/Vec3;ILjava/util/List;Ljava/util/List;)V"))
	private boolean redirectCollectColumnInstances(WeatherEffectRenderer instance,
												   Level level,
												   int i,
												   float f,
												   Vec3 vec3,
												   int j,
												   List<WeatherEffectRenderer.ColumnInstance> list,
												   List<WeatherEffectRenderer.ColumnInstance> list2,
												   @Share("renderAsync") LocalBooleanRef renderAsync) {
		// no-op
		return !renderAsync.get();
	}

	@ModifyVariable(method = "render(Lnet/minecraft/world/level/Level;Lnet/minecraft/client/renderer/MultiBufferSource;IFLnet/minecraft/world/phys/Vec3;)V",
		ordinal = 0,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;collectColumnInstances(Lnet/minecraft/world/level/Level;IFLnet/minecraft/world/phys/Vec3;ILjava/util/List;Ljava/util/List;)V"))
	private List<WeatherEffectRenderer.ColumnInstance>
	modifyRainColumns(List<WeatherEffectRenderer.ColumnInstance> original,
					  @Share("renderAsync") LocalBooleanRef renderAsync,
					  @Share("rainColumns") LocalRef<List<WeatherEffectRenderer.ColumnInstance>> rainColumns) {
		return renderAsync.get() ? rainColumns.get() : original;
	}

	@ModifyVariable(method = "render(Lnet/minecraft/world/level/Level;Lnet/minecraft/client/renderer/MultiBufferSource;IFLnet/minecraft/world/phys/Vec3;)V",
		ordinal = 1,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;collectColumnInstances(Lnet/minecraft/world/level/Level;IFLnet/minecraft/world/phys/Vec3;ILjava/util/List;Ljava/util/List;)V"))
	private List<WeatherEffectRenderer.ColumnInstance>
	modifySnowColumns(List<WeatherEffectRenderer.ColumnInstance> original,
					  @Share("renderAsync") LocalBooleanRef renderAsync,
					  @Share("snowColumns") LocalRef<List<WeatherEffectRenderer.ColumnInstance>> snowColumns) {
		return renderAsync.get() ? snowColumns.get() : original;
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public final void collectColumnInstances(
		Level level, int i, float f, Vec3 vec3, int j, List<WeatherEffectRenderer.ColumnInstance> list, List<WeatherEffectRenderer.ColumnInstance> list2
	) {
		int k = Mth.floor(vec3.x);
		int l = Mth.floor(vec3.y);
		int m = Mth.floor(vec3.z);
		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
		RandomSource randomSource = RandomSource.create();

		boolean enableCull = ConfigHelper.isCullWeathers();
		for (int z = m - j; z <= m + j; z++) {
			for (int x = k - j; x <= k + j; x++) {
				int p = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				int bY = Math.max(l - j, p);
				int tY = Math.max(l + j, p);
				if (tY - bY == 0 ||
					(enableCull && !FrustumUtil.isColumnVisible(AsyncRenderer.frustum, x, z, bY, tY))) {
					continue;
				}
				Biome.Precipitation precipitation = this.getPrecipitationAt(level, mutableBlockPos.set(x, l, z));
				if (precipitation == Biome.Precipitation.NONE) {
					continue;
				}
				int s = x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761;
				randomSource.setSeed(s);
				int t = Math.max(l, p);
				int u = LevelRenderer.getLightColor(level, mutableBlockPos.set(x, t, z));
				if (precipitation == Biome.Precipitation.RAIN) {
					list.add(this.createRainColumnInstance(randomSource, i, x, bY, tY, z, u, f));
				} else if (precipitation == Biome.Precipitation.SNOW) {
					list2.add(this.createSnowColumnInstance(randomSource, i, x, bY, tY, z, u, f));
				}
			}
		}
	}
}
