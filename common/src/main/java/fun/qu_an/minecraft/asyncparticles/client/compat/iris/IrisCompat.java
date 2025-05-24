package fun.qu_an.minecraft.asyncparticles.client.compat.iris;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import org.jetbrains.annotations.Nullable;

public class IrisCompat {
	@Nullable
	public static ParticleRenderingSettings getParticleRenderingSettings() {
		if (!IrisApi.getInstance().isShaderPackInUse()) {
			return null;
		}
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
		if (pipeline == null) {
			return null;
		}
		return pipeline.getParticleRenderingSettings();
	}
}
