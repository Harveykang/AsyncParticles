package fun.qu_an.minecraft.asyncparticles.client.util;

import org.lwjgl.system.MemoryStack;

public class MemStackUtil {
	private static final ParticleThreadLocal<MemoryStack> MEMORY_STACKS = ParticleThreadLocal.withInitial(MemoryStack::stackGet);

	@SuppressWarnings("resource")
	public static MemoryStack stackPush() {
		return stackGet().push();
	}

	public static MemoryStack stackGet() {
		return MEMORY_STACKS.getSafe(MemoryStack::stackGet);
	}
}
