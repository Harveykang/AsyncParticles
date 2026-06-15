package fun.qu_an.minecraft.asyncparticles.client.addon;

public interface GpuParticleAddon {
	int oCOLOR_OFFSET = 48;
	int oCOLOR_RED_OFFSET = oCOLOR_OFFSET;
	int oCOLOR_GREEN_OFFSET = oCOLOR_OFFSET + 1;
	int oCOLOR_BLUE_OFFSET = oCOLOR_OFFSET + 2;
	int oCOLOR_ALPHA_OFFSET = oCOLOR_OFFSET + 3;
	int COLOR_SIZE_FULL = 4;
	int COLOR_SIZE = 1;
	int COLOR_OFFSET = 52;
	int COLOR_RED_OFFSET = COLOR_OFFSET;
	int COLOR_GREEN_OFFSET = COLOR_OFFSET + 1;
	int COLOR_BLUE_OFFSET = COLOR_OFFSET + 2;
	int COLOR_ALPHA_OFFSET = COLOR_OFFSET + 3;

	default void asyncparticles$postTick(long address) {
		throw new IllegalStateException("Not implemented");
	}

	default boolean asyncparticles$shouldRender() {
		throw new IllegalStateException("Not implemented");
	}
}
