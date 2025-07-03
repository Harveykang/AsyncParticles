package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.WeatherRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.WeatherEffectRendererAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.FrustumUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = WeatherEffectRenderer.class, priority = 1500)
public abstract class MixinWeatherEffectRenderer implements WeatherEffectRendererAddon {
	@Unique
	private boolean asyncparticles$beginingPhase = false;

	public void asyncparticles$onBegin() {
		asyncparticles$beginingPhase = true;
	}

	@Shadow protected abstract void render(MultiBufferSource multiBufferSource, Vec3 vec3, int i, float f, List<WeatherEffectRenderer.ColumnInstance> list, List<WeatherEffectRenderer.ColumnInstance> list2);

	@Inject(method = "render(Lnet/minecraft/world/level/Level;Lnet/minecraft/client/renderer/MultiBufferSource;IFLnet/minecraft/world/phys/Vec3;)V",
		order = 1500, at = @At(value = "NEW", ordinal = 0, target = "()Ljava/util/ArrayList;"), cancellable = true)
	private void onNewArrayList(Level level,
								MultiBufferSource multiBufferSource,
								int ticks,
								float partialTick,
								Vec3 cameraPos,
								CallbackInfo ci,
								@Local(ordinal = 1) int rainDistance,
								@Local(ordinal = 1) float rainLevel) {
		if (ConfigHelper.isRenderWeatherAsync()) {
			ci.cancel();
			if (asyncparticles$beginingPhase) {
				WeatherRenderer.beginWeather(partialTick, cameraPos, rainDistance, (WeatherEffectRenderer) (Object) this, ticks);
				asyncparticles$beginingPhase = false;
			} else {
				render(null, cameraPos, rainDistance, rainLevel, null, null);
			}
		}
	}

	@Redirect(method = "render(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/phys/Vec3;IFLjava/util/List;Ljava/util/List;)V",
		at = @At(value = "INVOKE", ordinal = 0, target = "Ljava/util/List;isEmpty()Z"))
	private boolean redirectIsEmpty0(List<WeatherEffectRenderer.ColumnInstance> list) {
		return list != null ? list.isEmpty() : !WeatherRenderer.shouldRenderRain();
	}

	@Redirect(method = "render(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/phys/Vec3;IFLjava/util/List;Ljava/util/List;)V",
		at = @At(value = "INVOKE", ordinal = 1, target = "Ljava/util/List;isEmpty()Z"))
	private boolean redirectIsEmpty1(List<WeatherEffectRenderer.ColumnInstance> list) {
		return list != null ? list.isEmpty() : !WeatherRenderer.shouldRenderSnow();
	}

	@WrapOperation(method = "render(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/phys/Vec3;IFLjava/util/List;Ljava/util/List;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer redirectGetBuffer(MultiBufferSource instance, RenderType renderType, Operation<VertexConsumer> original) {
		if (instance == null) {
			return null;
		}
		return original.call(instance, renderType);
	}

	@WrapOperation(method = "render(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/phys/Vec3;IFLjava/util/List;Ljava/util/List;)V",
		at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;renderInstances(Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/phys/Vec3;FIF)V"))
	private void wrapRenderRainInstances(WeatherEffectRenderer instance,
										 VertexConsumer vertexConsumer,
										 List<WeatherEffectRenderer.ColumnInstance> list,
										 Vec3 vec3,
										 float f,
										 int i,
										 float g,
										 Operation<Void> original,
										 @Local(ordinal = 0) RenderType renderType) {
		if (vertexConsumer == null) {
			WeatherRenderer.endRain(renderType);
		} else {
			original.call(instance, vertexConsumer, list, vec3, f, i, g);
		}
	}

	@WrapOperation(method = "render(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/phys/Vec3;IFLjava/util/List;Ljava/util/List;)V",
		at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;renderInstances(Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/phys/Vec3;FIF)V"))
	private void wrapRenderSnowInstances(WeatherEffectRenderer instance,
										 VertexConsumer vertexConsumer,
										 List<WeatherEffectRenderer.ColumnInstance> list,
										 Vec3 vec3,
										 float f,
										 int i,
										 float g,
										 Operation<Void> original,
										 @Local(ordinal = 0) RenderType renderType) {
		if (vertexConsumer == null) {
			WeatherRenderer.endSnow(renderType);
		} else {
			original.call(instance, vertexConsumer, list, vec3, f, i, g);
		}
	}

	@Inject(method = "collectColumnInstances", at = @At(value = "HEAD"))
	private void onCollectColumnInstances(CallbackInfo ci, @Share("enableCull") LocalBooleanRef enableCull) {
		enableCull.set(ConfigHelper.isCullWeathers());
	}

	@WrapOperation(method = "collectColumnInstances", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;getPrecipitationAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation wrapGetPrecipitationAt(WeatherEffectRenderer instance,
													   Level level,
													   BlockPos blockPos,
													   Operation<Biome.Precipitation> original,
													   @Share("enableCull") LocalBooleanRef enableCull,
													   @Local(ordinal = 8) int bY,
													   @Local(ordinal = 9) int tY) {
		if (enableCull.get() && !FrustumUtil.isColumnVisible(AsyncRenderer.frustum, blockPos.getX(), blockPos.getZ(), bY, tY)) {
			return Biome.Precipitation.NONE;
		} else {
			return original.call(instance, level, blockPos);
		}
	}
}
