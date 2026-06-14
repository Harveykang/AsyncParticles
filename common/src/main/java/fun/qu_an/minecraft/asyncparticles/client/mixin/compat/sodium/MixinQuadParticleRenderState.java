package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sodium;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// To be compatible with async buffer filling
// Just trust the JIT
@Mixin(value = QuadParticleRenderState.class, priority = 1500)
public class MixinQuadParticleRenderState {
	@Unique
	private static final ParticleThreadLocal<Quaternionf> asyncparticles$TEMP_QUAT = ParticleThreadLocal.withInitial(RenderSystem::isOnRenderThread, Quaternionf::new);
	@Unique
	private static final ParticleThreadLocal<Vector3f> asyncparticles$TEMP_VEC = ParticleThreadLocal.withInitial(RenderSystem::isOnRenderThread, Vector3f::new);

	@Dynamic
	@TargetHandler(
		mixin = "net.caffeinemc.mods.sodium.mixin.features.render.particle.QuadParticleRenderStateMixin",
		name = "render"
	)
	@ModifyExpressionValue(method = "@MixinSquared:Handler", expect = 2, at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;TEMP_QUAT:Lorg/joml/Quaternionf;",
		opcode = Opcodes.GETSTATIC))
	private static Quaternionf modifyTempQuat(Quaternionf original) {
		return asyncparticles$TEMP_QUAT.getSafe(original);
	}

	@Dynamic
	@TargetHandler(
		mixin = "net.caffeinemc.mods.sodium.mixin.features.render.particle.QuadParticleRenderStateMixin",
		name = "sodium$emitVertices"
	)
	@ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;TEMP_VECTOR:Lorg/joml/Vector3f;",
		opcode = Opcodes.GETSTATIC))
	private static Vector3f modifyTempVec(Vector3f original) {
		return asyncparticles$TEMP_VEC.getSafe(original);
	}

	@TargetHandler(
		mixin = "net.caffeinemc.mods.sodium.mixin.features.render.particle.QuadParticleRenderStateMixin",
		name = "sodium$emitVertices"
	)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", remap = false,
		target = "Lorg/lwjgl/system/MemoryStack;stackPush()Lorg/lwjgl/system/MemoryStack;"))
	private static MemoryStack redirectStackPush() {
		return MemStackUtil.stackPush();
	}
}
