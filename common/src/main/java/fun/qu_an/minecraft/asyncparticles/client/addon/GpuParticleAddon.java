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

	default float asyncparticles$getQuadSize(float partialTickTime) {
		throw new IllegalStateException("Not implemented");
	}

	default float asyncparticles$getU0() {
		throw new IllegalStateException("Not implemented");
	}

	default float asyncparticles$getV0() {
		throw new IllegalStateException("Not implemented");
	}

	default float asyncparticles$getU1() {
		throw new IllegalStateException("Not implemented");
	}

	default float asyncparticles$getV1() {
		throw new IllegalStateException("Not implemented");
	}

	default int asyncparticles$getLightCoords(float partialTickTime) {
		throw new IllegalStateException("Not implemented");
	}

	default double asyncparticles$getXo() {
		throw new IllegalStateException("Not implemented");
	}

	default double asyncparticles$getYo() {
		throw new IllegalStateException("Not implemented");
	}

	default double asyncparticles$getZo() {
		throw new IllegalStateException("Not implemented");
	}

	default double asyncparticles$getX() {
		throw new IllegalStateException("Not implemented");
	}

	default double asyncparticles$getY() {
		throw new IllegalStateException("Not implemented");
	}

	default double asyncparticles$getZ() {
		throw new IllegalStateException("Not implemented");
	}

	default float asyncparticles$getORoll() {
		throw new IllegalStateException("Not implemented");
	}

	default float asyncparticles$getRoll() {
		throw new IllegalStateException("Not implemented");
	}

	default int asyncparticles$getOColor() {
		throw new IllegalStateException("Not implemented");
	}

	default int asyncparticles$getColor(int oColor) {
		throw new IllegalStateException("Not implemented");
	}
}
