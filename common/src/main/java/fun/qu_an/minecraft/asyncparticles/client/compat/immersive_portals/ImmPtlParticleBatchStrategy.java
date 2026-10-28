package fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.ParticleTransformFeedbackShader;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ParticleLayout;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IComputeReuseCondition;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleBatchKey;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.LayerBatch;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ParticleVertexFormats;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.IGlParticleBatchStrategy;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.IVkParticleBatchStrategy;
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
import org.lwjgl.opengl.GL15C;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ImmPtlParticleBatchStrategy implements IGlParticleBatchStrategy<ImmPtlParticleBatchStrategy.RenderInfo>,
	IVkParticleBatchStrategy<ImmPtlParticleBatchStrategy.RenderInfo> {
	private static final double REGION_SIZE = 256.0;

	@Override
	public RenderInfo createGlRenderInfo() {
		RenderInfo renderInfo = new RenderInfo();
		renderInfo.multiDrawFirst = new int[2];
		renderInfo.multiDrawCount = new int[2];
		return renderInfo;
	}

	@Override
	public RenderInfo createVkRenderInfo() {
		return new RenderInfo();
	}

	@Override
	public IParticleBatchKey keyFor(TextureSheetParticle particle) {
		// Input key: split each level/layer into regions with separate position origins.
		return new ImmPtlRegionKey(particle.getRenderType(), ImmersivePortalsCompat.getParticleWorld(particle),
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
				grouped.computeIfAbsent(keyFor(particle), ignored -> new ReferenceArrayList<>()).add(particle);
			}
		}
		return grouped;
	}

	@Override
	public LayerBatch createBatch(IParticleBatchKey key, Vec3 snapshotCamPos) {
		// Input batches only contain ImmPtlRegionKey, created by keyFor(particle).
		ImmPtlRegionKey region = (ImmPtlRegionKey) key;
		return new ImmPtlLayerBatch(key, regionOrigin(region.x(), region.y(), region.z()));
	}

	@Override
	public void writePositions(long address, GpuParticleAddon particle, LayerBatch batch, Vec3 snapshotCamPos) {
		ImmPtlLayerBatch spatialBatch = (ImmPtlLayerBatch) batch;
		Vec3 origin = spatialBatch.origin;
		MemoryUtil.memPutFloat(address, (float) (particle.asyncparticles$getXo() - origin.x));
		MemoryUtil.memPutFloat(address + 4L, (float) (particle.asyncparticles$getYo() - origin.y));
		MemoryUtil.memPutFloat(address + 8L, (float) (particle.asyncparticles$getZo() - origin.z));
		MemoryUtil.memPutFloat(address + 12L, (float) (particle.asyncparticles$getX() - origin.x));
		MemoryUtil.memPutFloat(address + 16L, (float) (particle.asyncparticles$getY() - origin.y));
		MemoryUtil.memPutFloat(address + 20L, (float) (particle.asyncparticles$getZ() - origin.z));
	}

	@Override
	public IParticleBatchKey keyFor(ParticleRenderType layer) {
		// Output key: select the current level/layer from the shared slice map, regardless of region.
		return new ImmPtlRenderKey(layer, Minecraft.getInstance().level);
	}

	@Override
	public IComputeReuseCondition prepareView(RenderInfo renderInfo, List<LayerBatch> batches, Camera camera, float partialTicks) {
		renderInfo.firstBatch = 0;
		renderInfo.endBatch = 0;
		renderInfo.targetOffset = 0;
		renderInfo.visibleParticleCount = 0;
		ClientLevel level = Minecraft.getInstance().level;
		boolean found = false;
		for (int i = 0; i < batches.size(); i++) {
			LayerBatch batch = batches.get(i);
			// Select the world using the input ImmPtlRegionKey; output keys only index slices.
			if (((ImmPtlRegionKey) batch.key).level() == level) {
				if (!found) {
					found = true;
					renderInfo.firstBatch = i;
					renderInfo.targetOffset = batch.targetOffset;
				}
				renderInfo.endBatch = i + 1;
				renderInfo.visibleParticleCount += batch.count();
			} else if (found) {
				break;
			}
		}
		if (renderInfo.visibleParticleCount == 0) {
			return null;
		}
		return ImmPtlComputeReuseCondition.of(level, camera, partialTicks);
	}

	@Override
	public void prepareGlCompute(RenderInfo renderInfo,
								 Camera camera,
								 float partialTicks,
								 Vec3 snapshotCamPos,
								 int particleCount,
								 int transformFeedback,
								 int targetBuffer) {
		ParticleTransformFeedbackShader.INSTANCE.use();
		long particleBytes = 4L * ParticleVertexFormats.IDENTITY_PARTICLE.getVertexSize();
		Backends.glTf.glBindTransformFeedbackBufferRange(transformFeedback, 0, targetBuffer,
			renderInfo.targetOffset * particleBytes, renderInfo.visibleParticleCount * particleBytes);
	}

	@Override
	public void drawGlCompute(RenderInfo renderInfo, Camera camera, float partialTicks, Vec3 snapshotCamPos,
							  List<LayerBatch> batches, int particleLimit) {
		Vec3 position = camera.getPosition();
		for (int i = renderInfo.firstBatch; i < renderInfo.endBatch; i++) {
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
				renderInfo.multiDrawFirst[0] = batch.tickOffset;
				renderInfo.multiDrawCount[0] = batch.tickCount;
				renderInfo.multiDrawFirst[1] = particleLimit + batch.appendOffset;
				renderInfo.multiDrawCount[1] = batch.appendCount;
				GL15C.glMultiDrawArrays(GL11C.GL_POINTS, renderInfo.multiDrawFirst, renderInfo.multiDrawCount);
			} else if (batch.tickCount > 0) {
				GL11C.glDrawArrays(GL11C.GL_POINTS, batch.tickOffset, batch.tickCount);
			} else if (batch.appendCount > 0) {
				GL11C.glDrawArrays(GL11C.GL_POINTS, particleLimit + batch.appendOffset, batch.appendCount);
			}
		}
	}

	@Override
	public void dispatchVkBatches(RenderInfo renderInfo,
								  long pipelineLayout,
								  ByteBuffer constants,
								  List<LayerBatch> batches,
								  Vec3 snapshotCamPos,
								  Camera camera,
								  float partialTicks,
								  int groupCount) {
		Vec3 position = camera.getPosition();
		VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
		for (int i = renderInfo.firstBatch; i < renderInfo.endBatch; i++) {
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
	public ParticleLayout buildGlLayout(RenderInfo renderInfo, List<LayerBatch> batches, int particleLimit) {
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
		Map<IParticleBatchKey, ParticleLayout.ParticleSlice> slices = new Object2ReferenceOpenHashMap<>(batches.size());
		batches.clear();
		int totalCount = 0;
		for (var level : levels.entrySet()) {
			for (var layer : level.getValue().entrySet()) {
				int start = totalCount;
				for (LayerBatch batch : layer.getValue()) {
					batch.targetOffset = totalCount;
					totalCount += batch.count();
					batches.add(batch);
				}
				if (totalCount > start) {
					// Merge all regions of this level/layer; different levels have separate output ranges.
					slices.put(new ImmPtlRenderKey(layer.getKey(), level.getKey()),
						new ParticleLayout.ParticleSlice(layer.getKey(), start, totalCount - start));
				}
			}
		}
		return ParticleLayout.of(totalCount, slices);
	}

	public static class RenderInfo {
		int firstBatch;
		int endBatch;
		int targetOffset;
		int visibleParticleCount;
		int[] multiDrawFirst;
		int[] multiDrawCount;
	}
}
