package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sable;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

@Mixin(value = Particle.class, priority = 1500)
public abstract class MixinParticle_LightCache implements LightCachedParticleAddon, ParticleExtension {
	@Unique
	private static final MethodHandle asyncparticles$sableLightHandler = asyncparticles$findSableLightHandler();
	@Shadow
	public double x;
	@Shadow
	public double y;
	@Shadow
	public double z;
	@Shadow
	public double xo;
	@Shadow
	public double yo;
	@Shadow
	public double zo;
	@Shadow
	@Final
	public ClientLevel level;
	@Unique
	private WeakReference<ClientSubLevel> asyncparticles$lightSubLevel = new WeakReference<>(null);
	@Unique
	private BlockPos asyncparticles$lightDetachPos;

	@Unique
	private static MethodHandle asyncparticles$findSableLightHandler() {
		try {
			for (Method method : Particle.class.getDeclaredMethods()) {
				MixinMerged merged = method.getAnnotation(MixinMerged.class);
				if (merged != null && merged.mixin().equals("dev.ryanhcode.sable.mixin.particle.ParticleMixin")
					&& method.getName().contains("sable$checkSubLevelLightColor")
					&& method.getReturnType() == void.class
					&& method.getParameterCount() == 2
					&& method.getParameterTypes()[0] == float.class
					&& method.getParameterTypes()[1] == CallbackInfoReturnable.class) {
					return MethodHandles.lookup().unreflect(method);
				}
			}
			throw new NoSuchMethodException("Sable particle light handler not found");
		} catch (Throwable e) {
			throw ExceptionUtil.toThrowDirectly(e);
		}
	}

	@Dynamic
	@TargetHandler(mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.light_cache.MixinParticle_LightCache",
		name = "asyncparticles$refresh")
	@WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
	private int wrapGetLightColor(BlockAndTintGetter level, BlockPos blockPos, Operation<Integer> original) {
		return asyncparticles$calcLight(level, blockPos, original);
	}

	@Dynamic
	@TargetHandler(mixin = "dev.ryanhcode.sable.mixin.particle.ParticleMixin", name = "sable$checkSubLevelLightColor")
	@ModifyExpressionValue(method = "@MixinSquared:Handler", remap = false,
		at = @At(value = "FIELD", target = "sable$trackingSubLevel:Ldev/ryanhcode/sable/sublevel/ClientSubLevel;", opcode = Opcodes.GETFIELD))
	private ClientSubLevel extendLightTracking(ClientSubLevel original, @Share("lightSubLevel") LocalRef<ClientSubLevel> subLevel) {
		return !ConfigHelper.fixParticleLightOnSableSublevel() ? original : subLevel.get();
	}

	@Dynamic
	@TargetHandler(mixin = "dev.ryanhcode.sable.mixin.particle.ParticleMixin", name = "sable$checkSubLevelLightColor")
	@Inject(method = "@MixinSquared:Handler", remap = false, at = @At("HEAD"), cancellable = true)
	private void resolveLightTracking(CallbackInfo ci, @Share("lightSubLevel") LocalRef<ClientSubLevel> subLevelRef) {
		ClientSubLevel subLevel = asyncparticles$getExtendedTrackingSubLevel();
		if (subLevel == null && ConfigHelper.simplifySableParticleSkyLight()) {
			ci.cancel();
			return;
		}
		subLevelRef.set(subLevel);
	}

	@Dynamic
	@TargetHandler(mixin = "dev.ryanhcode.sable.mixin.particle.ParticleMixin", name = "sable$checkSubLevelLightColor")
	@Inject(method = "@MixinSquared:Handler", remap = false, at = @At(value = "FIELD",
		target = "sable$trackingSubLevel:Ldev/ryanhcode/sable/sublevel/ClientSubLevel;", ordinal = 0, opcode = Opcodes.GETFIELD),
		cancellable = true)
	private void onLightCheck(float f,
							  CallbackInfoReturnable<Integer> cir,
							  CallbackInfo ci,
							  @Share("lightSubLevel") LocalRef<ClientSubLevel> subLevelRef,
							  @Local(ordinal = 0) BlockPos pos,
							  @Local(ordinal = 0) BlockState state) {
		ClientSubLevel subLevel = subLevelRef.get();
		if (subLevel != null && ConfigHelper.simplifySableParticleSkyLight()) {
			cir.setReturnValue(asyncparticles$getPackedLight(pos, state, subLevel));
			ci.cancel();
		}
	}

	@Unique
	private int asyncparticles$getPackedLight(BlockPos pos, BlockState state, ClientSubLevel subLevel) {
		int blockLight = this.level.getBrightness(LightLayer.BLOCK, pos);
		int skyLight = this.level.getBrightness(LightLayer.SKY, pos);
		Vector3d particlePos = GameUtil.SHARED_VECTOR3D.get().set(x, y, z);
		subLevel.logicalPose().transformPositionInverse(particlePos);
		BlockPos.MutableBlockPos localPos = GameUtil.SHARED_POS.get();
		localPos.set(particlePos.x, particlePos.y, particlePos.z);
		blockLight = Math.max(blockLight, subLevel.getLevel().getBrightness(LightLayer.BLOCK, localPos));
		skyLight = Math.min(skyLight, subLevel.scaleSkyLight(this.level.getBrightness(LightLayer.SKY, localPos)));

		int k = state.getLightEmission();
		if (blockLight < k) {
			blockLight = k;
		}

		return LightTexture.pack(blockLight, skyLight);
	}

	@Unique
	private @Nullable ClientSubLevel asyncparticles$getExtendedTrackingSubLevel() {
		ClientSubLevel trackingSubLevel = (ClientSubLevel) sable$getTrackingSubLevel();
		if (!ConfigHelper.fixParticleLightOnSableSublevel()) {
			return trackingSubLevel;
		}
		if (trackingSubLevel != null) {
			if (asyncparticles$lightSubLevel.get() != trackingSubLevel) {
				asyncparticles$lightSubLevel = new WeakReference<>(trackingSubLevel);
			}
			asyncparticles$lightDetachPos = null;
			return trackingSubLevel;
		}
		if ((trackingSubLevel = asyncparticles$lightSubLevel.get()) == null || trackingSubLevel.isRemoved()) {
			asyncparticles$lightDetachPos = null;
			return null;
		}
		if (asyncparticles$lightDetachPos == null) {
			asyncparticles$lightDetachPos = BlockPos.containing(xo, yo, zo);
		}
		if (asyncparticles$lightDetachPos.distManhattan(GameUtil.SHARED_POS.get().set(x, y, z)) <= 15) {
			return trackingSubLevel;
		}
		return null;
	}

	@Unique
	protected int asyncparticles$calcLight(BlockAndTintGetter level, BlockPos blockPos, Operation<Integer> original) {
		CallbackInfoReturnable<Integer> cir = new CallbackInfoReturnable<>("asyncparticles$refresh", true);
		try {
			asyncparticles$sableLightHandler.invokeExact((Particle) (Object) this, 0f, cir);
		} catch (Throwable t) {
			throw ExceptionUtil.toThrowDirectly(t);
		}
		return cir.isCancelled() ? cir.getReturnValueI() : original.call(level, blockPos);
	}
}
