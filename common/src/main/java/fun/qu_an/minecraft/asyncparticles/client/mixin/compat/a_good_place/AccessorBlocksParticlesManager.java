package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.a_good_place;

import net.minecraft.core.BlockPos;
import nl.enjarai.a_good_place.particles.BlocksParticlesManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(BlocksParticlesManager.class)
public interface AccessorBlocksParticlesManager {
	@Accessor(value = "HIDDEN_BLOCKS", remap = false)
	static Set<BlockPos> accessor_getHiddenBlocks() {
		throw new AssertionError();
	}

	@Invoker(value = "markBlockForRender", remap = false)
	static void invoker_markBlockForRender(BlockPos pos) {
		throw new AssertionError();
	}
}
