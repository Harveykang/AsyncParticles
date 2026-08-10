package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.SingleQuadParticle;

public interface GpuParticleAddon extends LightCachedParticleAddon {
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

	void asyncparticles$postTick(long address);

	boolean asyncparticles$shouldRender();

	float asyncparticles$getQuadSize(float partialTickTime);

	float asyncparticles$getU0();

	float asyncparticles$getV0();

	float asyncparticles$getU1();

	float asyncparticles$getV1();

	int asyncparticles$getGpuLightCoords(float partialTickTime);

	double asyncparticles$getXo();

	double asyncparticles$getYo();

	double asyncparticles$getZo();

	double asyncparticles$getX();

	double asyncparticles$getY();

	double asyncparticles$getZ();

	float asyncparticles$getORoll();

	float asyncparticles$getRoll();

	int asyncparticles$getOColor();

	int asyncparticles$getColor(int oColor);

	SingleQuadParticle.Layer asyncparticles$getLayer();

	float asyncparticles$getAlpha();

	float asyncparticles$getRed();

	float asyncparticles$getGreen();

	float asyncparticles$getBlue();
}
