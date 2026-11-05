package fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals;

import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.*;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.ParticleTransformFeedbackShader;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.IGlParticlePipeline;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.IVkParticlePipeline;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.VkCompParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.VkLayoutResult;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.vulkan.Renderer;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import qouteall.imm_ptl.core.mixin.client.particle.IEParticle;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ImmPtlParticlePipeline implements IGlParticlePipeline<ImmPtlParticlePipeline.State>,
	IVkParticlePipeline<ImmPtlParticlePipeline.State> {
	private static final double REGION_SIZE = 256.0;

	@Override
	public State createGlState() {
		State state = new State();
		state.multiDrawFirst = new int[2];
		state.multiDrawCount = new int[2];
		return state;
	}

	@Override
	public State createVkState() {
		return new State();
	}

	@Override
	public IParticleBatchKey batchKey(TextureSheetParticle particle) {
		// Input key: split each level/layer into regions with separate position origins.
		return new ImmPtlRegionKey(particle.getRenderType(), ((IEParticle) particle).portal_getWorld(),
			region(particle.x), region(particle.y), region(particle.z));
	}

	private static long region(double coordinate) {
		return (long) Math.floor(coordinate / REGION_SIZE);
	}

	private static Vec3 regionOrigin(long x, long y, long z) {
		return new Vec3((x + 0.5) * REGION_SIZE, (y + 0.5) * REGION_SIZE, (z + 0.5) * REGION_SIZE);
	}

	@Override
	public <T extends Collection<TextureSheetParticle>> Map<IParticleBatchKey, Collection<TextureSheetParticle>>
	groupParticles(Map<ParticleRenderType, T> particles) {
		Map<IParticleBatchKey, Collection<TextureSheetParticle>> grouped = new Object2ReferenceOpenHashMap<>(particles.size());
		for (Collection<TextureSheetParticle> layer : particles.values()) {
			for (TextureSheetParticle particle : layer) {
				grouped.computeIfAbsent(batchKey(particle), ignored -> new ReferenceArrayList<>()).add(particle);
			}
		}
		return grouped;
	}

	@Override
	public LayerBatch createBatch(IParticleBatchKey key, Vec3 snapshotCamPos) {
		// Input batches only contain ImmPtlRegionKey, created by batchKey(particle).
		ImmPtlRegionKey region = (ImmPtlRegionKey) key;
		return new ImmPtlLayerBatch(key, regionOrigin(region.x(), region.y(), region.z()));
	}

	@Override
	public Vec3 positionOrigin(LayerBatch batch, Vec3 snapshotCamPos) {
		return ((ImmPtlLayerBatch) batch).origin;
	}

	@Override
	public IParticleRenderKey renderKey(ParticleRenderType layer) {
		// Output key: select the current level/layer from the shared slice map, regardless of region.
		return new ImmPtlRenderKey(layer, Minecraft.getInstance().level);
	}

	@Override
	public IComputeReuseCondition prepareView(State state, List<LayerBatch> batches, Camera camera, float partialTicks) {
		state.firstBatch = 0;
		state.endBatch = 0;
		state.targetOffset = 0;
		state.visibleParticleCount = 0;
		ClientLevel level = Minecraft.getInstance().level;
		boolean found = false;
		for (int i = 0; i < batches.size(); i++) {
			LayerBatch batch = batches.get(i);
			// Select the world using the input ImmPtlRegionKey; output keys only index slices.
			if (((ImmPtlRegionKey) batch.key).level() == level) {
				if (!found) {
					found = true;
					state.firstBatch = i;
					state.targetOffset = batch.targetOffset;
				}
				state.endBatch = i + 1;
				state.visibleParticleCount += batch.count();
			} else if (found) {
				break;
			}
		}
		if (state.visibleParticleCount == 0) {
			return null;
		}
		return ImmPtlComputeReuseCondition.of(level, camera, partialTicks);
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
		long particleBytes = 4L * ParticleVertexFormats.IDENTITY_PARTICLE.getVertexSize();
		Backends.glTf.glBindTransformFeedbackBufferRange(transformFeedback, 0, targetBuffer,
			state.targetOffset * particleBytes, state.visibleParticleCount * particleBytes);
	}

	@Override
	public void drawGlCompute(State state, Camera camera, float partialTicks, Vec3 snapshotCamPos,
							  List<LayerBatch> batches, int particleLimit) {
		Vec3 position = camera.getPosition();
		for (int i = state.firstBatch; i < state.endBatch; i++) {
			ImmPtlLayerBatch batch = (ImmPtlLayerBatch) batches.get(i);
			Vec3 origin = batch.origin;
			ParticleTransformFeedbackShader.INSTANCE.setup(
				partialTicks,
				camera.getLeftVector().x(), camera.getLeftVector().y(), camera.getLeftVector().z(),
				camera.getUpVector().x(), camera.getUpVector().y(), camera.getUpVector().z(),
				(float) (origin.x - position.x),
				(float) (origin.y - position.y),
				(float) (origin.z - position.z));
			if (batch.tickCount > 0 && batch.appendCount > 0) {
				state.multiDrawFirst[0] = batch.tickOffset;
				state.multiDrawCount[0] = batch.tickCount;
				state.multiDrawFirst[1] = particleLimit + batch.appendOffset;
				state.multiDrawCount[1] = batch.appendCount;
				Backends.gl.glMultiDrawArrays(GL11C.GL_POINTS, state.multiDrawFirst, state.multiDrawCount);
			} else if (batch.tickCount > 0) {
				GL11C.glDrawArrays(GL11C.GL_POINTS, batch.tickOffset, batch.tickCount);
			} else if (batch.appendCount > 0) {
				GL11C.glDrawArrays(GL11C.GL_POINTS, particleLimit + batch.appendOffset, batch.appendCount);
			}
		}
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
		for (int i = state.firstBatch; i < state.endBatch; i++) {
			ImmPtlLayerBatch batch = (ImmPtlLayerBatch) batches.get(i);
			if (batch.groupCount == 0) {
				continue;
			}
			Vec3 origin = batch.origin;
			constants.putFloat(28, (float) (origin.x - position.x));
			constants.putFloat(32, (float) (origin.y - position.y));
			constants.putFloat(36, (float) (origin.z - position.z));
			constants.putInt(40, batch.groupOffset);
			VK10.vkCmdPushConstants(commandBuffer, pipelineLayout, VK10.VK_SHADER_STAGE_COMPUTE_BIT, 0, constants);
			VK10.vkCmdDispatch(commandBuffer, batch.groupCount, 1, 1);
		}
	}

	@Override
	public ParticleLayout buildGlLayout(State state, List<LayerBatch> batches, int particleLimit) {
		return buildLayout(batches);
	}

	@Override
	public VkLayoutResult buildVkLayout(List<LayerBatch> batches, int particleLimit) {
		ParticleLayout particleLayout = buildLayout(batches);
		int groups = 0;
		for (LayerBatch layerBatch : batches) {
			ImmPtlLayerBatch batch = (ImmPtlLayerBatch) layerBatch;
			batch.groupOffset = groups;
			batch.groupCount = (batch.tickCount + VkCompParticleRenderer.WORKGROUP_SIZE - 1) / VkCompParticleRenderer.WORKGROUP_SIZE
				+ (batch.appendCount + VkCompParticleRenderer.WORKGROUP_SIZE - 1) / VkCompParticleRenderer.WORKGROUP_SIZE;
			groups += batch.groupCount;
		}
		return new VkLayoutResult(particleLayout, groups);
	}

	private ParticleLayout buildLayout(List<LayerBatch> batches) {
		Map<ClientLevel, Map<ParticleRenderType, List<LayerBatch>>> levels = new Object2ReferenceLinkedOpenHashMap<>();
		for (LayerBatch batch : batches) {
			// Batches retain their input ImmPtlRegionKey; merging output slices does not replace it.
			levels.computeIfAbsent(((ImmPtlRegionKey) batch.key).level(), ignored -> new Object2ReferenceLinkedOpenHashMap<>())
				.computeIfAbsent(batch.key.layer(), ignored -> new ReferenceArrayList<>()).add(batch);
		}
		Map<IParticleRenderKey, ParticleLayout.Slice> slices = new Object2ReferenceOpenHashMap<>(batches.size());
		batches.clear();
		int totalCount = 0;
		for (Map.Entry<ClientLevel, Map<ParticleRenderType, List<LayerBatch>>> level : levels.entrySet()) {
			for (Map.Entry<ParticleRenderType, List<LayerBatch>> layer : level.getValue().entrySet()) {
				int start = totalCount;
				for (LayerBatch batch : layer.getValue()) {
					batch.targetOffset = totalCount;
					totalCount += batch.count();
					batches.add(batch);
				}
				if (totalCount > start) {
					// Merge all regions of this level/layer; different levels have separate output ranges.
					slices.put(new ImmPtlRenderKey(layer.getKey(), level.getKey()),
						new ParticleLayout.Slice(layer.getKey(), start, totalCount - start));
				}
			}
		}
		return ParticleLayout.of(totalCount, slices);
	}

	public static class State {
		int firstBatch;
		int endBatch;
		int targetOffset;
		int visibleParticleCount;
		int[] multiDrawFirst;
		int[] multiDrawCount;
	}
}
