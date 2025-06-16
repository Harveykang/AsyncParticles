package fun.qu_an.minecraft.asyncparticles.client.compat.iris;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;

public class IrisCompat {
	public static ParticleRenderingSettings getParticleRenderingSettings() {
		if (!IrisApi.getInstance().isShaderPackInUse()) {
			return ParticleRenderingSettings.UNSET;
		}
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
		if (pipeline == null) {
			return ParticleRenderingSettings.UNSET;
		}
		ParticleRenderingSettings settings = pipeline.getParticleRenderingSettings();
		if (settings == null) {
			return ParticleRenderingSettings.UNSET;
		}
		return settings;
	}
}
