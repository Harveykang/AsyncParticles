package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.vs2;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.valkyrienskies.mod.common.world.RaycastUtilsKt;

@Mixin(RaycastUtilsKt.class)
public interface InvokerRaycastUtils {
	@Invoker("clip")
	static BlockHitResult invoker_clip(Level level, ClipContext context, Vec3 realStart, Vec3 realEnd) {
		throw new AssertionError();
	}

	@Invoker("vanillaClip")
	static BlockHitResult invoker_vanillaClip(BlockGetter level, ClipContext context) {
		throw new AssertionError();
	}
}
