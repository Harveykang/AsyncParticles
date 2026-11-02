package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.IGlParticlePipeline;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.ParticleTransformFeedbackShader;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.IVkParticlePipeline;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.VkCompParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.VkLayoutResult;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.vulkan.Renderer;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ParticlePipeline implements IGlParticlePipeline<ParticlePipeline.State>,
	IVkParticlePipeline<ParticlePipeline.State> {
	@Override
	public State createGlState() {
		return new State();
	}

	@Override
	public State createVkState() {
		return null;
	}

	@Override
	public IParticleBatchKey batchKey(TextureSheetParticle particle) {
		return (IParticleBatchKey) particle.getRenderType();
	}

	@Override
	public IParticleRenderKey renderKey(ParticleRenderType layer) {
		return (IParticleRenderKey) layer;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public <T extends Collection<TextureSheetParticle>> Map<IParticleBatchKey, Collection<TextureSheetParticle>>
	groupParticles(Map<ParticleRenderType, T> particles) {
		return (Map) particles;
	}

	@Override
	public LayerBatch createBatch(IParticleBatchKey key, Vec3 snapshotCamPos) {
		return new LayerBatch(key);
	}

	@Override
	public Vec3 positionOrigin(LayerBatch batch, Vec3 snapshotCamPos) {
		return snapshotCamPos;
	}

	@Override
	public IComputeReuseCondition prepareView(State state, List<LayerBatch> batches, Camera camera, float partialTicks) {
		return IComputeReuseCondition.DUMMY;
	}

	@Override
	public void prepareGlCompute(State state,
								 Camera camera,
								 float partialTicks,
								 Vec3 snapshotCamPos,
								 int particleCount,
								 int transformFeedback,
								 int targetBuffer) {
		ParticleTransformFeedbackShader.INSTANCE.use();
		Vec3 position = camera.getPosition();
		ParticleTransformFeedbackShader.INSTANCE.setup(
			partialTicks,
			camera.getLeftVector().x(), camera.getLeftVector().y(), camera.getLeftVector().z(),
			camera.getUpVector().x(), camera.getUpVector().y(), camera.getUpVector().z(),
			(float) (snapshotCamPos.x - position.x),
			(float) (snapshotCamPos.y - position.y),
			(float) (snapshotCamPos.z - position.z));
		if (transformFeedback == 0) {
			long particleBytes = 4L * ParticleVertexFormats.IDENTITY_PARTICLE.getVertexSize();
			Backends.glTf.glBindTransformFeedbackBufferRange(0, 0, targetBuffer, 0L, particleCount * particleBytes);
		}
	}

	@Override
	public void drawGlCompute(State state,
							  Camera camera,
							  float partialTicks,
							  Vec3 snapshotCamPos,
							  List<LayerBatch> batches,
							  int particleLimit) {
		Backends.gl.glMultiDrawArrays(GL11C.GL_POINTS, state.multiDrawFirst, state.multiDrawCount);
	}

	@Override
	public void dispatchVkBatches(State state,
								  long pipelineLayout,
								  ByteBuffer constants,
								  List<LayerBatch> batches,
								  Vec3 snapshotCamPos,
								  Camera camera,
								  float partialTicks,
								  int groupCount) {
		Vec3 position = camera.getPosition();
		VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
		constants.putFloat(28, (float) (snapshotCamPos.x - position.x));
		constants.putFloat(32, (float) (snapshotCamPos.y - position.y));
		constants.putFloat(36, (float) (snapshotCamPos.z - position.z));
		constants.putInt(40, 0);
		VK10.vkCmdPushConstants(commandBuffer, pipelineLayout, VK10.VK_SHADER_STAGE_COMPUTE_BIT, 0, constants);
		VK10.vkCmdDispatch(commandBuffer, groupCount, 1, 1);
	}

	@Override
	public ParticleLayout buildGlLayout(State state, List<LayerBatch> batches, int particleLimit) {
		int size = batches.size();
		Map<IParticleRenderKey, ParticleLayout.Slice> slices = new Object2ReferenceOpenHashMap<>(size);
		IntArrayList first = new IntArrayList(size * 2);
		IntArrayList count = new IntArrayList(size * 2);
		int totalCount = 0;
		for (LayerBatch batch : batches) {
			int tickCount = batch.tickCount;
			int appendCount = batch.appendCount;
			if (tickCount > 0) {
				first.add(batch.tickOffset);
				count.add(tickCount);
			}
			if (appendCount > 0) {
				first.add(particleLimit + batch.appendOffset);
				count.add(appendCount);
			}
			batch.targetOffset = totalCount;
			int batchCount = tickCount + appendCount;
			if (batchCount > 0) {
				slices.put((IParticleRenderKey) batch.key, new ParticleLayout.Slice(batch.key.layer(), totalCount, batchCount));
				totalCount += batchCount;
			}
		}
		state.multiDrawFirst = first.toIntArray();
		state.multiDrawCount = count.toIntArray();
		return ParticleLayout.of(totalCount, slices);
	}

	@Override
	public VkLayoutResult buildVkLayout(List<LayerBatch> batches, int particleLimit) {
		Map<IParticleRenderKey, ParticleLayout.Slice> slices = new Object2ReferenceOpenHashMap<>(batches.size());
		int totalCount = 0;
		int groups = 0;
		for (LayerBatch batch : batches) {
			batch.targetOffset = totalCount;
			int count = batch.count();
			if (count > 0) {
				slices.put((IParticleRenderKey) batch.key, new ParticleLayout.Slice(batch.key.layer(), totalCount, count));
				totalCount += count;
			}
			groups += (batch.tickCount + VkCompParticleRenderer.WORKGROUP_SIZE - 1) / VkCompParticleRenderer.WORKGROUP_SIZE;
			groups += (batch.appendCount + VkCompParticleRenderer.WORKGROUP_SIZE - 1) / VkCompParticleRenderer.WORKGROUP_SIZE;
		}
		return new VkLayoutResult(ParticleLayout.of(totalCount, slices), groups);
	}

	public static class State {
		int[] multiDrawFirst;
		int[] multiDrawCount;
	}
}
