package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneParticleTypeAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleType;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleLightCache;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomParticleInstance.class)
public abstract class MixinCustomParticleInstance extends TextureSheetParticle implements PolytoneParticleAddon, ParticleAddon, LightCachedParticleAddon {
	@Shadow(remap = false)
	@Final
	public CustomParticleType type;
	@Shadow(remap = false)
	@Final
	protected BakedModel model;
	@Unique
	private boolean asyncparticles$lockTickAndRender;

	protected MixinCustomParticleInstance(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Override
	@Invoker(value = "initTick", remap = false)
	public abstract void asyncparticles$initTick();

	@Override
	public boolean asyncparticles$canRenderFast() {
		return ConfigHelper.isPolytoneGpuRendering()
			&& ((Object) this).getClass() == CustomParticleInstance.class
			&& model == null
			&& ((PolytoneParticleTypeAddon) type).asyncparticles$isSimpleQuad();
	}

	@Override
	public boolean asyncparticles$isStaticLight() {
		return !ConfigHelper.isPolytoneLightCache();
	}

	@WrapOperation(method = "getLightColor", at = @At(value = "INVOKE",
		target = "Lnet/mehvahdjukaar/polytone/content/particle/custom/ParticleLightCache$Entry;get(DDDF)I", remap = false))
	private int useCurrentLight(ParticleLightCache.Entry cache,
	                            double x,
	                            double y,
	                            double z,
	                            float partialTick,
	                            Operation<Integer> original) {
		if (asyncparticles$isEnabledLightCache()) {
			return super.getLightColor(partialTick);
		}
		return original.call(cache, x, y, z, partialTick);
	}

	@ModifyExpressionValue(method = "<init>", at = @At(value = "FIELD",
		target = "Lnet/mehvahdjukaar/polytone/content/particle/custom/CustomParticleInstance;STATE_HACK:Lnet/minecraft/world/level/block/state/BlockState;",
		opcode = Opcodes.GETSTATIC))
	private BlockState getSpawnState(BlockState original) {
		return PolytoneCompat.SPAWN_STATE.get();
	}

	@Inject(method = "setStateHack", at = @At("HEAD"), cancellable = true, remap = false)
	private static void setSpawnState(BlockState state, CallbackInfo ci) {
		PolytoneCompat.SPAWN_STATE.set(state);
		ci.cancel();
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void preserveCustomGeometry(CallbackInfo ci) {
		asyncparticles$lockTickAndRender = ConfigHelper.isAsyncParticleTick() && ConfigHelper.isPolytoneAsyncTick()
			&& !ConfigHelper.isPolytoneGpuOnlyAsyncTick() && !asyncparticles$canRenderFast();
		if (model != null || !((PolytoneParticleTypeAddon) type).asyncparticles$isSimpleQuad()) {
			this.asyncparticles$setNoCulling();
		}
	}

	@WrapMethod(method = "tickInternal", remap = false)
	private void tickWithoutConcurrentRender(Operation<Void> original) {
		if (asyncparticles$lockTickAndRender) {
			synchronized (this) {
				original.call();
			}
		} else {
			original.call();
		}
	}

	@WrapMethod(method = "render")
	private void renderWithoutConcurrentTick(VertexConsumer buffer, Camera camera, float partialTick,
	                                         Operation<Void> original) {
		if (asyncparticles$lockTickAndRender) {
			synchronized (this) {
				original.call(buffer, camera, partialTick);
			}
		} else {
			original.call(buffer, camera, partialTick);
		}
	}
}
