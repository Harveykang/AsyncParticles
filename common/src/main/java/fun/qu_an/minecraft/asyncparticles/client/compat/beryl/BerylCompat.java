package fun.qu_an.minecraft.asyncparticles.client.compat.beryl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.beryl.render.RenderingPipeline;
import net.beryl.render.ShaderRenderPipeline;
import net.beryl.render.util.SUtil;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;

import java.util.List;

public class BerylCompat {
	private static final List<Runnable> shaderBegin = new ReferenceArrayList<>();
	private static final List<Runnable> shaderEnd = new ReferenceArrayList<>();

	public static void onShaderBegin() {
		for (Runnable r : shaderBegin) {
			r.run();
		}
	}

	public static void onShaderEnd() {
		for (Runnable r : shaderEnd) {
			r.run();
		}
	}

	public static void linkParticleShaderPipeline(VertexFormat vertexFormat, RenderPipeline pipeline) {
		Supplier<GraphicsPipeline> supplier = Suppliers.memoize(() -> SUtil.createGraphicsPipeline(vertexFormat, "particle/particle"));
		Runnable begin = () -> ShaderRenderPipeline.of(pipeline).redirectPipeline(supplier.get());
		shaderBegin.add(begin);
		shaderEnd.add(() -> ShaderRenderPipeline.of(pipeline).resetPipeline());
		if (RenderingPipeline.isUsingShaderPipeline()) {
			begin.run();
		}
	}
}
