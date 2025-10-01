package fun.qu_an.minecraft.asyncparticles.client.compat.iris;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;

public class IrisCompat {
	public static int getParticleRenderingSettings() {
		if (!IrisApi.getInstance().isShaderPackInUse()) {
			return 0; // UNSET
		}
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
		if (pipeline == null) {
			return 0;
		}
		ParticleRenderingSettings settings = pipeline.getParticleRenderingSettings();
		if (settings == null) {
			return 0;
		}
		int ordinal = settings.ordinal();
		return ModListHelper.IS_LEGACY_IRIS ? ordinal + 1 : ordinal;
	}
}
